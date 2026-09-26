package com.malimall.backend.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public class PubliciteDtos {

    public record CreerPubliciteRequest(
            @NotNull Long boutiqueId,
            @NotBlank @Size(max = 100) String titre,
            /** 1 à 30 jours, facturés au tarif journalier (PubliciteService.PRIX_PAR_JOUR_MMC). */
            @Min(1) @Max(30) int dureeJours
    ) {}

    public record PubliciteResponse(
            Long id,
            Long boutiqueId,
            String boutiqueNom,
            String titre,
            String imageUrl,
            int dureeJours,
            int prixPayeMmc,
            String statut,
            Instant dateCreation,
            Instant dateActivation,
            Instant dateExpiration
    ) {}
}
