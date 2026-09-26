package com.malimall.backend.mapper;

import com.malimall.backend.dto.CatalogueDtos.BoutiqueResponse;
import com.malimall.backend.entity.Boutique;

/** Traducteur Entité -&gt; DTO, sans état, pour garder les contrôleurs minces. */
public final class BoutiqueMapper {

    private BoutiqueMapper() {}

    public static BoutiqueResponse toResponse(Boutique boutique) {
        return new BoutiqueResponse(
                boutique.getId(),
                boutique.getNom(),
                boutique.getDescription(),
                boutique.getCategorie(),
                boutique.isCertifiee(),
                boutique.getProprietaire().getId(),
                boutique.getImageUrl(),
                boutique.getQuartier());
    }
}
