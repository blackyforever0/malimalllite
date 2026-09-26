package com.malimall.backend.service;

import com.malimall.backend.config.MaliMallProperties;
import com.malimall.backend.dto.CourseDtos.ChauffeurDisponibleResponse;
import com.malimall.backend.dto.CourseDtos.CourseResponse;
import com.malimall.backend.dto.CourseDtos.EstimationResponse;
import com.malimall.backend.entity.Chauffeur;
import com.malimall.backend.entity.Course;
import com.malimall.backend.entity.EcritureComptable;
import com.malimall.backend.entity.Utilisateur;
import com.malimall.backend.entity.Vehicule;
import com.malimall.backend.entity.enums.StatutCourse;
import com.malimall.backend.entity.enums.TypeOperation;
import com.malimall.backend.entity.enums.TypeVehicule;
import com.malimall.backend.exception.AccesRefuseException;
import com.malimall.backend.exception.RessourceIntrouvableException;
import com.malimall.backend.exception.TransitionStatutInvalideException;
import com.malimall.backend.mapper.CourseMapper;
import com.malimall.backend.repository.ChauffeurRepository;
import com.malimall.backend.repository.CourseRepository;
import com.malimall.backend.repository.EcritureComptableRepository;
import com.malimall.backend.repository.UtilisateurRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Courses MotoTaxi. Même principe de paiement que le Market : le prix est
 * gelé sur le portefeuille du client à la demande, puis réglé à la fin de
 * la course (chauffeur crédité, commission plateforme prélevée), ou libéré
 * si le client annule avant qu'un chauffeur n'accepte.
 *
 * Seuls les véhicules qui transportent des passagers (Telimani) peuvent
 * accepter une course — voir Vehicule.transportePassager().
 */
@Service
public class CourseService {

    private final CourseRepository courseRepository;
    private final ChauffeurRepository chauffeurRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final EcritureComptableRepository ecritureComptableRepository;
    private final PortefeuilleService portefeuilleService;
    private final QuartiersBamako quartiers;
    private final DirectionsService directionsService;
    private final MaliMallProperties properties;

    public CourseService(CourseRepository courseRepository,
                         ChauffeurRepository chauffeurRepository,
                         UtilisateurRepository utilisateurRepository,
                         EcritureComptableRepository ecritureComptableRepository,
                         PortefeuilleService portefeuilleService,
                         QuartiersBamako quartiers,
                         DirectionsService directionsService,
                         MaliMallProperties properties) {
        this.courseRepository = courseRepository;
        this.chauffeurRepository = chauffeurRepository;
        this.utilisateurRepository = utilisateurRepository;
        this.ecritureComptableRepository = ecritureComptableRepository;
        this.portefeuilleService = portefeuilleService;
        this.quartiers = quartiers;
        this.directionsService = directionsService;
        this.properties = properties;
    }

    public List<String> quartiers() {
        return quartiers.noms();
    }

    /**
     * Distance et tracé : si Directions API est configurée (voir
     * DirectionsService) et répond, on utilise sa distance routière réelle
     * (plus précise que l'estimation à vol d'oiseau) pour placer la course
     * dans le bon palier de prix, et son polyline pour un tracé de carte
     * fidèle aux rues plutôt qu'une ligne droite. Sinon, repli complet sur
     * QuartiersBamako (aucune API externe requise pour que l'app fonctionne).
     */
    public EstimationResponse estimer(String depart, String destination, TypeVehicule typeVehicule) {
        verifierTrajet(depart, destination);
        QuartiersBamako.Coordonnee depCoord = quartiers.coordonnee(depart);
        QuartiersBamako.Coordonnee destCoord = quartiers.coordonnee(destination);
        var itineraire = directionsService.itineraire(depCoord.lat(), depCoord.lon(), destCoord.lat(), destCoord.lon());

        BigDecimal distance = itineraire.map(DirectionsService.Itineraire::distanceKm)
                .orElseGet(() -> quartiers.distanceKm(depart, destination));
        String polyline = itineraire.map(DirectionsService.Itineraire::polylineEncodee).orElse(null);

        return new EstimationResponse(depart, destination, distance, quartiers.prixMmc(distance, typeVehicule), typeVehicule, polyline);
    }

    /**
     * Chauffeurs disponibles, toutes catégories de véhicule confondues — le
     * choix de la catégorie (et donc du tarif) se fait à la demande, pas ici.
     */
    @Transactional(readOnly = true)
    public List<ChauffeurDisponibleResponse> chauffeursDisponibles() {
        return chauffeurRepository.findByDisponibleTrue().stream()
                .filter(c -> c.getVehicule() != null)
                .map(c -> new ChauffeurDisponibleResponse(
                        c.getId(),
                        c.getUtilisateur().getPrenom() + " " + c.getUtilisateur().getNom(),
                        c.getVehicule().getType().name(),
                        c.getNoteMoyenne(),
                        com.malimall.backend.dto.PositionDto.recente(c)))
                .toList();
    }

