package com.malimall.backend.mapper;

import com.malimall.backend.dto.PortefeuilleDtos.EcritureResponse;
import com.malimall.backend.dto.PortefeuilleDtos.PortefeuilleResponse;
import com.malimall.backend.entity.EcritureComptable;
import com.malimall.backend.entity.Portefeuille;

public final class PortefeuilleMapper {

    private PortefeuilleMapper() {}

    public static PortefeuilleResponse toResponse(Portefeuille portefeuille) {
        return new PortefeuilleResponse(
                portefeuille.getSoldeMmc(),
                portefeuille.getSoldeGeleMmc(),
                portefeuille.soldeDisponible());
    }

    public static EcritureResponse toResponse(EcritureComptable ecriture) {
        return new EcritureResponse(
                ecriture.getId(),
                ecriture.getTypeOperation(),
                ecriture.getMontantMmc(),
                ecriture.getReferenceType(),
                ecriture.getReferenceId(),
                ecriture.getDateCreation());
    }
}
