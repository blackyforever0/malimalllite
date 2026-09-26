package com.malimall.backend.service;

import com.malimall.backend.entity.EcritureComptable;
import com.malimall.backend.entity.Utilisateur;
import com.malimall.backend.entity.enums.TypeOperation;
import com.malimall.backend.exception.RessourceIntrouvableException;
import com.malimall.backend.repository.EcritureComptableRepository;
import com.malimall.backend.repository.UtilisateurRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Recharge manuelle de portefeuille — outil de développement/démo pour ce
 * lot (aucun moyen de paiement réel n'est branché). Toujours appelé derrière
 * un contrôleur exigeant ROLE_ADMIN (voir AdminController).
 */
@Service
public class AdminService {

    private final PortefeuilleService portefeuilleService;
    private final UtilisateurRepository utilisateurRepository;
    private final EcritureComptableRepository ecritureComptableRepository;

    public AdminService(PortefeuilleService portefeuilleService,
                         UtilisateurRepository utilisateurRepository,
                         EcritureComptableRepository ecritureComptableRepository) {
        this.portefeuilleService = portefeuilleService;
        this.utilisateurRepository = utilisateurRepository;
        this.ecritureComptableRepository = ecritureComptableRepository;
    }

    @Transactional
    public void rechargerPortefeuille(Long utilisateurId, int montantMmc) {
        Utilisateur utilisateur = utilisateurRepository.findById(utilisateurId)
                .orElseThrow(() -> new RessourceIntrouvableException("Utilisateur introuvable : " + utilisateurId));

        portefeuilleService.crediter(utilisateurId, montantMmc);

        ecritureComptableRepository.save(EcritureComptable.builder()
                .utilisateur(utilisateur)
                .typeOperation(TypeOperation.RECHARGE)
                .montantMmc(montantMmc)
                .referenceType("ADMIN_RECHARGE")
                .build());
    }

    /**
     * Débite directement {@code montantMmc} du portefeuille (jamais de solde
     * négatif : lève FondsInsuffisantsException si le solde disponible est
     * insuffisant) — outil de modération manuelle (ex. correction d'un abus),
     * distinct du débit interne lié à une commande/course.
     */
    @Transactional
    public void debiterPortefeuille(Long utilisateurId, int montantMmc) {
        Utilisateur utilisateur = utilisateurRepository.findById(utilisateurId)
                .orElseThrow(() -> new RessourceIntrouvableException("Utilisateur introuvable : " + utilisateurId));

        portefeuilleService.debiterDirect(utilisateurId, montantMmc);

        ecritureComptableRepository.save(EcritureComptable.builder()
                .utilisateur(utilisateur)
                .typeOperation(TypeOperation.RETRAIT)
                .montantMmc(montantMmc)
                .referenceType("ADMIN_DEBIT")
                .build());
    }

    /** Gèle {@code montantMmc} (fonds réservés, non débités) — ex. le temps d'examiner un abus signalé. */
    @Transactional
    public void gelerPortefeuille(Long utilisateurId, int montantMmc) {
        portefeuilleService.geler(utilisateurId, montantMmc);
    }

    /** Débloque des fonds préalablement gelés par l'admin. */
    @Transactional
    public void debloquerPortefeuille(Long utilisateurId, int montantMmc) {
        portefeuilleService.debloquer(utilisateurId, montantMmc);
    }
}
