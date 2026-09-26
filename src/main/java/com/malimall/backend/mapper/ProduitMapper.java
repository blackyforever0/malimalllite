package com.malimall.backend.mapper;

import com.malimall.backend.dto.CatalogueDtos.ProduitResponse;
import com.malimall.backend.entity.Produit;

public final class ProduitMapper {

    private ProduitMapper() {}

    /** quantiteVendue à 0 — préférer {@link #toResponse(Produit, int)} quand le vrai compteur est disponible. */
    public static ProduitResponse toResponse(Produit produit) {
        return toResponse(produit, 0);
    }

    public static ProduitResponse toResponse(Produit produit, int quantiteVendue) {
        return new ProduitResponse(
                produit.getId(),
                produit.getBoutique().getId(),
                produit.getNom(),
                produit.getDescription(),
                produit.getPrixMmc(),
                produit.getStock(),
                produit.getCategorie(),
                produit.isActif(),
                produit.getImageUrl(),
                quantiteVendue,
                produit.getPoidsKg(),
                produit.isPromoActive(),
                produit.isPromoActive() ? produit.getPromoPrixMmc() : null);
    }
}
