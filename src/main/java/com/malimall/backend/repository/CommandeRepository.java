package com.malimall.backend.repository;

import com.malimall.backend.entity.Commande;
import com.malimall.backend.entity.enums.StatutLivraison;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommandeRepository extends JpaRepository<Commande, Long> {
    List<Commande> findByAcheteurId(Long acheteurId);
    List<Commande> findByBoutiqueId(Long boutiqueId);
    List<Commande> findByBoutiqueProprietaireId(Long vendeurId);
    List<Commande> findByChauffeurId(Long chauffeurId);

    /** Livraisons pas encore prises en charge — pour qu'un chauffeur puisse les parcourir et s'auto-assigner. */
    List<Commande> findByStatutLivraison(StatutLivraison statutLivraison);
    long countByStatutLivraison(StatutLivraison statutLivraison);

    /** Console admin : compteurs pour les tuiles KPI. */
    long countByStatut(com.malimall.backend.entity.enums.StatutCommande statut);
    long countBySignaleeTrue();
}
