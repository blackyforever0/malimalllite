package com.malimall.backend.entity.enums;

/**
 * Cycle de vie d'une {@link com.malimall.backend.entity.Publicite} :
 * demande créée en EN_ATTENTE (fonds déjà débités du vendeur, voir
 * PubliciteService), un admin la fait passer à ACTIVE (diffusion commence,
 * date d'expiration calculée) ou REFUSEE (pas de remboursement). EXPIREE
 * est calculé côté lecture
 * (dateExpiration dépassée), jamais un statut stocké en base.
 */
public enum StatutPublicite {
    EN_ATTENTE,
    ACTIVE,
    EXPIREE,
    REFUSEE
}
