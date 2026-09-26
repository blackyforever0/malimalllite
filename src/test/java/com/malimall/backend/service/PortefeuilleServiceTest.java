package com.malimall.backend.service;

import com.malimall.backend.entity.Portefeuille;
import com.malimall.backend.entity.Utilisateur;
import com.malimall.backend.exception.FondsInsuffisantsException;
import com.malimall.backend.exception.RessourceIntrouvableException;
import com.malimall.backend.repository.EcritureComptableRepository;
import com.malimall.backend.repository.PortefeuilleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * PortefeuilleService est le SEUL point d'écriture sur un Portefeuille (cf. sa
 * javadoc — jamais de repository.save() direct ailleurs). Ces tests isolent
 * chaque mutation avec des mocks de repository, sans base de données : le
 * parcours complet avec une vraie base est couvert par
 * {@code ParcoursMarketIntegrationTest}.
 */
@ExtendWith(MockitoExtension.class)
class PortefeuilleServiceTest {

    private static final Long UTILISATEUR_ID = 42L;

    @Mock
    private PortefeuilleRepository portefeuilleRepository;
    @Mock
    private EcritureComptableRepository ecritureComptableRepository;

    @InjectMocks
    private PortefeuilleService portefeuilleService;

    private Portefeuille portefeuille;

    @BeforeEach
    void setUp() {
        portefeuille = Portefeuille.builder()
                .id(1L)
                .utilisateur(Utilisateur.builder().id(UTILISATEUR_ID).build())
                .soldeMmc(1000)
                .soldeGeleMmc(200)
                .build();
    }

    @Test
    void geler_reserveLesFondsQuandLeSoldeDisponibleSuffit() {
        when(portefeuilleRepository.findByUtilisateurIdForUpdate(UTILISATEUR_ID)).thenReturn(Optional.of(portefeuille));
        when(portefeuilleRepository.save(any(Portefeuille.class))).thenAnswer(inv -> inv.getArgument(0));

        Portefeuille resultat = portefeuilleService.geler(UTILISATEUR_ID, 500);

        assertThat(resultat.getSoldeGeleMmc()).isEqualTo(700);
        assertThat(resultat.getSoldeMmc()).isEqualTo(1000); // le solde réel ne bouge pas encore, seul le gel change
    }

    @Test
    void geler_refuseQuandLeSoldeDisponibleEstInsuffisant() {
        when(portefeuilleRepository.findByUtilisateurIdForUpdate(UTILISATEUR_ID)).thenReturn(Optional.of(portefeuille));
        // disponible = 1000 - 200 = 800

        assertThatThrownBy(() -> portefeuilleService.geler(UTILISATEUR_ID, 801))
                .isInstanceOf(FondsInsuffisantsException.class);

        verify(portefeuilleRepository, never()).save(any());
    }

    @Test
    void debloquer_neDescendJamaisSousZero() {
        when(portefeuilleRepository.findByUtilisateurIdForUpdate(UTILISATEUR_ID)).thenReturn(Optional.of(portefeuille));
        when(portefeuilleRepository.save(any(Portefeuille.class))).thenAnswer(inv -> inv.getArgument(0));

        Portefeuille resultat = portefeuilleService.debloquer(UTILISATEUR_ID, 5000); // bien plus que soldeGeleMmc (200)

        assertThat(resultat.getSoldeGeleMmc()).isZero();
    }

    @Test
    void crediter_augmenteLeSoldeReelSansToucherAuGel() {
        when(portefeuilleRepository.findByUtilisateurIdForUpdate(UTILISATEUR_ID)).thenReturn(Optional.of(portefeuille));
        when(portefeuilleRepository.save(any(Portefeuille.class))).thenAnswer(inv -> inv.getArgument(0));

        Portefeuille resultat = portefeuilleService.crediter(UTILISATEUR_ID, 276);

        assertThat(resultat.getSoldeMmc()).isEqualTo(1276);
        assertThat(resultat.getSoldeGeleMmc()).isEqualTo(200);
    }

    @Test
    void debiter_debiteLeSoldeReelEtLibereLesFondsGelesCorrespondants() {
        when(portefeuilleRepository.findByUtilisateurIdForUpdate(UTILISATEUR_ID)).thenReturn(Optional.of(portefeuille));
        when(portefeuilleRepository.save(any(Portefeuille.class))).thenAnswer(inv -> inv.getArgument(0));

        Portefeuille resultat = portefeuilleService.debiter(UTILISATEUR_ID, 200);

        assertThat(resultat.getSoldeMmc()).isEqualTo(800);
        assertThat(resultat.getSoldeGeleMmc()).isZero();
    }

    @Test
    void obtenirPourUtilisateur_leveUneExceptionQuandAucunPortefeuille() {
        when(portefeuilleRepository.findByUtilisateurId(UTILISATEUR_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> portefeuilleService.obtenirPourUtilisateur(UTILISATEUR_ID))
                .isInstanceOf(RessourceIntrouvableException.class);
    }
}
