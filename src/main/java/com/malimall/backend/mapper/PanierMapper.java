package com.malimall.backend.mapper;

import com.malimall.backend.dto.PanierDtos.LigneResponse;
import com.malimall.backend.dto.PanierDtos.PanierResponse;
import com.malimall.backend.entity.LigneCommande;
import com.malimall.backend.entity.Panier;

import java.util.List;

public final class PanierMapper {

    private PanierMapper() {}

    public static PanierResponse toResponse(Panier panier) {
        List<LigneResponse> lignes = panier.getLignes().stream()
                .map(PanierMapper::toLigneResponse)
                .toList();
        int total = lignes.stream().mapToInt(LigneResponse::sousTotal).sum();
        return new PanierResponse(panier.getId(), lignes, total);
    }

    private static LigneResponse toLigneResponse(LigneCommande ligne) {
        return new LigneResponse(
                ligne.getId(),
                ligne.getProduit().getId(),
                ligne.getProduit().getNom(),
                ligne.getProduit().getBoutique().getId(),
                ligne.getProduit().getBoutique().getNom(),
                ligne.getQuantite(),
                ligne.getPrixUnitaireMmc(),
                ligne.sousTotal());
    }
}
