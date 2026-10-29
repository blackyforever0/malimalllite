package com.malimall.backend.service;

import com.malimall.backend.dto.CommandeDtos.CommandeResponse;
import com.malimall.backend.entity.Chauffeur;
import com.malimall.backend.entity.Commande;
import com.malimall.backend.entity.LigneCommande;
import com.malimall.backend.entity.Vendeur;
import com.malimall.backend.entity.enums.StatutCommande;
import com.malimall.backend.entity.enums.StatutLivraison;
import com.malimall.backend.exception.AccesRefuseException;
import com.malimall.backend.exception.RessourceIntrouvableException;
import com.malimall.backend.exception.TransitionStatutInvalideException;
import com.malimall.backend.mapper.CommandeMapper;
import com.malimall.backend.repository.ChauffeurRepository;
import com.malimall.backend.repository.CommandeRepository;
import com.malimall.backend.repository.VendeurRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Lecture des Commandes et annulation acheteur avant la prise en charge.
 * Volontairement séparé de {@link LivraisonService} (transitions de livraison)
 * et {@link PanierService} (checkout).
 *
 * Le mapping vers CommandeResponse est fait ICI, à l'intérieur de méthodes
 * @Transactional(readOnly = true) — jamais dans le contrôleur — parce que
 * spring.jpa.open-in-view=false ferme la session Hibernate dès que la
 * méthode de service retourne, et CommandeMapper déréférence des
 * associations @ManyToOne LAZY (boutique.nom, produit.nom...) qui
 * planteraient (LazyInitializationException) si on les touchait après coup.
 */
@Service
public class CommandeService {

    private final CommandeRepository commandeRepository;
    private final VendeurRepository vendeurRepository;
    private final ChauffeurRepository chauffeurRepository;
    private final PortefeuilleService portefeuilleService;

    public CommandeService(CommandeRepository commandeRepository,
                            VendeurRepository vendeurRepository,
                            ChauffeurRepository chauffeurRepository,
                            PortefeuilleService portefeuilleService) {
        this.commandeRepository = commandeRepository;
        this.vendeurRepository = vendeurRepository;
        this.chauffeurRepository = chauffeurRepository;
        this.portefeuilleService = portefeuilleService;
    }

    @Transactional
    public CommandeResponse annuler(Long commandeId, Long acheteurUtilisateurId) {
        Commande commande = commandeRepository.findByIdForUpdate(commandeId)
                .orElseThrow(() -> new RessourceIntrouvableException("Commande introuvable : " + commandeId));
        if (!commande.getAcheteur().getId().equals(acheteurUtilisateurId)) {
            throw new AccesRefuseException("Cette commande n'appartient pas à cet acheteur");
        }
        boolean chauffeurNonAssigne = commande.getChauffeur() == null
                && commande.getStatutLivraison() == StatutLivraison.EN_ATTENTE_LIVREUR;
        boolean retraitNonConfirme = commande.isRetraitParClient()
                && commande.getChauffeur() == null
                && commande.getStatut() == StatutCommande.CONFIRMEE;
        if (commande.getStatut() != StatutCommande.CONFIRMEE
                || (!chauffeurNonAssigne && !retraitNonConfirme)
                || commande.isSignalee()) {
            throw new TransitionStatutInvalideException(
                    "Cette commande ne peut plus être annulée après l'acceptation du livreur ou la remise au vendeur");
        }

        for (LigneCommande ligne : commande.getLignes()) {
            ligne.getProduit().setStock(ligne.getProduit().getStock() + ligne.getQuantite());
        }
        portefeuilleService.debloquer(acheteurUtilisateurId, commande.montantTotalAvecLivraison());
        commande.setStatut(StatutCommande.ANNULEE);
        commande.setStatutLivraison(null);
        return CommandeMapper.toResponse(commandeRepository.save(commande), acheteurUtilisateurId, false);
    }

    @Transactional(readOnly = true)
    public CommandeResponse obtenir(Long commandeId, Long viewerUtilisateurId, boolean viewerEstAdmin) {
        Commande commande = charger(commandeId);
        if (!viewerEstAdmin && !concerneUtilisateur(commande, viewerUtilisateurId)) {
            throw new AccesRefuseException("Vous n'avez pas accès à cette commande");
        }
        return CommandeMapper.toResponse(commande, viewerUtilisateurId, viewerEstAdmin);
    }

    @Transactional(readOnly = true)
    public List<CommandeResponse> mesCommandesAcheteur(Long utilisateurId, boolean viewerEstAdmin) {
        return mapper(commandeRepository.findByAcheteurId(utilisateurId), utilisateurId, viewerEstAdmin);
    }

    @Transactional(readOnly = true)
    public List<CommandeResponse> mesCommandesVendeur(Long utilisateurId, boolean viewerEstAdmin) {
        Vendeur vendeur = vendeurRepository.findByUtilisateurId(utilisateurId)
                .orElseThrow(() -> new IllegalStateException("Aucun profil vendeur pour cet utilisateur"));
        return mapper(commandeRepository.findByBoutiqueProprietaireId(vendeur.getId()), utilisateurId, viewerEstAdmin);
    }

    @Transactional(readOnly = true)
    public List<CommandeResponse> mesCommandesChauffeur(Long utilisateurId, boolean viewerEstAdmin) {
        Chauffeur chauffeur = chauffeurRepository.findByUtilisateurId(utilisateurId)
                .orElseThrow(() -> new IllegalStateException("Aucun profil chauffeur pour cet utilisateur"));
        return mapper(commandeRepository.findByChauffeurId(chauffeur.getId()), utilisateurId, viewerEstAdmin);
    }

    /**
     * Livraisons pas encore prises en charge, pour qu'un chauffeur puisse les
     * parcourir et s'auto-assigner (POST /{id}/assigner-chauffeur). Volontairement
     * PAS de contrôle d'accès "concerneUtilisateur" ici : par définition, une
     * commande encore EN_ATTENTE_LIVREUR ne concerne aucun chauffeur en
     * particulier — c'est justement ce que ce chauffeur cherche à découvrir.
     * Les codes retrait/livraison restent naturellement masqués par
     * CommandeMapper (le viewer n'est ni le vendeur propriétaire ni l'acheteur).
     */
    @Transactional(readOnly = true)
    public List<CommandeResponse> commandesDisponibles(Long callerUtilisateurId) {
        List<Commande> commandes = commandeRepository.findByStatutLivraison(StatutLivraison.EN_ATTENTE_LIVREUR);
        return commandes.stream()
                .map(c -> CommandeMapper.toResponse(c, callerUtilisateurId, false))
                .toList();
    }

    private List<CommandeResponse> mapper(List<Commande> commandes, Long viewerUtilisateurId, boolean viewerEstAdmin) {
        return commandes.stream()
                .map(c -> CommandeMapper.toResponse(c, viewerUtilisateurId, viewerEstAdmin))
                .toList();
    }

    private Commande charger(Long commandeId) {
        return commandeRepository.findById(commandeId)
                .orElseThrow(() -> new RessourceIntrouvableException("Commande introuvable : " + commandeId));
    }

    private boolean concerneUtilisateur(Commande commande, Long utilisateurId) {
        boolean estAcheteur = commande.getAcheteur().getId().equals(utilisateurId);
        boolean estVendeurProprietaire = commande.getBoutique().getProprietaire()
                .getUtilisateur().getId().equals(utilisateurId);
        boolean estChauffeurAssigne = commande.getChauffeur() != null
                && commande.getChauffeur().getUtilisateur().getId().equals(utilisateurId);
        return estAcheteur || estVendeurProprietaire || estChauffeurAssigne;
    }
}
