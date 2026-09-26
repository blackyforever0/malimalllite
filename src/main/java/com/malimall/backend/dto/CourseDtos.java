package com.malimall.backend.dto;

import com.malimall.backend.entity.enums.StatutCourse;
import com.malimall.backend.entity.enums.TypeVehicule;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;

public class CourseDtos {

    public record TrajetRequest(
            @NotBlank String depart,
            @NotBlank String destination,
            /** Catégorie de véhicule demandée — détermine le tarif par palier (QuartiersBamako). */
            @NotNull TypeVehicule typeVehicule
    ) {}

    public record EstimationResponse(
            String depart, String destination, BigDecimal distanceKm, int prixMmc, TypeVehicule typeVehicule,
            /** Tracé routier réel (polyline Google encodé) — null si Directions API indisponible (voir DirectionsService). */
            String itineraire
    ) {}

    public record NoterCourseRequest(@Min(1) @Max(5) int note, @Size(max = 500) String commentaire) {}

    /** Chauffeur disponible, affiché sur l'accueil MotoTaxi. */
    public record ChauffeurDisponibleResponse(
            Long chauffeurId,
            String nom,
            String vehiculeType,
            BigDecimal noteMoyenne,
            /** null si le chauffeur n'a pas partagé sa position récemment. */
            com.malimall.backend.dto.PositionDto position
    ) {}

    public record CourseResponse(
            Long id,
            String depart,
            String destination,
            BigDecimal distanceKm,
            int prixMmc,
            StatutCourse statut,
            String clientNom,
            String clientTelephone,
            Long chauffeurId,
            String chauffeurNom,
            String chauffeurTelephone,
            BigDecimal chauffeurNote,
            /** Catégorie demandée par le client à la création — connue avant même qu'un chauffeur accepte. */
            TypeVehicule typeVehiculeDemande,
            /** Véhicule réel du chauffeur assigné — null tant que personne n'a accepté. */
            String vehiculeType,
            String vehiculeImmatriculation,
            Integer note,
            Instant dateDemande,
            Instant dateAcceptation,
            Instant dateFin,
            /** Position en direct du chauffeur pendant la course (null sinon). */
            com.malimall.backend.dto.PositionDto chauffeurPosition,
            /** Tracé routier réel (polyline Google encodé) — null si Directions API indisponible. */
            String itineraire
    ) {}
}