    @Transactional
    public CourseResponse demander(Long clientUtilisateurId, String depart, String destination, TypeVehicule typeVehicule) {
        EstimationResponse estimation = estimer(depart, destination, typeVehicule);
        Utilisateur client = utilisateurRepository.findById(clientUtilisateurId)
                .orElseThrow(() -> new RessourceIntrouvableException("Utilisateur introuvable"));
        boolean dejaEnCours = courseRepository.findByClientIdOrderByDateDemandeDesc(clientUtilisateurId).stream()
                .anyMatch(c -> c.getStatut() == StatutCourse.DEMANDEE || c.getStatut() == StatutCourse.ACCEPTEE);
        if (dejaEnCours) {
            throw new TransitionStatutInvalideException("Vous avez déjà une course en cours");
        }
        portefeuilleService.geler(clientUtilisateurId, estimation.prixMmc());
        Course course = courseRepository.save(Course.builder()
                .client(client)
                .depart(depart)
                .destination(destination)
                .distanceKm(estimation.distanceKm())
                .prixMmc(estimation.prixMmc())
                .typeVehicule(typeVehicule)
                .itinerairePolyline(estimation.itineraire())
                .build());
        return CourseMapper.toResponse(course);
    }

    @Transactional(readOnly = true)
    public CourseResponse obtenir(Long courseId, Long viewerUtilisateurId) {
        Course course = charger(courseId);
        boolean estClient = course.getClient().getId().equals(viewerUtilisateurId);
        boolean estChauffeur = course.getChauffeur() != null
                && course.getChauffeur().getUtilisateur().getId().equals(viewerUtilisateurId);
        boolean chauffeurQuiParcourt = course.getStatut() == StatutCourse.DEMANDEE
                && chauffeurRepository.findByUtilisateurId(viewerUtilisateurId).isPresent();
        if (!estClient && !estChauffeur && !chauffeurQuiParcourt) {
            throw new AccesRefuseException("Vous n'avez pas accès à cette course");
        }
        return CourseMapper.toResponse(course);
    }

