package com.malimall.backend.entity;

import com.malimall.backend.entity.enums.StatutVerification;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "utilisateur")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Utilisateur {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_utilisateur")
    private Long id;

    @Column(nullable = false, length = 100)
    private String nom;

    @Column(nullable = false, length = 100)
    private String prenom;

    @Column(nullable = false, unique = true, length = 20)
    private String telephone;

    @Column(unique = true, length = 150)
    private String email;

    /** Haché (BCrypt), jamais en clair. */
    @Column(name = "mot_de_passe", nullable = false, length = 255)
    private String motDePasse;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut_verification", nullable = false, length = 20)
    @Builder.Default
    private StatutVerification statutVerification = StatutVerification.NON_VERIFIE;

    @Column(name = "est_admin", nullable = false)
    @Builder.Default
    private boolean estAdmin = false;

    /** Suspendu par un administrateur — connexion refusée (voir UserPrincipal.isEnabled()). */
    @Column(nullable = false)
    @Builder.Default
    private boolean suspendu = false;

    /** Photo de profil — URL publique Supabase Storage, nullable. */
    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "date_creation", nullable = false, updatable = false)
    @Builder.Default
    private Instant dateCreation = Instant.now();
}
