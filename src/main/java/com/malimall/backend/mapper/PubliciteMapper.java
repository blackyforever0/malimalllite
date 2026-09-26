package com.malimall.backend.mapper;

import com.malimall.backend.dto.PubliciteDtos.PubliciteResponse;
import com.malimall.backend.entity.Publicite;
import com.malimall.backend.entity.enums.StatutPublicite;

import java.time.Instant;

public final class PubliciteMapper {

    private PubliciteMapper() {}

    public static PubliciteResponse toResponse(Publicite p) {
        // EXPIREE n'est jamais stocké en base — calculé ici à la lecture dès
        // que la date d'expiration est dépassée (voir StatutPublicite).
        String statutAffiche = (p.getStatut() == StatutPublicite.ACTIVE
                && p.getDateExpiration() != null
                && p.getDateExpiration().isBefore(Instant.now()))
                ? StatutPublicite.EXPIREE.name()
                : p.getStatut().name();

        return new PubliciteResponse(
                p.getId(),
                p.getBoutique().getId(),
                p.getBoutique().getNom(),
                p.getTitre(),
                p.getImageUrl(),
                p.getDureeJours(),
                p.getPrixPayeMmc(),
                statutAffiche,
                p.getDateCreation(),
                p.getDateActivation(),
                p.getDateExpiration());
    }
}
