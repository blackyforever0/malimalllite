package com.malimall.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

public class AuthDtos {

    public record InscriptionRequest(
            @NotBlank String nom,
            @NotBlank String prenom,
            @NotBlank String telephone,
            @Email String email,
            @NotBlank @Size(min = 6, message = "au moins 6 caractères") String motDePasse
    ) {}

    public record ConnexionRequest(
            @NotBlank String telephone,
            @NotBlank String motDePasse
    ) {}

    public record AuthResponse(
            String token,
            Long utilisateurId,
            List<String> roles
    ) {}

    /**
     * vendeurId / chauffeurId : id de la spécialisation Vendeur/Chauffeur de
     * cet utilisateur si elle existe, sinon null. Ajouté pour que le client
     * (app Flutter) puisse retrouver "sa" boutique ou reconnaître ses propres
     * livraisons sans avoir à mémoriser cet id côté client au moment de
     * devenir-vendeur/devenir-chauffeur (utile après une réinstallation ou une
     * connexion sur un nouvel appareil).
     */
    public record MoiResponse(
            Long id,
            String nom,
            String prenom,
            String telephone,
            String email,
            List<String> roles,
            Long vendeurId,
            Long chauffeurId,
            String imageUrl,
            boolean verifie,
            Instant dateCreation
    ) {}
}
