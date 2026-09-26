package com.malimall.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import java.util.Optional;

/**
 * Itinéraire réel (tracé routier + distance), avec DEUX moteurs possibles :
 *
 *  1) OSRM (Open Source Routing Machine), serveur de démonstration public
 *     "router.project-osrm.org" — AUCUNE clé requise, essayé en premier.
 *     C'est un moteur de routage réel (OpenStreetMap), pas une simulation :
 *     il calcule un vrai tracé le long des rues. Son polyline encodé utilise
 *     le même format/précision que Google (compatible avec le même décodeur
 *     côté mobile, decodeGooglePolyline). Idéal ici : pas de clé à
 *     configurer, pas de facturation Google Cloud à activer.
 *
 *  2) Google Directions API — la même API que le vrai MaliMall (voir
 *     previewRoute côté Cloud Functions dans le vrai projet), utilisée
 *     seulement si malimall.google.maps-api-key est configurée ET qu'OSRM a
 *     échoué (utile si un jour une clé Google fonctionnelle est disponible).
 *
 * Dans les deux cas, appelé côté serveur uniquement, et jamais bloquant :
 * toute erreur (réseau, clé invalide, service indisponible…) renvoie
 * simplement Optional.empty(), et l'appelant (CourseService) retombe alors
 * sur l'estimation à vol d'oiseau (QuartiersBamako) — une course peut donc
 * toujours être demandée, même hors ligne ou sans aucune clé configurée.
 */
@Service
public class DirectionsService {

    private static final Logger log = LoggerFactory.getLogger(DirectionsService.class);

    /** Tracé + distance : le polyline est encodé (format Google/OSRM, précision 1e5), à décoder côté client. */
    public record Itineraire(String polylineEncodee, BigDecimal distanceKm) {}

    private final boolean googleConfigure;
    private final String googleApiKey;
    private final RestClient osrmClient;
    private final RestClient googleClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public DirectionsService(@Value("${malimall.google.maps-api-key:}") String googleApiKey) {
        this.googleApiKey = googleApiKey;
        this.googleConfigure = googleApiKey != null && !googleApiKey.isBlank();
        this.osrmClient = RestClient.create("https://router.project-osrm.org");
        this.googleClient = googleConfigure ? RestClient.create("https://maps.googleapis.com") : null;
    }

    public Optional<Itineraire> itineraire(double latDepart, double lonDepart, double latDestination, double lonDestination) {
        Optional<Itineraire> viaOsrm = itineraireOsrm(latDepart, lonDepart, latDestination, lonDestination);
        if (viaOsrm.isPresent()) {
            return viaOsrm;
        }
        if (googleConfigure) {
            return itineraireGoogle(latDepart, lonDepart, latDestination, lonDestination);
        }
        return Optional.empty();
    }

    private Optional<Itineraire> itineraireOsrm(double latDepart, double lonDepart, double latDestination, double lonDestination) {
        try {
            // OSRM prend les coordonnées en "lon,lat" (et non "lat,lon").
            String coordonnees = String.format(Locale.ROOT, "%f,%f;%f,%f", lonDepart, latDepart, lonDestination, latDestination);
            String corps = osrmClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/route/v1/driving/" + coordonnees)
                            .queryParam("overview", "full")
                            .queryParam("geometries", "polyline")
                            .build())
                    .retrieve()
                    .body(String.class);
            return parserOsrm(corps);
        } catch (Exception e) {
            log.warn("Appel OSRM échoué — tentative Google Directions (si configurée) ou repli sur l'estimation à vol d'oiseau : {}",
                    e.getMessage());
            return Optional.empty();
        }
    }

    private Optional<Itineraire> parserOsrm(String corpsJson) {
        try {
            JsonNode racine = objectMapper.readTree(corpsJson);
            if (!"Ok".equalsIgnoreCase(racine.path("code").asText())) {
                log.warn("OSRM a renvoyé le code {}", racine.path("code").asText());
                return Optional.empty();
            }
            JsonNode route = racine.path("routes").get(0);
            if (route == null) {
                return Optional.empty();
            }
            String polyline = route.path("geometry").asText(null);
            double distanceMetres = route.path("distance").asDouble(-1);
            if (polyline == null || polyline.isBlank() || distanceMetres < 0) {
                return Optional.empty();
            }
            BigDecimal distanceKm = BigDecimal.valueOf(Math.max(1.0, distanceMetres / 1000.0)).setScale(1, RoundingMode.HALF_UP);
            return Optional.of(new Itineraire(polyline, distanceKm));
        } catch (Exception e) {
            log.warn("Réponse OSRM illisible : {}", e.getMessage());
            return Optional.empty();
        }
    }

    private Optional<Itineraire> itineraireGoogle(double latDepart, double lonDepart, double latDestination, double lonDestination) {
        try {
            String corps = googleClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/maps/api/directions/json")
                            .queryParam("origin", latDepart + "," + lonDepart)
                            .queryParam("destination", latDestination + "," + lonDestination)
                            .queryParam("mode", "driving")
                            .queryParam("key", googleApiKey)
                            .build())
                    .retrieve()
                    .body(String.class);
            return parserGoogle(corps);
        } catch (Exception e) {
            log.warn("Appel Google Directions échoué — repli sur l'estimation à vol d'oiseau : {}", e.getMessage());
            return Optional.empty();
        }
    }

    private Optional<Itineraire> parserGoogle(String corpsJson) {
        try {
            JsonNode racine = objectMapper.readTree(corpsJson);
            if (!"OK".equals(racine.path("status").asText())) {
                log.warn("Google Directions a renvoyé le statut {}", racine.path("status").asText());
                return Optional.empty();
            }
            JsonNode route = racine.path("routes").get(0);
            if (route == null) {
                return Optional.empty();
            }
            String polyline = route.path("overview_polyline").path("points").asText(null);
            JsonNode legs = route.path("legs");
            if (polyline == null || polyline.isBlank() || !legs.isArray() || legs.isEmpty()) {
                return Optional.empty();
            }
            int distanceMetres = legs.get(0).path("distance").path("value").asInt(-1);
            if (distanceMetres < 0) {
                return Optional.empty();
            }
            BigDecimal distanceKm = BigDecimal.valueOf(Math.max(1.0, distanceMetres / 1000.0)).setScale(1, RoundingMode.HALF_UP);
            return Optional.of(new Itineraire(polyline, distanceKm));
        } catch (Exception e) {
            log.warn("Réponse Google Directions illisible : {}", e.getMessage());
            return Optional.empty();
        }
    }
}
