package com.malimall.backend.service;

import com.malimall.backend.config.MaliMallProperties;
import com.malimall.backend.dto.CommandeDtos.CommandeResponse;
import com.malimall.backend.entity.Chauffeur;
import com.malimall.backend.entity.Commande;
import com.malimall.backend.entity.EcritureComptable;
import com.malimall.backend.entity.Signalement;
import com.malimall.backend.entity.Vehicule;
import com.malimall.backend.entity.enums.MotifSignalement;
import com.malimall.backend.entity.Utilisateur;
import com.malimall.backend.entity.enums.StatutCommande;
import com.malimall.backend.entity.enums.StatutLivraison;
import com.malimall.backend.entity.enums.TypeOperation;
import com.malimall.backend.exception.AccesRefuseException;
import com.malimall.backend.exception.CodeInvalideException;
import com.malimall.backend.exception.RessourceIntrouvableException;
import com.malimall.backend.exception.TransitionStatutInvalideException;
import com.malimall.backend.mapper.CommandeMapper;
import com.malimall.backend.repository.ChauffeurRepository;
import com.malimall.backend.repository.CommandeRepository;
import com.malimall.backend.repository.EcritureComptableRepository;
import com.malimall.backend.repository.SignalementRepository;
import com.malimall.backend.repository.UtilisateurRepository;
import com.malimall.backend.service.CommissionCalculator.CommissionSplit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Cycle de livraison à deux codes, chacun sur sa propre machine à états :
 * assignerChauffeur -&gt; scannerCodeRetrait (chez le vendeur) -&gt;
 * confirmerLivraison (chez l'acheteur, règlement des fonds). Chaque méthode
 * est une garde explicite (statut attendu + code attendu) avant toute
 * mutation — jamais de transition implicite. Retourne des CommandeResponse
 * déjà mappés (jamais l'entité) pour la même raison que CommandeService —
 * voir sa javadoc sur open-in-view=false.
 */
@Service
public class LivraisonService {

    private final CommandeRepository commandeRepository;
    private final ChauffeurRepository chauffeurRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final EcritureComptableRepository ecritureComptableRepository;
    private final PortefeuilleService portefeuilleService;
    private final CommissionCalculator commissionCalculator;
    private final MaliMallProperties properties;
    private final SignalementRepository signalementRepository;

    public LivraisonService(CommandeRepository commandeRepository,
                             ChauffeurRepository chauffeurRepository,
                             UtilisateurRepository utilisateurRepository,
                             EcritureComptableRepository ecritureComptableRepository,
                             PortefeuilleService portefeuilleService,
                             CommissionCalculator commissionCalculator,
                             MaliMallProperties properties,
                             SignalementRepository signalementRepository) {
        this.commandeRepository = commandeRepository;
        this.chauffeurRepository = chauffeurRepository;
        this.utilisateurRepository = utilisateurRepository;
        this.ecritureComptableRepository = ecritureComptableRepository;
        this.portefeuilleService = portefeuilleService;
        this.commissionCalculator = commissionCalculator;
        this.properties = properties;
        this.signalementRepository = signalementRepository;
    }

    @Transactional
    public CommandeResponse assignerChauffeur(Long commandeId, Long chauffeurUtilisateurId) {
        Commande commande = charger(commandeId);
        Chauffeur chauffeur = chauffeurRepository.findByUtilisateurId(chauffeurUtilisateurId)
                .orElseThrow(() -> new RessourceIntrouvableException("Profil chauffeur introuvable"));
        if (!chauffeur.isDisponible()) {
            throw new TransitionStatutInvalideException("Ce chauffeur n'est pas disponible actuellement");
        }
        if (commande.getStatutLivraison() != StatutLivraison.EN_ATTENTE_LIVREUR) {
            throw new TransitionStatutInvalideException("Cette commande a déjà un livreur assigné");
        }
        Vehicule vehicule = chauffeur.getVehicule();
        if (vehicule != null && commande.poidsTotalKg().compareTo(BigDecimal.valueOf(vehicule.capaciteMaxKg())) > 0) {
            throw new TransitionStatutInvalideException(
                    "Colis trop lourd pour votre " + vehicule.getType().name().toLowerCase()
                            + " : un véhicule plus grand est nécessaire");
        }
        commande.setChauffeur(chauffeur);
        commande.setDateAssignation(Instant.now());
        commande.setStatutLivraison(StatutLivraison.ASSIGNEE);
        commande.setStatut(StatutCommande.EN_LIVRAISON);
        commande = commandeRepository.save(commande);
        return CommandeMapper.toResponse(commande, chauffeurUtilisateurId, false);
    }

    @Transactional
    public CommandeResponse scannerCodeRetrait(Long commandeId, Long chauffeurUtilisateurId, String code) {
        Commande commande = charger(commandeId);
        verifierChauffeurAssigne(commande, chauffeurUtilisateurId);
        if (commande.getStatutLivraison() != StatutLivraison.ASSIGNEE) {
            throw new TransitionStatutInvalideException("Cette commande n'est pas au stade du retrait");
        }
        if (!commande.getCodeRetrait().equals(code)) {
            throw new CodeInvalideException("Code de retrait invalide");
        }
        commande.setStatutLivraison(StatutLivraison.RECUPEREE);
        commande.setDateRetrait(Instant.now());
        commande = commandeRepository.save(commande);
        return CommandeMapper.toResponse(commande, chauffeurUtilisateurId, false);
    }

    /**
     * Règlement des fonds : débite l'acheteur (les fonds étaient déjà gelés
     * depuis validerPanier), crédite vendeur/chauffeur/plateforme selon
     * {@link CommissionCalculator}, écrit 3 {@link EcritureComptable}.
     * Idempotent par construction : ne rejoue rien si déjà LIVREE.
     */
    @Transactional
    public CommandeResponse confirmerLivraison(Long commandeId, Long acheteurUtilisateurId, String code) {
        Commande commande = charger(commandeId);
        if (!commande.getAcheteur().getId().equals(acheteurUtilisateurId)) {
            throw new AccesRefuseException("Cette commande n'appartient pas à cet acheteur");
        }
        if (commande.getStatut() == StatutCommande.LIVREE) {
            return CommandeMapper.toResponse(commande, acheteurUtilisateurId, false);
        }
        if (commande.isSignalee()) {
            throw new TransitionStatutInvalideException(
                    "Un signalement est en cours sur cette commande : les fonds restent bloqués jusqu'à son traitement");
        }
        if (commande.getStatutLivraison() != StatutLivraison.RECUPEREE
                && commande.getStatutLivraison() != StatutLivraison.EN_ROUTE) {
            throw new TransitionStatutInvalideException("Cette commande n'est pas encore prête à être livrée");
        }
        if (!commande.getCodeLivraison().equals(code)) {
            throw new CodeInvalideException("Code de livraison invalide");
        }

        Long acheteurId = commande.getAcheteur().getId();
        Long vendeurUtilisateurId = commande.getBoutique().getProprietaire().getUtilisateur().getId();
        Long chauffeurUtilisateurId = commande.getChauffeur().getUtilisateur().getId();
        Long plateformeUtilisateurId = plateformeUtilisateurId();

        CommissionSplit split = commissionCalculator.splitLivraisonNormale(
                commande.getMontantTotalMmc(), commande.getFraisLivraisonMmc());

        portefeuilleService.debiter(acheteurId, commande.montantTotalAvecLivraison());
        portefeuilleService.crediter(vendeurUtilisateurId, split.sellerReceives());
        portefeuilleService.crediter(chauffeurUtilisateurId, split.driverReceives());
        portefeuilleService.crediter(plateformeUtilisateurId, split.platformCommission());

        ecrire(vendeurUtilisateurId, TypeOperation.VENTE, split.sellerReceives(), commande.getId());
        ecrire(chauffeurUtilisateurId, TypeOperation.FRAIS_LIVRAISON, split.driverReceives(), commande.getId());
        ecrire(plateformeUtilisateurId, TypeOperation.COMMISSION, split.platformCommission(), commande.getId());

        commande.setStatut(StatutCommande.LIVREE);
        commande.setStatutLivraison(StatutLivraison.LIVREE);
        commande.setDateLivraison(Instant.now());
        commande = commandeRepository.save(commande);
        return CommandeMapper.toResponse(commande, acheteurUtilisateurId, false);
    }

    /**
     * Retrait sur place (Commande.retraitParClient) : le vendeur valide le
     * code que le CLIENT lui présente en venant chercher sa commande —
     * aucun livreur, donc aucune part livreur dans le règlement (frais de
     * livraison déjà à 0 depuis PanierService.validerPanier). Miroir de
     * confirmerLivraison, mais déclenché par le vendeur et sans chauffeur.
     */
    @Transactional
    public CommandeResponse confirmerRetraitClient(Long commandeId, Long vendeurUtilisateurId, String code) {
        Commande commande = charger(commandeId);
        if (!commande.isRetraitParClient()) {
            throw new TransitionStatutInvalideException("Cette commande n'est pas en retrait sur place");
        }
        if (!commande.getBoutique().getProprietaire().getUtilisateur().getId().equals(vendeurUtilisateurId)) {
            throw new AccesRefuseException("Cette commande ne concerne pas votre boutique");
        }
        if (commande.getStatut() == StatutCommande.LIVREE) {
            return CommandeMapper.toResponse(commande, vendeurUtilisateurId, false);
        }
        if (commande.getStatut() == StatutCommande.ANNULEE) {
            throw new TransitionStatutInvalideException("Cette commande a été annulée");
        }
        if (commande.isSignalee()) {
            throw new TransitionStatutInvalideException(
                    "Un signalement est en cours sur cette commande : les fonds restent bloqués jusqu'à son traitement");
        }
        if (!commande.getCodeRetrait().equals(code)) {
            throw new CodeInvalideException("Code de retrait invalide");
        }

        Long acheteurId = commande.getAcheteur().getId();
        Long vendeurUtilisateurIdReel = commande.getBoutique().getProprietaire().getUtilisateur().getId();
        Long plateformeUtilisateurId = plateformeUtilisateurId();

        // fraisLivraisonMmc vaut toujours 0 en retrait sur place : le split ne rémunère donc que le
        // vendeur (et la commission plateforme), jamais de part livreur.
        CommissionSplit split = commissionCalculator.splitLivraisonNormale(
                commande.getMontantTotalMmc(), commande.getFraisLivraisonMmc());

        portefeuilleService.debiter(acheteurId, commande.montantTotalAvecLivraison());
        portefeuilleService.crediter(vendeurUtilisateurIdReel, split.sellerReceives());
        portefeuilleService.crediter(plateformeUtilisateurId, split.platformCommission());

        ecrire(vendeurUtilisateurIdReel, TypeOperation.VENTE, split.sellerReceives(), commande.getId());
        ecrire(plateformeUtilisateurId, TypeOperation.COMMISSION, split.platformCommission(), commande.getId());

        commande.setStatut(StatutCommande.LIVREE);
        commande.setDateRetrait(Instant.now());
        commande.setDateLivraison(Instant.now());
        commande = commandeRepository.save(commande);
        return CommandeMapper.toResponse(commande, vendeurUtilisateurId, false);
    }

    /** L'acheteur note son livreur (1 à 5) une fois la commande livrée — une seule fois. */
    @Transactional
    public CommandeResponse noterLivreur(Long commandeId, Long acheteurUtilisateurId, int note) {
        Commande commande = charger(commandeId);
        if (!commande.getAcheteur().getId().equals(acheteurUtilisateurId)) {
            throw new AccesRefuseException("Cette commande n'appartient pas à cet acheteur");
        }
        if (commande.getStatut() != StatutCommande.LIVREE || commande.getChauffeur() == null) {
            throw new TransitionStatutInvalideException("Le livreur ne peut être noté qu'après la livraison");
        }
        if (commande.getNoteLivreur() != null) {
            throw new TransitionStatutInvalideException("Ce livreur a déjà été noté pour cette commande");
        }
        commande.setNoteLivreur(note);
        commande.getChauffeur().ajouterNote(note);
        commande = commandeRepository.save(commande);
        return CommandeMapper.toResponse(commande, acheteurUtilisateurId, false);
    }

    /**
     * Signale un problème sur une livraison (acheteur, vendeur ou livreur
     * concerné). La commande passe en "signalée" : la confirmation de
     * livraison — et donc le déblocage des fonds — est suspendue jusqu'au
     * traitement du signalement par l'équipe.
     */
    @Transactional
    public CommandeResponse signaler(Long commandeId, Long auteurUtilisateurId, MotifSignalement motif, String description) {
        Commande commande = charger(commandeId);
        boolean estAcheteur = commande.getAcheteur().getId().equals(auteurUtilisateurId);
        boolean estVendeur = commande.getBoutique().getProprietaire().getUtilisateur().getId().equals(auteurUtilisateurId);
        boolean estLivreur = commande.getChauffeur() != null
                && commande.getChauffeur().getUtilisateur().getId().equals(auteurUtilisateurId);
        if (!estAcheteur && !estVendeur && !estLivreur) {
            throw new AccesRefuseException("Vous n'êtes pas concerné par cette commande");
        }
        if (commande.getStatut() == StatutCommande.LIVREE) {
            throw new TransitionStatutInvalideException("Cette commande est déjà livrée");
        }
        Utilisateur auteur = utilisateurRepository.findById(auteurUtilisateurId)
                .orElseThrow(() -> new RessourceIntrouvableException("Utilisateur introuvable : " + auteurUtilisateurId));
        signalementRepository.save(Signalement.builder()
                .auteur(auteur)
                .commande(commande)
                .motif(motif)
                .description(description)
                .build());
        commande.setSignalee(true);
        commande = commandeRepository.save(commande);
        return CommandeMapper.toResponse(commande, auteurUtilisateurId, false);
    }

    private Commande charger(Long commandeId) {
        return commandeRepository.findById(commandeId)
                .orElseThrow(() -> new RessourceIntrouvableException("Commande introuvable : " + commandeId));
    }

    private void verifierChauffeurAssigne(Commande commande, Long chauffeurUtilisateurId) {
        if (commande.getChauffeur() == null
                || !commande.getChauffeur().getUtilisateur().getId().equals(chauffeurUtilisateurId)) {
            throw new AccesRefuseException("Ce chauffeur n'est pas assigné à cette commande");
        }
    }

    private void ecrire(Long utilisateurId, TypeOperation type, int montant, Long commandeId) {
        Utilisateur utilisateur = utilisateurRepository.findById(utilisateurId)
                .orElseThrow(() -> new RessourceIntrouvableException("Utilisateur introuvable : " + utilisateurId));
        ecritureComptableRepository.save(EcritureComptable.builder()
                .utilisateur(utilisateur)
                .typeOperation(type)
                .montantMmc(montant)
                .referenceType("COMMANDE")
                .referenceId(commandeId)
                .build());
    }

    private Long plateformeUtilisateurId() {
        return utilisateurRepository.findByTelephone(properties.plateforme().telephone())
                .orElseThrow(() -> new IllegalStateException(
                        "Utilisateur plateforme introuvable (téléphone " + properties.plateforme().telephone()
                                + ") — vérifier V2__seed_demo_data.sql"))
                .getId();
    }
}
