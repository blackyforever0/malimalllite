package com.malimall.backend.dto;

import com.malimall.backend.entity.enums.TypeVehicule;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class ProfilDtos {

    public record DevenirChauffeurRequest(
            @NotNull TypeVehicule typeVehicule,
            @NotBlank String immatriculation,
            /** Facultatif. */
            String numeroPermis
    ) {}
}
