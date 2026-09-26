package com.malimall.backend.entity;

import com.malimall.backend.entity.enums.TypeOperation;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Journal (ledger) : trace CHAQUE mouvement de MMC réalisé (jamais les
 * gels/dégels, qui ne sont pas des mouvements réalisés — seuls les
 * mouvements définitifs sont journalisés).
 */
@Entity
@Table(name = "ecriture_comptable")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EcritureComptable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_ecriture")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "utilisateur_id", nullable = false)
    private Utilisateur utilisateur;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_operation", nullable = false, length = 20)
    private TypeOperation typeOperation;

    @Column(name = "montant_mmc", nullable = false)
    private int montantMmc;

    @Column(name = "reference_type", length = 20)
    private String referenceType;

    @Column(name = "reference_id")
    private Long referenceId;

    @Column(name = "date_creation", nullable = false, updatable = false)
    @Builder.Default
    private Instant dateCreation = Instant.now();
}
