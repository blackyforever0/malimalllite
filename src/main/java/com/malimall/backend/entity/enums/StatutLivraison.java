package com.malimall.backend.entity.enums;

/**
 * Sous-état de livraison d'une Commande, distinct de {@link StatutCommande}.
 * Ne devient pertinent qu'à partir du moment où statut = EN_LIVRAISON ;
 * reste null tant qu'aucun livreur n'a été assigné.
 */
public enum StatutLivraison {
    EN_ATTENTE_LIVREUR,
    ASSIGNEE,
    RECUPEREE,
    EN_ROUTE,
    LIVREE
}
