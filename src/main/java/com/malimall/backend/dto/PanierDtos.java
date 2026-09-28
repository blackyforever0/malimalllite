package com.malimall.backend.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

public class PanierDtos {

    public record AjouterLigneRequest(
            @NotNull Long produitId,
            @Positive int quantite
    ) {}

    public record LigneResponse(
            Long id,
            Long produitId,
            String produitNom,
            Long boutiqueId,
            String boutiqueNom,
            int quantite,
            int prixUnitaireMmc,
            int sousTotal
    ) {}

    /** Corps optionnel de POST /api/panier/valider. */
    public record ValiderPanierRequest(
            @Size(max = 150) String adresseLivraison,
            /** Point exact sur la carte (facultatif). */
            Double latitude,
            Double longitude,
            /** true : le client vient chercher lui-même sa commande chez le vendeur (pas de livreur). */
            boolean retraitParClient
    ) {}

    public record PanierResponse(
            Long id,
            List<LigneResponse> lignes,
            int totalMmc
    ) {}
}
