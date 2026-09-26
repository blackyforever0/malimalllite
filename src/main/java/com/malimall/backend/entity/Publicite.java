package com.malimall.backend.entity;

import com.malimall.backend.entity.enums.StatutPublicite;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Demande de diffusion payante d'une bannière sur l'accueil Market — voir
 * PubliciteService pour le cycle de vie (paiement immédiat à la demande,
 * activation après validation admin).
 */
@Entity
@Table(name = "publicite")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Publicite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_publicite")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "boutique_id", nullable = false)
    private Boutique boutique;

    @Column(nullable = false, length = 100)
    private String titre;

    /** URL publique Supabase Storage ; null si aucune image n'a encore été téléversée. */
    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "duree_jours", nullable = false)
    private int dureeJours;

    @Column(name = "prix_paye_mmc", nullable = false)
    private int prixPayeMmc;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatutPublicite statut;

    @Column(name = "date_creation", nullable = false, updatable = false)
    @Builder.Default
    private Instant dateCreation = Instant.now();

    /** Renseignée uniquement au moment de la validation admin. */
    @Column(name = "date_activation")
    private Instant dateActivation;

    /** dateActivation + dureeJours, calculée à la validation admin. */
    @Column(name = "date_expiration")
    private Instant dateExpiration;
}
