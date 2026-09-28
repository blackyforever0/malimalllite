package com.malimall.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public class CatalogueDtos {

    public record BoutiqueRequest(
            @NotBlank String nom,
            String description,
            String categorie,
            String quartier
    ) {}

    public record BoutiqueResponse(
            Long id,
            String nom,
            String description,
            String categorie,
            boolean certifiee,
            Long proprietaireId,
            String imageUrl,
            String quartier,
            PositionDto position
    ) {}

    /** Corps de PUT /api/boutiques/{id}/position — voir BoutiqueService.mettreAJourPosition. */
    public record PositionRequest(@NotNull Double latitude, @NotNull Double longitude) {}

    public record ProduitRequest(
            @NotBlank String nom,
            String description,
            @Positive int prixMmc,
            @PositiveOrZero int stock,
            String categorie,
            /** Facultatif : 1 kg par défaut. */
            @Positive BigDecimal poidsKg
    ) {}

    public record ProduitResponse(
            Long id,
            Long boutiqueId,
            String nom,
            String description,
            int prixMmc,
            int stock,
            String categorie,
            boolean actif,
            String imageUrl,
            int quantiteVendue,
            BigDecimal poidsKg,
            boolean enPromotion,
            Integer promoPrixMmc
    ) {}

    public record ReapprovisionnerRequest(@Positive int quantite) {}

    public record MettreEnPromotionRequest(@Positive int prixMmc, @Positive int dureeJours) {}
}
