package com.malimall.backend.service;

import com.malimall.backend.entity.EcritureComptable;
import com.malimall.backend.entity.Portefeuille;
import com.malimall.backend.entity.Utilisateur;
import com.malimall.backend.entity.enums.TypeOperation;
import com.malimall.backend.exception.FondsInsuffisantsException;
import com.malimall.backend.exception.RessourceIntrouvableException;
import com.malimall.backend.repository.EcritureComptableRepository;
import com.malimall.backend.repository.PortefeuilleRepository;
import com.malimall.backend.repository.UtilisateurRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Toute mutation d'un {@link Portefeuille} passe par ici — jamais de
 * repository.save() direct ailleurs dans le code. Chaque méthode charge le
 * portefeuille avec un verrou pessimiste (SELECT ... FOR UPDATE) pour éviter
 * une race entre deux règlements concurrents sur le même compte. Porte aussi
 * les lectures (solde, historique) pour que les contrôleurs n'accèdent
 * jamais un repository directement.
 */
@Service
public class PortefeuilleService {

    private final PortefeuilleRepository portefeuilleRepository;
    private final EcritureComptableRepository ecritureComptableRepository;
    private final UtilisateurRepository utilisateurRepository;

    public PortefeuilleService(PortefeuilleRepository portefeuilleRepository,
                                EcritureComptableRepository ecritureComptableRepository,
                                UtilisateurRepository utilisateurRepository) {
        this.portefeuilleRepository = portefeuilleRepository;
        this.ecritureComptableRepository = ecritureComptableRepository;
        this.utilisateurRepository = utilisateurRepository;
    }

    public Portefeuille obtenirPourUtilisateur(Long utilisateurId) {
        return portefeuilleRepository.findByUtilisateurId(utilisateurId)
                .orElseThrow(() -> new RessourceIntrouvableException("Portefeuille introuvable pour l'utilisateur " + utilisateurId));
    }

    public List<EcritureComptable> historiquePourUtilisateur(Long utilisateurId) {
        return ecritureComptableRepository.findByUtilisateurIdOrderByDateCreationDesc(utilisateurId);
    }

    @Transactional
    public Portefeuille creerPour(Utilisateur utilisateur) {
        Portefeuille p = Portefeuille.builder()
                .utilisateur(utilisateur)
                .soldeMmc(0)
                .soldeGeleMmc(0)
                .build();
        return portefeuilleRepository.save(p);
    }

    public int soldeDisponible(Portefeuille p) {
        return p.getSoldeMmc() - p.getSoldeGeleMmc();
    }

    private Portefeuille chargerPourEcriture(Long utilisateurId) {
        return portefeuilleRepository.findByUtilisateurIdForUpdate(utilisateurId)
                .orElseThrow(() -> new RessourceIntrouvableException("Portefeuille introuvable pour l'utilisateur " + utilisateurId));
    }

    /** Gèle {@code montant} MMC (fonds réservés, pas encore débités) — lève si le solde disponible est insuffisant. */
    @Transactional
    public Portefeuille geler(Long utilisateurId, int montant) {
        Portefeuille p = chargerPourEcriture(utilisateurId);
        if (soldeDisponible(p) < montant) {
            throw new FondsInsuffisantsException(
                    "Solde disponible insuffisant : %d MMC disponibles, %d MMC requis".formatted(soldeDisponible(p), montant));
        }
        p.setSoldeGeleMmc(p.getSoldeGeleMmc() + montant);
        p.setDateMaj(Instant.now());
        return portefeuilleRepository.save(p);
    }

    /** Débloque {@code montant} MMC préalablement gelés (ex : commande annulée) — plancher à zéro. */
    @Transactional
    public Portefeuille debloquer(Long utilisateurId, int montant) {
        Portefeuille p = chargerPourEcriture(utilisateurId);
        p.setSoldeGeleMmc(Math.max(0, p.getSoldeGeleMmc() - montant));
        p.setDateMaj(Instant.now());
        return portefeuilleRepository.save(p);
    }

    /** Crédite {@code montant} MMC (vente, frais de livraison, commission…). */
    @Transactional
    public Portefeuille crediter(Long utilisateurId, int montant) {
        Portefeuille p = chargerPourEcriture(utilisateurId);
        p.setSoldeMmc(p.getSoldeMmc() + montant);
        p.setDateMaj(Instant.now());
        return portefeuilleRepository.save(p);
    }

