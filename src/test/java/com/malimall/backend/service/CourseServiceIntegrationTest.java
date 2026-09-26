package com.malimall.backend.service;

import com.malimall.backend.dto.CourseDtos.CourseResponse;
import com.malimall.backend.dto.CourseDtos.EstimationResponse;
import com.malimall.backend.entity.Chauffeur;
import com.malimall.backend.entity.Client;
import com.malimall.backend.entity.Utilisateur;
import com.malimall.backend.entity.Vehicule;
import com.malimall.backend.entity.enums.StatutCourse;
import com.malimall.backend.entity.enums.TypeVehicule;
import com.malimall.backend.exception.TransitionStatutInvalideException;
import com.malimall.backend.repository.ChauffeurRepository;
import com.malimall.backend.repository.ClientRepository;
import com.malimall.backend.repository.UtilisateurRepository;
import com.malimall.backend.repository.VehiculeRepository;
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
 * Parcours MotoTaxi complet sur H2 : estimation du prix, demande (fonds
 * gelés), acceptation par un chauffeur Telimani, fin de course (règlement),
 * notation — et les refus attendus (véhicule sans passagers, annulation).
 * Même convention que ParcoursMarketIntegrationTest : un scénario qui lève
 * une exception attendue est un @Test à part.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CourseServiceIntegrationTest {

    @Autowired private UtilisateurRepository utilisateurRepository;
    @Autowired private ClientRepository clientRepository;
    @Autowired private ChauffeurRepository chauffeurRepository;
    @Autowired private VehiculeRepository vehiculeRepository;
    @Autowired private PortefeuilleService portefeuilleService;
    @Autowired private CourseService courseService;
    @Autowired private EspaceLivreurService espaceLivreurService;
    @Autowired private PasswordEncoder passwordEncoder;

    private Long clientId;
    private Long chauffeurTelimaniId;
    private Long chauffeurScooterId;
    private Long plateformeId;

    @BeforeEach
    void seed() {
        plateformeId = creerUtilisateur("MaliMall", "Plateforme", "+000000000", 0);
        clientId = creerUtilisateur("Diarra", "Ibrahim", "+22370000010", 5_000);
        chauffeurTelimaniId = creerChauffeur("Konaté", "Moussa", "+22370000011", TypeVehicule.TELIMANI);
        chauffeurScooterId = creerChauffeur("Traoré", "Awa", "+22370000012", TypeVehicule.SCOOTER);
    }

    private Long creerUtilisateur(String nom, String prenom, String telephone, int solde) {
        Utilisateur u = utilisateurRepository.save(Utilisateur.builder()
                .nom(nom).prenom(prenom).telephone(telephone)
                .motDePasse(passwordEncoder.encode("password123"))
                .build());
        clientRepository.save(Client.builder().utilisateur(u).build());
        portefeuilleService.creerPour(u);
        if (solde > 0) {
            portefeuilleService.crediter(u.getId(), solde);
        }
        return u.getId();
    }

    private Long creerChauffeur(String nom, String prenom, String telephone, TypeVehicule type) {
        Long id = creerUtilisateur(nom, prenom, telephone, 0);
        Chauffeur chauffeur = chauffeurRepository.save(Chauffeur.builder()
                .utilisateur(utilisateurRepository.findById(id).orElseThrow())
                .disponible(true)
                .build());
        Vehicule vehicule = vehiculeRepository.save(Vehicule.builder()
                .chauffeur(chauffeur).type(type).immatriculation("KT-" + telephone.substring(9))
                .build());
        chauffeur.setVehicule(vehicule);
        return id;
    }

    @Test
    void estimation_calculeUneDistanceEtUnPrixParPalier() {
        EstimationResponse e = courseService.estimer("Hippodrome", "ACI 2000", TypeVehicule.TELIMANI);
        assertThat(e.distanceKm().doubleValue()).isGreaterThan(1.0);
        assertThat(e.prixMmc() % 50).isZero();
        assertThat(e.prixMmc()).isGreaterThanOrEqualTo(200);
    }

    @Test
    void parcoursComplet_demandeAcceptationFinEtNotation() {
        CourseResponse demandee = courseService.demander(clientId, "Hippodrome", "ACI 2000", TypeVehicule.TELIMANI);
        int prix = demandee.prixMmc();
        assertThat(demandee.statut()).isEqualTo(StatutCourse.DEMANDEE);
        assertThat(portefeuilleService.obtenirPourUtilisateur(clientId).getSoldeGeleMmc()).isEqualTo(prix);

        // Le chauffeur Telimani la voit et l'accepte ; le chauffeur Scooter ne la voit pas.
        assertThat(courseService.disponibles(chauffeurTelimaniId)).extracting(CourseResponse::id).contains(demandee.id());
        assertThat(courseService.disponibles(chauffeurScooterId)).isEmpty();
        CourseResponse acceptee = courseService.accepter(demandee.id(), chauffeurTelimaniId);
        assertThat(acceptee.statut()).isEqualTo(StatutCourse.ACCEPTEE);
        assertThat(acceptee.chauffeurNom()).isEqualTo("Moussa Konaté");

        CourseResponse terminee = courseService.terminer(demandee.id(), chauffeurTelimaniId);
        assertThat(terminee.statut()).isEqualTo(StatutCourse.TERMINEE);

        int commission = Math.round(prix * 0.08f);
        assertThat(portefeuilleService.obtenirPourUtilisateur(clientId).getSoldeMmc()).isEqualTo(5_000 - prix);
        assertThat(portefeuilleService.obtenirPourUtilisateur(clientId).getSoldeGeleMmc()).isZero();
        assertThat(portefeuilleService.obtenirPourUtilisateur(chauffeurTelimaniId).getSoldeMmc()).isEqualTo(prix - commission);
        assertThat(portefeuilleService.obtenirPourUtilisateur(plateformeId).getSoldeMmc()).isEqualTo(commission);

        CourseResponse notee = courseService.noter(demandee.id(), clientId, 4, "Très bien");
        assertThat(notee.note()).isEqualTo(4);
        assertThat(courseService.mesCoursesClient(clientId)).hasSize(1);
        assertThat(courseService.mesCoursesChauffeur(chauffeurTelimaniId)).hasSize(1);
    }

    @Test
    void annulation_libereLesFondsGeles() {
        CourseResponse demandee = courseService.demander(clientId, "Badalabougou", "Centre-ville", TypeVehicule.TELIMANI);
        CourseResponse annulee = courseService.annuler(demandee.id(), clientId);

        assertThat(annulee.statut()).isEqualTo(StatutCourse.ANNULEE);
        assertThat(portefeuilleService.obtenirPourUtilisateur(clientId).getSoldeGeleMmc()).isZero();
        assertThat(portefeuilleService.obtenirPourUtilisateur(clientId).getSoldeMmc()).isEqualTo(5_000);
    }

    @Test
    void accepter_refuseUnChauffeurDuneAutreCategorieDeVehicule() {
        CourseResponse demandee = courseService.demander(clientId, "Hippodrome", "ACI 2000", TypeVehicule.TELIMANI);

        assertThatThrownBy(() -> courseService.accepter(demandee.id(), chauffeurScooterId))
                .isInstanceOf(TransitionStatutInvalideException.class)
                .hasMessageContaining("TELIMANI");
    }

    @Test
    void positionGps_partageeAvecLeClientPendantLaCourse() {
        espaceLivreurService.mettreAJourPosition(chauffeurTelimaniId, 12.6392, -8.0029);
        assertThat(courseService.chauffeursDisponibles())
                .filteredOn(c -> c.nom().equals("Moussa Konaté"))
                .singleElement()
                .satisfies(c -> assertThat(c.position()).isNotNull());

        CourseResponse demandee = courseService.demander(clientId, "Hippodrome", "ACI 2000", TypeVehicule.TELIMANI);
        assertThat(demandee.chauffeurPosition()).isNull();
        courseService.accepter(demandee.id(), chauffeurTelimaniId);

        CourseResponse vueClient = courseService.obtenir(demandee.id(), clientId);
        assertThat(vueClient.chauffeurPosition()).isNotNull();
        assertThat(vueClient.chauffeurPosition().latitude()).isEqualTo(12.6392);
    }

    @Test
    void positionGps_refuseDesCoordonneesImpossibles() {
        assertThatThrownBy(() -> espaceLivreurService.mettreAJourPosition(chauffeurTelimaniId, 120, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
