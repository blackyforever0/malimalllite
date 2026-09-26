package com.malimall.backend.service;

import com.malimall.backend.dto.PubliciteDtos.CreerPubliciteRequest;
import com.malimall.backend.entity.Boutique;
import com.malimall.backend.entity.Publicite;
import com.malimall.backend.entity.Utilisateur;
import com.malimall.backend.entity.Vendeur;
import com.malimall.backend.entity.enums.StatutPublicite;
import com.malimall.backend.exception.AccesRefuseException;
import com.malimall.backend.exception.FondsInsuffisantsException;
import com.malimall.backend.exception.TransitionStatutInvalideException;
import com.malimall.backend.repository.BoutiqueRepository;
import com.malimall.backend.repository.ClientRepository;
import com.malimall.backend.repository.UtilisateurRepository;
import com.malimall.backend.repository.VendeurRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Même style que ParcoursMarketIntegrationTest (H2 en mémoire, comptes
 * construits directement via les repositories) : vérifie le débit immédiat
 * à la demande, le crédit symétrique de la plateforme (voir la note de
 * classe de PubliciteService sur l'équilibre du journal comptable),
 * l'activation admin (statut + dateExpiration calculée), l'absence de
 * remboursement au refus, et le contrôle de propriété sur la boutique.
 *
 * Important (Spring Test) : un scénario qui déclenche volontairement une
 * exception est un @Test à part — voir la note équivalente dans
 * ParcoursMarketIntegrationTest.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PubliciteServiceIntegrationTest {

    @Autowired private UtilisateurRepository utilisateurRepository;
    @Autowired private ClientRepository clientRepository;
    @Autowired private VendeurRepository vendeurRepository;
    @Autowired private BoutiqueRepository boutiqueRepository;
    @Autowired private PortefeuilleService portefeuilleService;
    @Autowired private PubliciteService publiciteService;
    @Autowired private PasswordEncoder passwordEncoder;

    private Long vendeurUtilisateurId;
    private Long autreVendeurUtilisateurId;
    private Long vendeurPauvreUtilisateurId;
    private Long plateformeUtilisateurId;
    private Long boutiqueId;
    private Long boutiquePauvreId;

    @BeforeEach
    void seedDonnees() {
        plateformeUtilisateurId = creerUtilisateurAvecClientEtPortefeuille("MaliMall", "Plateforme", "+000000000", 0);

        vendeurUtilisateurId = creerUtilisateurAvecClientEtPortefeuille("Traoré", "Moussa", "+22370000103", 1_000);
        Vendeur vendeur = vendeurRepository.save(Vendeur.builder()
                .utilisateur(utilisateurRepository.findById(vendeurUtilisateurId).orElseThrow())
                .build());
        Boutique boutique = boutiqueRepository.save(Boutique.builder()
                .proprietaire(vendeur)
                .nom("Boutique Traoré")
                .categorie("Alimentation")
                .certifiee(false) // volontairement non certifiée — voir la note de classe de PubliciteService
                .build());
        boutiqueId = boutique.getId();

        autreVendeurUtilisateurId = creerUtilisateurAvecClientEtPortefeuille("Coulibaly", "Awa", "+22370000104", 1_000);
        vendeurRepository.save(Vendeur.builder()
                .utilisateur(utilisateurRepository.findById(autreVendeurUtilisateurId).orElseThrow())
                .build());

        // Solde volontairement trop faible pour la moindre demande de publicité (min 1 jour = 20 MMC).
        vendeurPauvreUtilisateurId = creerUtilisateurAvecClientEtPortefeuille("Diarra", "Fatoumata", "+22370000105", 10);
        Vendeur vendeurPauvre = vendeurRepository.save(Vendeur.builder()
                .utilisateur(utilisateurRepository.findById(vendeurPauvreUtilisateurId).orElseThrow())
                .build());
        Boutique boutiquePauvre = boutiqueRepository.save(Boutique.builder()
                .proprietaire(vendeurPauvre)
                .nom("Boutique Diarra")
                .categorie("Artisanat")
                .certifiee(false)
                .build());
        boutiquePauvreId = boutiquePauvre.getId();
    }

    private Long creerUtilisateurAvecClientEtPortefeuille(String nom, String prenom, String telephone, int soldeInitial) {
        Utilisateur utilisateur = utilisateurRepository.save(Utilisateur.builder()
                .nom(nom)
                .prenom(prenom)
                .telephone(telephone)
                .motDePasse(passwordEncoder.encode("password123"))
                .build());
        clientRepository.save(com.malimall.backend.entity.Client.builder().utilisateur(utilisateur).build());
        portefeuilleService.creerPour(utilisateur);
        if (soldeInitial > 0) {
            portefeuilleService.crediter(utilisateur.getId(), soldeInitial);
        }
        return utilisateur.getId();
    }

    @Test
    void demander_debiteLeVendeurEtCrediteLaPlateforme() {
        Publicite publicite = publiciteService.demander(vendeurUtilisateurId,
                new CreerPubliciteRequest(boutiqueId, "Promo artisanat", 5));

        assertThat(publicite.getStatut()).isEqualTo(StatutPublicite.EN_ATTENTE);
        assertThat(publicite.getPrixPayeMmc()).isEqualTo(100); // 20 MMC/jour * 5 jours
        assertThat(portefeuilleService.obtenirPourUtilisateur(vendeurUtilisateurId).getSoldeMmc()).isEqualTo(900);
        assertThat(portefeuilleService.obtenirPourUtilisateur(plateformeUtilisateurId).getSoldeMmc()).isEqualTo(100);
    }

    @Test
    void demander_soldeInsuffisant_leveFondsInsuffisantsEtNeDebitePersonne() {
        assertThatThrownBy(() -> publiciteService.demander(vendeurPauvreUtilisateurId,
                new CreerPubliciteRequest(boutiquePauvreId, "Trop cher", 1))) // 20 MMC requis, 10 MMC disponibles
                .isInstanceOf(FondsInsuffisantsException.class);

        // Le débit ayant échoué avant toute écriture, le solde du vendeur pauvre
        // et celui de la plateforme doivent rester inchangés.
        assertThat(portefeuilleService.obtenirPourUtilisateur(vendeurPauvreUtilisateurId).getSoldeMmc()).isEqualTo(10);
        assertThat(portefeuilleService.obtenirPourUtilisateur(plateformeUtilisateurId).getSoldeMmc()).isEqualTo(0);
    }

    @Test
    void demander_boutiqueNAppartientPasAuVendeur_leveAccesRefuse() {
        assertThatThrownBy(() -> publiciteService.demander(autreVendeurUtilisateurId,
                new CreerPubliciteRequest(boutiqueId, "Promo", 3)))
                .isInstanceOf(AccesRefuseException.class);
    }

    @Test
    void valider_activeLaPubliciteEtCalculeLaDateExpiration() {
        Publicite publicite = publiciteService.demander(vendeurUtilisateurId,
                new CreerPubliciteRequest(boutiqueId, "Promo artisanat", 7));

        Publicite activee = publiciteService.valider(publicite.getId());

        assertThat(activee.getStatut()).isEqualTo(StatutPublicite.ACTIVE);
        assertThat(activee.getDateActivation()).isNotNull();
        assertThat(activee.getDateExpiration()).isEqualTo(activee.getDateActivation().plusSeconds(7L * 24 * 3600));
        assertThat(publiciteService.actives()).extracting(Publicite::getId).contains(activee.getId());
    }

    @Test
    void refuser_neRembourseJamaisLeVendeur() {
        Publicite publicite = publiciteService.demander(vendeurUtilisateurId,
                new CreerPubliciteRequest(boutiqueId, "Promo artisanat", 5));
        int soldeApresDemande = portefeuilleService.obtenirPourUtilisateur(vendeurUtilisateurId).getSoldeMmc();

        Publicite refusee = publiciteService.refuser(publicite.getId());

        assertThat(refusee.getStatut()).isEqualTo(StatutPublicite.REFUSEE);
        assertThat(portefeuilleService.obtenirPourUtilisateur(vendeurUtilisateurId).getSoldeMmc()).isEqualTo(soldeApresDemande);
    }

    @Test
    void valider_unePubliciteDejaTraitee_leveTransitionStatutInvalide() {
        Publicite publicite = publiciteService.demander(vendeurUtilisateurId,
                new CreerPubliciteRequest(boutiqueId, "Promo artisanat", 5));
        publiciteService.refuser(publicite.getId());

        assertThatThrownBy(() -> publiciteService.valider(publicite.getId()))
                .isInstanceOf(TransitionStatutInvalideException.class);
    }
}
