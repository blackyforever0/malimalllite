package com.malimall.backend.service;

import com.malimall.backend.dto.LivreurDtos.EspaceLivreurResponse;
import com.malimall.backend.entity.Chauffeur;
import com.malimall.backend.entity.Vehicule;
import com.malimall.backend.entity.enums.StatutCommande;
import com.malimall.backend.entity.enums.StatutCourse;
import com.malimall.backend.entity.enums.StatutLivraison;
import com.malimall.backend.entity.enums.TypeOperation;
import com.malimall.backend.exception.RessourceIntrouvableException;
import com.malimall.backend.repository.ChauffeurRepository;
import com.malimall.backend.repository.CommandeRepository;
import com.malimall.backend.repository.CourseRepository;
import com.malimall.backend.repository.EcritureComptableRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Tableau de bord et disponibilité du livreur / chauffeur (maquette 18). */
@Service
public class EspaceLivreurService {

    private final ChauffeurRepository chauffeurRepository;
    private final CommandeRepository commandeRepository;
    private final CourseRepository courseRepository;
    private final EcritureComptableRepository ecritureComptableRepository;

    public EspaceLivreurService(ChauffeurRepository chauffeurRepository,
                                CommandeRepository commandeRepository,
                                CourseRepository courseRepository,
                                EcritureComptableRepository ecritureComptableRepository) {
        this.chauffeurRepository = chauffeurRepository;
        this.commandeRepository = commandeRepository;
        this.courseRepository = courseRepository;
        this.ecritureComptableRepository = ecritureComptableRepository;
    }

    @Transactional(readOnly = true)
    public EspaceLivreurResponse resume(Long utilisateurId) {
        return versResponse(charger(utilisateurId));
    }

    @Transactional
    public EspaceLivreurResponse changerDisponibilite(Long utilisateurId, boolean disponible) {
        Chauffeur chauffeur = charger(utilisateurId);
        chauffeur.setDisponible(disponible);
        return versResponse(chauffeurRepository.save(chauffeur));
    }

    /** Mise à jour de la position GPS du chauffeur (envoyée toutes les ~15 s par l'app). */
    @Transactional
    public void mettreAJourPosition(Long utilisateurId, double latitude, double longitude) {
        if (Math.abs(latitude) > 90 || Math.abs(longitude) > 180) {
            throw new IllegalArgumentException("Position GPS invalide");
        }
        Chauffeur chauffeur = charger(utilisateurId);
        chauffeur.setLatitude(latitude);
        chauffeur.setLongitude(longitude);
        chauffeur.setPositionMaj(java.time.Instant.now());
        chauffeurRepository.save(chauffeur);
    }

    private Chauffeur charger(Long utilisateurId) {
        return chauffeurRepository.findByUtilisateurId(utilisateurId)
                .orElseThrow(() -> new RessourceIntrouvableException("Aucun profil chauffeur pour cet utilisateur"));
    }

    private EspaceLivreurResponse versResponse(Chauffeur chauffeur) {
        Vehicule vehicule = chauffeur.getVehicule();
        long livraisonsTerminees = commandeRepository.findByChauffeurId(chauffeur.getId()).stream()
                .filter(c -> c.getStatut() == StatutCommande.LIVREE)
                .count();
        long coursesTerminees = courseRepository.findByChauffeurIdOrderByDateDemandeDesc(chauffeur.getId()).stream()
                .filter(c -> c.getStatut() == StatutCourse.TERMINEE)
                .count();
        long gains = ecritureComptableRepository
                .findByUtilisateurIdOrderByDateCreationDesc(chauffeur.getUtilisateur().getId()).stream()
                .filter(e -> e.getTypeOperation() == TypeOperation.FRAIS_LIVRAISON
                        || e.getTypeOperation() == TypeOperation.COURSE_REVENU)
                .mapToLong(e -> e.getMontantMmc())
                .sum();
        // "passagers" reste vrai uniquement pour Telimani (seul à vraiment
        // transporter des personnes) — mais Scooter et TriCycle voient eux
        // aussi leurs propres demandes de courses (tarif par palier, voir
        // QuartiersBamako), d'où le comptage par catégorie de véhicule
        // ci-dessous plutôt que conditionné à "passagers".
        boolean passagers = vehicule != null && vehicule.transportePassager();
        Integer capacite = vehicule == null || vehicule.capaciteMaxKg() == Integer.MAX_VALUE
                ? null : vehicule.capaciteMaxKg();
        long coursesEnAttentePourMoi = vehicule == null
                ? 0
                : courseRepository.countByStatutAndTypeVehicule(StatutCourse.DEMANDEE, vehicule.getType());
        return new EspaceLivreurResponse(
                chauffeur.getId(),
                chauffeur.isDisponible(),
                vehicule != null ? vehicule.getType().name() : null,
                vehicule != null ? vehicule.getImmatriculation() : null,
                capacite,
                passagers,
                chauffeur.getNoteMoyenne(),
                livraisonsTerminees + coursesTerminees,
                gains,
                commandeRepository.countByStatutLivraison(StatutLivraison.EN_ATTENTE_LIVREUR),
                coursesEnAttentePourMoi);
    }
}
