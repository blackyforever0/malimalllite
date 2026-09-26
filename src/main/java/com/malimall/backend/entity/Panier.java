package com.malimall.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Panier "courant" d'un Client — créé à la demande (premier ajouterLigne()),
 * vidé (les LigneCommande sont déplacées, pas copiées) à la validation.
 */
@Entity
@Table(name = "panier")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Panier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_panier")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false, unique = true)
    private Client client;

    @Column(name = "date_creation", nullable = false, updatable = false)
    @Builder.Default
    private Instant dateCreation = Instant.now();

    /**
     * PAS de orphanRemoval ici : au checkout, une ligne change de parent
     * (panier -&gt; commande) plutôt que d'être supprimée — orphanRemoval sur
     * une collection dont un enfant peut légitimement changer de propriétaire
     * est un piège JPA classique (suppression accidentelle au lieu d'un
     * ré-attachement). La suppression volontaire d'une ligne (retirerLigne)
     * est faite explicitement via le repository, jamais par simple retrait
     * de cette collection.
     */
    @OneToMany(mappedBy = "panier", cascade = CascadeType.ALL)
    @Builder.Default
    private List<LigneCommande> lignes = new ArrayList<>();
}
