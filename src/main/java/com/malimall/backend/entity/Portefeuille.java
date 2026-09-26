package com.malimall.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Un portefeuille MMC par utilisateur (y compris le portefeuille "système"
 * de la plateforme, qui est un Utilisateur seedé comme les autres — aucun
 * cas spécial dans le code, {@link com.malimall.backend.service.PortefeuilleService}
 * traite tout le monde pareil).
 *
 * soldeDisponible = soldeMMC - soldeGeleMMC. Toute mutation DOIT passer par
 * PortefeuilleService ; ne jamais appeler save() directement depuis un
 * contrôleur ou un autre service.
 */
@Entity
@Table(name = "portefeuille")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Portefeuille {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_portefeuille")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "utilisateur_id", nullable = false, unique = true)
    private Utilisateur utilisateur;

    @Column(name = "solde_mmc", nullable = false)
    @Builder.Default
    private int soldeMmc = 0;

    @Column(name = "solde_gele_mmc", nullable = false)
    @Builder.Default
    private int soldeGeleMmc = 0;

    /** Verrou optimiste en complément du verrou pessimiste pris en lecture par le repository. */
    @Version
    @Column(name = "version")
    private Long version;

    @Column(name = "date_maj", nullable = false)
    @Builder.Default
    private Instant dateMaj = Instant.now();

    @Transient
    public int soldeDisponible() {
        return soldeMmc - soldeGeleMmc;
    }
}
