package com.malimall.backend.service;

import com.malimall.backend.config.MaliMallProperties;
import com.malimall.backend.service.CommissionCalculator.CommissionSplit;
import com.malimall.backend.service.CommissionCalculator.CompensationSplit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * CommissionCalculator est pur (aucune dépendance base de données ni Spring) :
 * ces tests fixent la règle de commission — 8 % séparément sur le montant
 * produit et sur les frais de livraison — avec les mêmes montants que le
 * parcours vérifié manuellement via Swagger (1800 MMC produit + 300 MMC
 * livraison -> vendeur 1656, chauffeur 276, plateforme 168), pour qu'une
 * régression future soit détectée immédiatement, sans repasser par toute
 * l'application.
 */
class CommissionCalculatorTest {

    private CommissionCalculator calculator;

    @BeforeEach
    void setUp() {
        MaliMallProperties properties = new MaliMallProperties(
                new MaliMallProperties.Jwt("test-secret-key-au-moins-32-caracteres!!", 120),
                new MaliMallProperties.Commission(0.08),
                new MaliMallProperties.Livraison(300),
                new MaliMallProperties.Plateforme("+000000000"));
        calculator = new CommissionCalculator(properties);
    }

    @Test
    @DisplayName("1800 MMC produit + 300 MMC livraison -> vendeur 1656, chauffeur 276, plateforme 168 (parcours vérifié manuellement)")
    void splitLivraisonNormale_casVerifieManuellement() {
        CommissionSplit split = calculator.splitLivraisonNormale(1800, 300);

        assertThat(split.sellerReceives()).isEqualTo(1656);
        assertThat(split.driverReceives()).isEqualTo(276);
        assertThat(split.platformCommission()).isEqualTo(168);
        assertThat(split.totalCommission()).isEqualTo(168);
    }

    @Test
    void splitLivraisonNormale_neFaitDisparaitreAucunMmc() {
        int montantTotal = 1000;
        int fraisLivraison = 300;

        CommissionSplit split = calculator.splitLivraisonNormale(montantTotal, fraisLivraison);

        assertThat(split.sellerReceives()).isEqualTo(920);
        assertThat(split.driverReceives()).isEqualTo(276);
        assertThat(split.platformCommission()).isEqualTo(104);
        // Le partage doit toujours conserver le total (rien ne doit apparaître ni disparaître à l'arrondi).
        assertThat(split.sellerReceives() + split.driverReceives() + split.platformCommission())
                .isEqualTo(montantTotal + fraisLivraison);
    }

    @Test
    void splitLivraisonNormale_montantsPlusEleves() {
        CommissionSplit split = calculator.splitLivraisonNormale(2500, 500);

        assertThat(split.sellerReceives()).isEqualTo(2300);
        assertThat(split.driverReceives()).isEqualTo(460);
        assertThat(split.platformCommission()).isEqualTo(240);
    }

    @Test
    void splitLivraisonNormale_montantNul() {
        CommissionSplit split = calculator.splitLivraisonNormale(0, 0);

        assertThat(split.sellerReceives()).isZero();
        assertThat(split.driverReceives()).isZero();
        assertThat(split.platformCommission()).isZero();
    }

    @Test
    @DisplayName("Formule d'annulation après retrait (endpoint non branché ce lot, mais la logique doit rester correcte et testée)")
    void splitAnnulationApresRetrait_calculIndependantVerifie() {
        CompensationSplit split = calculator.splitAnnulationApresRetrait(1800, 300);

        assertThat(split.driverReceives()).isEqualTo(359);
        assertThat(split.driverCommission()).isEqualTo(31);
        assertThat(split.sellerBonus()).isEqualTo(166);
        assertThat(split.sellerCommission()).isEqualTo(14);
        assertThat(split.totalCommission()).isEqualTo(45);
    }
}
