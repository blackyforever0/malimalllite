package com.malimall.backend.entity.enums;

/**
 * Type d'écriture dans le journal comptable ({@code ecriture_comptable}).
 * Couvre les ventes, frais de livraison et commissions, ainsi que le débit
 * acheteur (ACHAT).
 */
public enum TypeOperation {
    RECHARGE,
    RETRAIT,
    ACHAT,
    VENTE,
    FRAIS_LIVRAISON,
    COMMISSION,
    COURSE_PAIEMENT,
    COURSE_REVENU,
    PUBLICITE,
    TRANSFERT_ENVOYE,
    TRANSFERT_RECU
}
