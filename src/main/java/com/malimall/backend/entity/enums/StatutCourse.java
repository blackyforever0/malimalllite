package com.malimall.backend.entity.enums;

/**
 * Cycle de vie d'une course MotoTaxi : DEMANDEE -> ACCEPTEE -> EN_COURS
 * (passager pris en charge) -> ARRIVEE (arrivée signalée par le chauffeur)
 * -> TERMINEE après confirmation du passager et règlement. ANNULEE : le
 * client annule avant l'acceptation et ses fonds sont libérés.
 */
public enum StatutCourse {
    DEMANDEE,
    ACCEPTEE,
    EN_COURS,
    ARRIVEE,
    TERMINEE,
    ANNULEE
}
