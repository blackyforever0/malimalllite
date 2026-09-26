package com.malimall.backend.entity.enums;

/**
 * Cycle de vie d'une course MotoTaxi : DEMANDEE (fonds du client gelés) ->
 * ACCEPTEE (un chauffeur vient chercher le client) -> TERMINEE (fonds
 * réglés au chauffeur). ANNULEE : le client annule avant l'acceptation,
 * ses fonds sont libérés.
 */
public enum StatutCourse {
    DEMANDEE,
    ACCEPTEE,
    TERMINEE,
    ANNULEE
}
