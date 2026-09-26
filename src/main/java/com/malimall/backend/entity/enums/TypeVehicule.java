package com.malimall.backend.entity.enums;

/**
 * Scooter : livraison seule (objets portables en sac), ≤ 10 kg, pas de passager.
 * Telimani : passager OU livraison de 0 à 20 kg.
 * TriCycle : livraison seule, charges lourdes/volumineuses (&gt; 20 kg), pas de passager.
 */
public enum TypeVehicule {
    SCOOTER,
    TELIMANI,
    TRICYCLE
}
