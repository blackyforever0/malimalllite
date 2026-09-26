package com.malimall.backend.service;

import com.malimall.backend.entity.enums.TypeVehicule;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Quartiers de Bamako desservis, avec leurs coordonnées approximatives.
 * Sert à estimer la distance d'une course (à vol d'oiseau, majorée de 30 %
 * pour tenir compte du tracé des routes) et donc son prix — aucune API de
 * cartographie externe (Directions ou autre) n'est nécessaire : le tarif
 * est un forfait par palier de distance, pas un calcul au km, donc une
 * estimation à vol d'oiseau suffit largement à placer le trajet dans le bon
 * palier.
 */
@Component
public class QuartiersBamako {

    /**
     * Tarif par palier de distance (FCFA), converti en MMC (1 MMC = 5 FCFA) :
     * Scooter/Telimani ("léger") 1000f jusqu'à 3 km, double (2000f) jusqu'à
     * 6 km, triple (3000f) au-delà ; TriCycle 2500f jusqu'à 3 km, même
     * doublement/triplement (5000f / 7500f).
     */
    private static final double MAJORATION_ROUTE = 1.3;

    private static final Map<String, double[]> QUARTIERS = new LinkedHashMap<>();

    static {
        QUARTIERS.put("ACI 2000", new double[]{12.6280, -8.0200});
        QUARTIERS.put("Badalabougou", new double[]{12.6250, -7.9950});
        QUARTIERS.put("Baco Djicoroni", new double[]{12.6000, -8.0150});
        QUARTIERS.put("Centre-ville", new double[]{12.6400, -7.9990});
        QUARTIERS.put("Djicoroni Para", new double[]{12.6380, -8.0520});
        QUARTIERS.put("Faladié", new double[]{12.6000, -7.9600});
        QUARTIERS.put("Hamdallaye", new double[]{12.6420, -8.0290});
        QUARTIERS.put("Hippodrome", new double[]{12.6520, -7.9960});
        QUARTIERS.put("Kalaban Coura", new double[]{12.5780, -8.0000});
        QUARTIERS.put("Lafiabougou", new double[]{12.6450, -8.0450});
        QUARTIERS.put("Magnambougou", new double[]{12.6070, -7.9500});
        QUARTIERS.put("Médina Coura", new double[]{12.6560, -7.9870});
        QUARTIERS.put("Missira", new double[]{12.6600, -7.9960});
        QUARTIERS.put("Niaréla", new double[]{12.6510, -7.9830});
        QUARTIERS.put("Sabalibougou", new double[]{12.5930, -7.9740});
        QUARTIERS.put("Sotuba", new double[]{12.6600, -7.9230});
        QUARTIERS.put("Torokorobougou", new double[]{12.6120, -7.9880});
    }

    public List<String> noms() {
        return List.copyOf(QUARTIERS.keySet());
    }

    public boolean existe(String quartier) {
        return quartier != null && QUARTIERS.containsKey(quartier);
    }

    /** Coordonnées d'un quartier (lat, lon) — utilisées pour interroger Directions API (DirectionsService). */
    public record Coordonnee(double lat, double lon) {}

    public Coordonnee coordonnee(String quartier) {
        double[] c = coordonnees(quartier);
        return new Coordonnee(c[0], c[1]);
    }

    /** Distance routière estimée en km (une décimale, 1,0 km minimum). */
    public BigDecimal distanceKm(String depart, String destination) {
        double[] a = coordonnees(depart);
        double[] b = coordonnees(destination);
        double km = haversineKm(a[0], a[1], b[0], b[1]) * MAJORATION_ROUTE;
        return BigDecimal.valueOf(Math.max(1.0, km)).setScale(1, RoundingMode.HALF_UP);
    }

    /** Tarif par palier de distance selon la catégorie de véhicule demandée. */
    public int prixMmc(BigDecimal distanceKm, TypeVehicule typeVehicule) {
        double km = distanceKm.doubleValue();
        int prixFcfa = switch (typeVehicule) {
            case TRICYCLE -> km <= 3.0 ? 2500 : km <= 6.0 ? 5000 : 7500;
            case SCOOTER, TELIMANI -> km <= 3.0 ? 1000 : km <= 6.0 ? 2000 : 3000;
        };
        return prixFcfa / 5;
    }

    private double[] coordonnees(String quartier) {
        double[] c = QUARTIERS.get(quartier);
        if (c == null) {
            throw new IllegalArgumentException("Quartier inconnu : " + quartier);
        }
        return c;
    }

    private static double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        double r = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double h = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return 2 * r * Math.asin(Math.sqrt(h));
    }
}
