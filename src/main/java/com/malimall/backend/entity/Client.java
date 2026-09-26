package com.malimall.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Spécialisation de base : créée automatiquement pour tout {@link Utilisateur}
 * à l'inscription (la capacité d'achat est le socle commun). Vendeur /
 * Chauffeur / Admin restent des spécialisations additionnelles, optionnelles
 * et cumulables.
 */
@Entity
@Table(name = "client")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Client {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_client")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "utilisateur_id", nullable = false, unique = true)
    private Utilisateur utilisateur;

    @Column(name = "adresse_livraison_defaut", length = 255)
    private String adresseLivraisonDefaut;
}
