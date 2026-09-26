package com.malimall.backend.entity;

import com.malimall.backend.entity.enums.TypeVehicule;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "vehicule")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Vehicule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_vehicule")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chauffeur_id", nullable = false, unique = true)
    private Chauffeur chauffeur;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TypeVehicule type;

    @Column(nullable = false, length = 20)
    private String immatriculation;

    /** Capacité maximale de charge, dérivée du type — pas de colonne dédiée. */
    @Transient
    public int capaciteMaxKg() {
        return switch (type) {
            case SCOOTER -> 10;
            case TELIMANI -> 20;
            case TRICYCLE -> Integer.MAX_VALUE;
        };
    }

    @Transient
    public boolean transportePassager() {
        return type == TypeVehicule.TELIMANI;
    }
}
