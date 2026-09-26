package com.malimall.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "chauffeur")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Chauffeur {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_chauffeur")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "utilisateur_id", nullable = false, unique = true)
    private Utilisateur utilisateur;

    @Column(nullable = false)
    @Builder.Default
    private boolean disponible = true;

    @Column(name = "note_moyenne", nullable = false, precision = 2, scale = 1)
    @Builder.Default
    private BigDecimal noteMoyenne = new BigDecimal("5.0");

    @Column(name = "nombre_notes", nullable = false)
    @Builder.Default
    private int nombreNotes = 0;

    @Column(name = "numero_permis", length = 40)
    private String numeroPermis;

    /** Suspendu par un administrateur — ne peut plus accepter de courses ni de livraisons. */
    @Column(nullable = false)
    @Builder.Default
    private boolean suspendu = false;

    /** Dernière position GPS envoyée par l'application du chauffeur. */
    private Double latitude;

    private Double longitude;

    @Column(name = "position_maj")
    private Instant positionMaj;

    /** Côté inverse de Vehicule.chauffeur (aucune colonne ajoutée). */
    @OneToOne(mappedBy = "chauffeur", fetch = FetchType.LAZY)
    private Vehicule vehicule;

    /** Intègre une nouvelle note (1 à 5) dans la moyenne. */
    public void ajouterNote(int note) {
        BigDecimal total = noteMoyenne.multiply(BigDecimal.valueOf(nombreNotes)).add(BigDecimal.valueOf(note));
        nombreNotes += 1;
        noteMoyenne = total.divide(BigDecimal.valueOf(nombreNotes), 1, java.math.RoundingMode.HALF_UP);
    }
}
