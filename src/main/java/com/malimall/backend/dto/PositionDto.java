package com.malimall.backend.dto;

import com.malimall.backend.entity.Boutique;
import com.malimall.backend.entity.Chauffeur;

import java.time.Duration;
import java.time.Instant;

/** Position GPS d'un chauffeur (ou d'une boutique) et date de sa dernière mise à jour. */
public record PositionDto(double latitude, double longitude, Instant maj) {

    /** Au-delà, la position n'est plus montrée : elle ne reflète plus la réalité. */
    public static final Duration FRAICHEUR_MAX = Duration.ofMinutes(10);

    /** Position récente du chauffeur, ou null s'il n'en a pas envoyé depuis 10 minutes. */
    public static PositionDto recente(Chauffeur chauffeur) {
        if (chauffeur == null || chauffeur.getLatitude() == null || chauffeur.getLongitude() == null
                || chauffeur.getPositionMaj() == null
                || chauffeur.getPositionMaj().isBefore(Instant.now().minus(FRAICHEUR_MAX))) {
            return null;
        }
        return new PositionDto(chauffeur.getLatitude(), chauffeur.getLongitude(), chauffeur.getPositionMaj());
    }

    /**
     * Position de la boutique, sans limite de fraîcheur : contrairement au
     * chauffeur (mobile, en direct), une boutique a une adresse fixe — la
     * position reste valable tant que le vendeur ne l'a pas explicitement
     * mise à jour (bouton "appuyez pour actualiser").
     */
    public static PositionDto deLaBoutique(Boutique boutique) {
        if (boutique == null || boutique.getLatitude() == null || boutique.getLongitude() == null
                || boutique.getPositionMaj() == null) {
            return null;
        }
        return new PositionDto(boutique.getLatitude(), boutique.getLongitude(), boutique.getPositionMaj());
    }
}
