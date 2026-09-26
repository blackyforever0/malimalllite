package com.malimall.backend.dto;

import java.math.BigDecimal;

public class LivreurDtos {

    public record DisponibiliteRequest(boolean disponible) {}

    /** Position GPS envoyée périodiquement par l'application du chauffeur. */
    public record PositionRequest(double latitude, double longitude) {}

    /** Tableau de bord de l'Espace livreur (maquette 18). */
    public record EspaceLivreurResponse(
            Long chauffeurId,
            boolean disponible,
            String vehiculeType,
            String immatriculation,
            /** null = pas de limite (TriCycle). */
            Integer capaciteMaxKg,
            boolean transportePassagers,
            BigDecimal noteMoyenne,
            long coursesEtLivraisonsTerminees,
            long gainsMmc,
            long livraisonsEnAttente,
            long coursesEnAttente
    ) {}
}
