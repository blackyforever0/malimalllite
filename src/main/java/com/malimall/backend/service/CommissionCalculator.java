package com.malimall.backend.service;

import com.malimall.backend.config.MaliMallProperties;
import org.springframework.stereotype.Component;

/**
 * Règle de commission de la plateforme MaliMall, appliquée à la
 * confirmation de livraison : 8 % sur le total produits ET 8 % séparément
 * sur les frais de livraison. Pur — aucune
 * dépendance à la base de données, entièrement testable unitairement.
 */
@Component
public class CommissionCalculator {

    private final double taux;

    public CommissionCalculator(MaliMallProperties properties) {
        this.taux = properties.commission().taux();
    }

    public record CommissionSplit(int sellerReceives, int driverReceives, int platformCommission) {
        public int totalCommission() {
            return platformCommission;
        }
    }

    /**
     * Répartition d'une livraison Market menée à bien normalement :
     * productCommission = round(montantTotalMmc * taux) ; sellerReceives = montantTotalMmc - productCommission
     * deliveryCommission = round(fraisLivraisonMmc * taux) ; driverReceives = fraisLivraisonMmc - deliveryCommission
     */
    public CommissionSplit splitLivraisonNormale(int montantTotalMmc, int fraisLivraisonMmc) {
        int productCommission = Math.round(montantTotalMmc * (float) taux);
        int sellerReceives = montantTotalMmc - productCommission;

        int deliveryCommission = Math.round(fraisLivraisonMmc * (float) taux);
        int driverReceives = fraisLivraisonMmc - deliveryCommission;

        return new CommissionSplit(sellerReceives, driverReceives, productCommission + deliveryCommission);
    }

    /**
     * Répartition en cas d'annulation après retrait (le colis a déjà été
     * récupéré chez le vendeur quand l'acheteur annule) : le livreur garde
     * 5 % du prix produit + 100 % des frais de livraison, le vendeur garde
     * 10 % du prix produit — les deux montants étant eux-mêmes réduits de
     * 8 % de commission plateforme. Le reste des fonds gelés retourne à
     * l'acheteur. Formule documentée et testée ici ; l'endpoint qui
     * l'utilise n'est pas branché dans ce lot (voir le rapport de plan).
     */
    public record CompensationSplit(int driverReceives, int driverCommission, int sellerBonus, int sellerCommission) {
        public int totalCommission() {
            return driverCommission + sellerCommission;
        }

        public int buyerLoses(int driverReceives0, int sellerBonus0) {
            return driverReceives0 + sellerBonus0;
        }
    }

    public CompensationSplit splitAnnulationApresRetrait(int montantTotalMmc, int fraisLivraisonMmc) {
        float rawDriverShare = fraisLivraisonMmc + montantTotalMmc * 0.05f;
        int driverReceives = Math.round(rawDriverShare * (float) (1 - taux));
        int driverCommission = Math.round(rawDriverShare * (float) taux);

        float rawSellerShare = montantTotalMmc * 0.10f;
        int sellerBonus = Math.round(rawSellerShare * (float) (1 - taux));
        int sellerCommission = Math.round(rawSellerShare * (float) taux);

        return new CompensationSplit(driverReceives, driverCommission, sellerBonus, sellerCommission);
    }
}
