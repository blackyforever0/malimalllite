package com.malimall.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Regroupe les valeurs de configuration métier (non liées à Spring lui-même) :
 * secret JWT, taux de commission de la plateforme, frais de livraison fixes
 * pour ce lot (pas encore de moteur de tarification à la distance), et le
 * numéro de téléphone sentinelle du portefeuille plateforme.
 */
@ConfigurationProperties(prefix = "malimall")
public record MaliMallProperties(Jwt jwt, Commission commission, Livraison livraison, Plateforme plateforme) {

    public record Jwt(String secret, long expirationMinutes) {}

    public record Commission(double taux) {}

    public record Livraison(int fraisFixeMmc) {}

    public record Plateforme(String telephone) {}
}
