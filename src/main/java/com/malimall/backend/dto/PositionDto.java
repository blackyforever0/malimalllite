package com.malimall.backend.dto;

import com.malimall.backend.entity.Chauffeur;

import java.time.Duration;
import java.time.Instant;

/** Position GPS d'un chauffeur et date de sa dernière mise à jour. */
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
}
