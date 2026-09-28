package com.malimall.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "boutique")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Boutique {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_boutique")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proprietaire_id", nullable = false)
    private Vendeur proprietaire;

    @Column(nullable = false, length = 150)
    private String nom;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 50)
    private String categorie;

    /** Quartier de la boutique (point de retrait des colis). */
    @Column(length = 100)
    private String quartier;

    /** URL publique Supabase Storage ; null si aucune photo n'a encore été téléversée. */
    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(nullable = false)
    @Builder.Default
    private boolean certifiee = false;

    /** Suspendue par un administrateur — n'apparaît plus dans le catalogue public. */
    @Column(nullable = false)
    @Builder.Default
    private boolean suspendue = false;

    @Column(name = "date_creation", nullable = false, updatable = false)
    @Builder.Default
    private Instant dateCreation = Instant.now();

    /**
     * Position GPS enregistrée par le vendeur (bouton "Ma boutique" — même
     * principe que Chauffeur.latitude/longitude/positionMaj), pour que les
     * livreurs sachent où récupérer les colis chez lui.
     */
    private Double latitude;

    private Double longitude;

    @Column(name = "position_maj")
    private Instant positionMaj;
}
