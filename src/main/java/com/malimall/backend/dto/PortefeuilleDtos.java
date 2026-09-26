package com.malimall.backend.dto;

import com.malimall.backend.entity.enums.TypeOperation;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

import java.time.Instant;

public class PortefeuilleDtos {

    public record PortefeuilleResponse(
            int soldeMmc,
            int soldeGeleMmc,
            int soldeDisponible
    ) {}

    public record EcritureResponse(
            Long id,
            TypeOperation typeOperation,
            int montantMmc,
            String referenceType,
            Long referenceId,
            Instant dateCreation
    ) {}

    /**
     * Recharge/retrait simulés via un opérateur mobile money (Orange Money,
     * Moov Money, Wave, Sama Money) — voir README simulation : aucun appel
     * réel à ces opérateurs, {@code operateur} et {@code telephoneOperateur}
     * ne sont là que pour l'expérience (écran de confirmation), jamais
     * persistés tels quels côté comptable.
     */
    public record RechargeRequest(@Positive int montantMmc, @NotBlank String operateur, @NotBlank String telephoneOperateur) {}

    public record RetraitRequest(@Positive int montantMmc, @NotBlank String operateur, @NotBlank String telephoneOperateur) {}

    public record TransfertRequest(@Positive int montantMmc, @NotBlank String telephoneDestinataire) {}

    public record TransfertResponse(PortefeuilleResponse portefeuille, String destinataireNomComplet) {}
}
