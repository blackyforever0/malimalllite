package com.malimall.backend.entity;

import com.malimall.backend.entity.enums.StatutCourse;
import com.malimall.backend.entity.enums.TypeVehicule;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

/** Course MotoTaxi entre deux quartiers. */
@Entity
@Table(name = "course")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_course")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Utilisateur client;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chauffeur_id")
    private Chauffeur chauffeur;

    @Column(nullable = false, length = 100)
    private String depart;

    @Column(nullable = false, length = 100)
    private String destination;

    @Column(name = "distance_km", nullable = false, precision = 5, scale = 1)
    private BigDecimal distanceKm;

    @Column(name = "prix_mmc", nullable = false)
    private int prixMmc;

    /**
     * Tracé routier réel encodé (polyline Google), obtenu via Directions API
     * au moment de l'estimation — voir DirectionsService. Null si la clé
     * Directions n'est pas configurée ou si l'appel a échoué : le mobile
     * retombe alors sur une ligne droite entre départ et destination.
     */
    @Column(name = "itineraire_polyline", columnDefinition = "TEXT")
    private String itinerairePolyline;

    /**
     * Catégorie de véhicule demandée par le client (Scooter/Telimani = tarif
     * "léger", TriCycle = tarif supérieur) — détermine le tarif par palier
     * ET les chauffeurs à qui la demande est visible (voir CourseService).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "type_vehicule", nullable = false, length = 20)
    private TypeVehicule typeVehicule;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private StatutCourse statut = StatutCourse.DEMANDEE;

    /** Note (1 à 5) donnée au chauffeur par le client. */
    private Integer note;

    @Column(length = 500)
    private String commentaire;

    @Column(name = "date_demande", nullable = false, updatable = false)
    @Builder.Default
    private Instant dateDemande = Instant.now();

    @Column(name = "date_acceptation")
    private Instant dateAcceptation;

    @Column(name = "date_fin")
    private Instant dateFin;
}