    /**
     * Débite {@code montant} MMC réellement (les fonds quittent le compte) et
     * libère le même montant de soldeGeleMmc — utilisé quand des fonds gelés
     * deviennent définitifs (règlement d'une livraison confirmée).
     */
    @Transactional
    public Portefeuille debiter(Long utilisateurId, int montant) {
        Portefeuille p = chargerPourEcriture(utilisateurId);
        p.setSoldeMmc(p.getSoldeMmc() - montant);
        p.setSoldeGeleMmc(Math.max(0, p.getSoldeGeleMmc() - montant));
        p.setDateMaj(Instant.now());
        return portefeuilleRepository.save(p);
    }

    /**
     * Débite {@code montant} MMC directement, SANS toucher soldeGeleMmc —
     * pour un paiement qui n'est jamais passé par un gel préalable (ex.
     * demande de publicité, débitée immédiatement, voir PubliciteService).
     * Ne pas utiliser {@link #debiter} ici : il libérerait à tort du solde
     * gelé par une AUTRE opération en cours (ex. une commande Market
     * simultanée) sans rapport avec ce paiement.
     */
    @Transactional
    public Portefeuille debiterDirect(Long utilisateurId, int montant) {
        Portefeuille p = chargerPourEcriture(utilisateurId);
        if (soldeDisponible(p) < montant) {
            throw new FondsInsuffisantsException(
                    "Solde disponible insuffisant : %d MMC disponibles, %d MMC requis".formatted(soldeDisponible(p), montant));
        }
        p.setSoldeMmc(p.getSoldeMmc() - montant);
        p.setDateMaj(Instant.now());
        return portefeuilleRepository.save(p);
    }

    /**
     * Recharge le portefeuille via un opérateur mobile money — le paiement
     * lui-même (Orange Money, Moov Money, Wave, Sama Money) est simulé côté
     * mobile (aucun appel réel à ces opérateurs, voir RechargeRequest) ;
     * seule cette écriture, elle, est bien réelle dans la base MaliMall.
     */
    @Transactional
    public Portefeuille recharger(Long utilisateurId, int montantMmc) {
        Portefeuille p = crediter(utilisateurId, montantMmc);
        journaliser(utilisateurId, TypeOperation.RECHARGE, montantMmc, null, null);
        return p;
    }

    /** Retrait vers un opérateur mobile money (simulé) — lève si solde disponible insuffisant. */
    @Transactional
    public Portefeuille retirer(Long utilisateurId, int montantMmc) {
        Portefeuille p = debiterDirect(utilisateurId, montantMmc);
        journaliser(utilisateurId, TypeOperation.RETRAIT, montantMmc, null, null);
        return p;
    }

    /**
     * Transfert de MMC entre deux utilisateurs MaliMall, identifié par le
     * téléphone du destinataire. Deux écritures symétriques (une par
     * portefeuille), chacune référençant l'autre utilisateur pour la
     * traçabilité.
     */
    @Transactional
    public Portefeuille transferer(Long expediteurId, String telephoneDestinataire, int montantMmc) {
        Utilisateur destinataire = utilisateurRepository.findByTelephone(telephoneDestinataire)
                .orElseThrow(() -> new RessourceIntrouvableException("Aucun compte MaliMall avec ce numéro"));
        if (destinataire.getId().equals(expediteurId)) {
            throw new IllegalArgumentException("Impossible de vous transférer des MMC à vous-même");
        }
        Portefeuille pExpediteur = debiterDirect(expediteurId, montantMmc);
        crediter(destinataire.getId(), montantMmc);
        journaliser(expediteurId, TypeOperation.TRANSFERT_ENVOYE, montantMmc, "UTILISATEUR", destinataire.getId());
        journaliser(destinataire.getId(), TypeOperation.TRANSFERT_RECU, montantMmc, "UTILISATEUR", expediteurId);
        return pExpediteur;
    }

    /** Nom complet du destinataire, utilisé pour l'écran de confirmation du transfert. */
    public Utilisateur trouverParTelephone(String telephone) {
        return utilisateurRepository.findByTelephone(telephone)
                .orElseThrow(() -> new RessourceIntrouvableException("Aucun compte MaliMall avec ce numéro"));
    }

    private void journaliser(Long utilisateurId, TypeOperation type, int montantMmc, String referenceType, Long referenceId) {
        EcritureComptable ecriture = EcritureComptable.builder()
                .utilisateur(utilisateurRepository.getReferenceById(utilisateurId))
                .typeOperation(type)
                .montantMmc(montantMmc)
                .referenceType(referenceType)
                .referenceId(referenceId)
                .dateCreation(Instant.now())
                .build();
        ecritureComptableRepository.save(ecriture);
    }
}
