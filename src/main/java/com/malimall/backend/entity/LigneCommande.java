package com.malimall.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Ligne d'article, rattachée à EXACTEMENT UN parent à la fois : soit un
 * Panier (encore en cours de constitution), soit une Commande (déjà validée
 * — panier_id passe alors à null et commande_id est renseigné, ré-attachement
 * plutôt que duplication, cf. PanierService.validerPanier()).
 */
@Entity
@Table(name = "ligne_commande")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LigneCommande {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_ligne")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "panier_id")
    private Panier panier;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "commande_id")
    private Commande commande;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "produit_id", nullable = false)
    private Produit produit;

    @Column(nullable = false)
    private int quantite;

    @Column(name = "prix_unitaire_mmc", nullable = false)
    private int prixUnitaireMmc;

    @Transient
    public int sousTotal() {
        return quantite * prixUnitaireMmc;
    }
}