    @Transactional(readOnly = true)
    public List<CourseResponse> mesCoursesClient(Long clientUtilisateurId) {
        return courseRepository.findByClientIdOrderByDateDemandeDesc(clientUtilisateurId).stream()
                .map(CourseMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CourseResponse> mesCoursesChauffeur(Long chauffeurUtilisateurId) {
        Chauffeur chauffeur = chauffeurDe(chauffeurUtilisateurId);
        return courseRepository.findByChauffeurIdOrderByDateDemandeDesc(chauffeur.getId()).stream()
                .map(CourseMapper::toResponse)
                .toList();
    }

    /** Courses en attente d'un chauffeur — visibles seulement dans sa propre catégorie de véhicule. */
    @Transactional(readOnly = true)
    public List<CourseResponse> disponibles(Long chauffeurUtilisateurId) {
        Chauffeur chauffeur = chauffeurDe(chauffeurUtilisateurId);
        if (chauffeur.getVehicule() == null) {
            return List.of();
        }
        TypeVehicule monType = chauffeur.getVehicule().getType();
        return courseRepository.findByStatutOrderByDateDemandeAsc(StatutCourse.DEMANDEE).stream()
                .filter(c -> c.getTypeVehicule() == monType)
                .map(CourseMapper::toResponse)
                .toList();
    }

    @Transactional
    public CourseResponse accepter(Long courseId, Long chauffeurUtilisateurId) {
        Course course = charger(courseId);
        Chauffeur chauffeur = chauffeurDe(chauffeurUtilisateurId);
        Vehicule vehicule = chauffeur.getVehicule();
        if (vehicule == null || vehicule.getType() != course.getTypeVehicule()) {
            throw new TransitionStatutInvalideException(
                    "Cette course est réservée aux chauffeurs " + course.getTypeVehicule().name());
        }
        if (!chauffeur.isDisponible()) {
            throw new TransitionStatutInvalideException("Passez en ligne pour accepter des courses");
        }
        if (course.getClient().getId().equals(chauffeurUtilisateurId)) {
            throw new TransitionStatutInvalideException("Vous ne pouvez pas accepter votre propre course");
        }
        if (course.getStatut() != StatutCourse.DEMANDEE) {
            throw new TransitionStatutInvalideException("Cette course n'est plus disponible");
        }
        course.setChauffeur(chauffeur);
        course.setStatut(StatutCourse.ACCEPTEE);
        course.setDateAcceptation(Instant.now());
        return CourseMapper.toResponse(courseRepository.save(course));
    }

    /** Fin de course : débit du client, crédit du chauffeur, commission plateforme. */
    @Transactional
    public CourseResponse terminer(Long courseId, Long chauffeurUtilisateurId) {
        Course course = charger(courseId);
        if (course.getChauffeur() == null
                || !course.getChauffeur().getUtilisateur().getId().equals(chauffeurUtilisateurId)) {
            throw new AccesRefuseException("Cette course ne vous est pas attribuée");
        }
        if (course.getStatut() != StatutCourse.ACCEPTEE) {
            throw new TransitionStatutInvalideException("Cette course n'est pas en cours");
        }
        int prix = course.getPrixMmc();
        int commission = Math.round(prix * (float) properties.commission().taux());
        int revenuChauffeur = prix - commission;
        Long clientId = course.getClient().getId();
        Long plateformeId = plateformeUtilisateurId();

        portefeuilleService.debiter(clientId, prix);
        portefeuilleService.crediter(chauffeurUtilisateurId, revenuChauffeur);
        portefeuilleService.crediter(plateformeId, commission);

        ecrire(clientId, TypeOperation.COURSE_PAIEMENT, prix, course.getId());
        ecrire(chauffeurUtilisateurId, TypeOperation.COURSE_REVENU, revenuChauffeur, course.getId());
        ecrire(plateformeId, TypeOperation.COMMISSION, commission, course.getId());

        course.setStatut(StatutCourse.TERMINEE);
        course.setDateFin(Instant.now());
        return CourseMapper.toResponse(courseRepository.save(course));
    }

    /** Le client annule tant qu'aucun chauffeur n'a accepté : ses fonds sont libérés. */
    @Transactional
    public CourseResponse annuler(Long courseId, Long clientUtilisateurId) {
        Course course = charger(courseId);
        if (!course.getClient().getId().equals(clientUtilisateurId)) {
            throw new AccesRefuseException("Cette course ne vous appartient pas");
        }
        if (course.getStatut() != StatutCourse.DEMANDEE) {
            throw new TransitionStatutInvalideException("Un chauffeur a déjà accepté cette course");
        }
        portefeuilleService.debloquer(clientUtilisateurId, course.getPrixMmc());
        course.setStatut(StatutCourse.ANNULEE);
        course.setDateFin(Instant.now());
        return CourseMapper.toResponse(courseRepository.save(course));
    }

    @Transactional
    public CourseResponse noter(Long courseId, Long clientUtilisateurId, int note, String commentaire) {
        Course course = charger(courseId);
        if (!course.getClient().getId().equals(clientUtilisateurId)) {
            throw new AccesRefuseException("Cette course ne vous appartient pas");
        }
        if (course.getStatut() != StatutCourse.TERMINEE) {
            throw new TransitionStatutInvalideException("Une course ne peut être notée qu'une fois terminée");
        }
        if (course.getNote() != null) {
            throw new TransitionStatutInvalideException("Cette course a déjà été notée");
        }
        course.setNote(note);
        course.setCommentaire(commentaire);
        course.getChauffeur().ajouterNote(note);
        return CourseMapper.toResponse(courseRepository.save(course));
    }

    private void verifierTrajet(String depart, String destination) {
        if (!quartiers.existe(depart) || !quartiers.existe(destination)) {
            throw new IllegalArgumentException("Quartier de départ ou de destination inconnu");
        }
        if (depart.equals(destination)) {
            throw new IllegalArgumentException("Le départ et la destination doivent être différents");
        }
    }

    private Course charger(Long courseId) {
        return courseRepository.findById(courseId)
                .orElseThrow(() -> new RessourceIntrouvableException("Course introuvable : " + courseId));
    }

    private Chauffeur chauffeurDe(Long utilisateurId) {
        return chauffeurRepository.findByUtilisateurId(utilisateurId)
                .orElseThrow(() -> new RessourceIntrouvableException("Aucun profil chauffeur pour cet utilisateur"));
    }

    private void ecrire(Long utilisateurId, TypeOperation type, int montant, Long courseId) {
        Utilisateur utilisateur = utilisateurRepository.findById(utilisateurId)
                .orElseThrow(() -> new RessourceIntrouvableException("Utilisateur introuvable : " + utilisateurId));
        ecritureComptableRepository.save(EcritureComptable.builder()
                .utilisateur(utilisateur)
                .typeOperation(type)
                .montantMmc(montant)
                .referenceType("COURSE")
                .referenceId(courseId)
                .build());
    }

    private Long plateformeUtilisateurId() {
        return utilisateurRepository.findByTelephone(properties.plateforme().telephone())
                .orElseThrow(() -> new IllegalStateException("Utilisateur plateforme introuvable"))
                .getId();
    }
}
