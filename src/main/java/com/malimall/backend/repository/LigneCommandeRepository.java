package com.malimall.backend.repository;

import com.malimall.backend.entity.LigneCommande;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LigneCommandeRepository extends JpaRepository<LigneCommande, Long> {

    /**
     * Quantité réellement vendue d'un produit — somme des lignes rattachées
     * à une Commande (jamais celles encore dans un Panier) dont la commande
     * n'est pas annulée. Alimente le badge "vendus" de la carte produit :
     * un vrai compteur, jamais une valeur d'exemple (voir décision
     * documentée dans le README mobile — pas de badge "likes" en revanche,
     * faute d'un système de favoris côté backend).
     */
    @Query("""
            SELECT COALESCE(SUM(l.quantite), 0) FROM LigneCommande l
            WHERE l.produit.id = :produitId
              AND l.commande IS NOT NULL
              AND l.commande.statut <> com.malimall.backend.entity.enums.StatutCommande.ANNULEE
            """)
    int compterQuantiteVendue(@Param("produitId") Long produitId);

    /** Un produit déjà commandé (même annulé) ne peut pas être supprimé sans casser l'historique — voir ProduitService.supprimer. */
    boolean existsByProduitId(Long produitId);
}
