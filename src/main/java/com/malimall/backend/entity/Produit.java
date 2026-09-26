package com.malimall.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "produit")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Produit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_produit")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "boutique_id", nullable = false)
    private Boutique boutique;

    @Column(nullable = false, length = 150)
    private String nom;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "prix_mmc", nullable = false)
    private int prixMmc;

    @Column(nullable = false)
    private int stock;

    @Column(length = 50)
    private String categorie;

    /** Poids unitaire, pour vérifier la capacité du véhicule du livreur. */
    @Column(name = "poids_kg", nullable = false, precision = 6, scale = 2)
    @Builder.Default
    private BigDecimal poidsKg = BigDecimal.ONE;

    /** URL publique Supabase Storage ; null si aucune photo n'a encore été téléversée. */
    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(nullable = false)
    @Builder.Default
    private boolean actif = true;

    @Column(name = "date_creation", nullable = false, updatable = false)
    @Builder.Default
    private Instant dateCreation = Instant.now();

    /** Promotion vendeur (voir ProduitService.mettreEnPromotion / retirerPromotion). */
    @Column(name = "en_promotion", nullable = false)
    @Builder.Default
    private boolean enPromotion = false;

    @Column(name = "promo_prix_mmc")
    private Integer promoPrixMmc;

    @Column(name = "promo_fin_at")
    private Instant promoFinAt;

    public void decrementerStock(int quantite) {
        if (quantite <= 0) {
            throw new IllegalArgumentException("La quantité doit être positive");
        }
        if (stock < quantite) {
            throw new IllegalStateException("Stock insuffisant pour le produit " + id);
        }
        stock -= quantite;
    }

    /** Promotion réellement active en ce moment (activée ET pas encore expirée). */
    public boolean isPromoActive() {
        return enPromotion && promoFinAt != null && promoFinAt.isAfter(Instant.now());
    }

    /** Prix à facturer à l'achat : le prix promo si actif, sinon le prix normal. */
    public int prixEffectifMmc() {
        return (isPromoActive() && promoPrixMmc != null) ? promoPrixMmc : prixMmc;
    }
}
