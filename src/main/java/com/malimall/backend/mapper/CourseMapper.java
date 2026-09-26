package com.malimall.backend.mapper;

import com.malimall.backend.dto.CourseDtos.CourseResponse;
import com.malimall.backend.entity.Chauffeur;
import com.malimall.backend.entity.Course;
import com.malimall.backend.entity.Utilisateur;
import com.malimall.backend.entity.Vehicule;

public final class CourseMapper {

    private CourseMapper() {}

    public static CourseResponse toResponse(Course course) {
        Chauffeur chauffeur = course.getChauffeur();
        Vehicule vehicule = chauffeur != null ? chauffeur.getVehicule() : null;
        Utilisateur client = course.getClient();
        return new CourseResponse(
                course.getId(),
                course.getDepart(),
                course.getDestination(),
                course.getDistanceKm(),
                course.getPrixMmc(),
                course.getStatut(),
                client.getPrenom() + " " + client.getNom(),
                client.getTelephone(),
                chauffeur != null ? chauffeur.getId() : null,
                chauffeur != null ? chauffeur.getUtilisateur().getPrenom() + " " + chauffeur.getUtilisateur().getNom() : null,
                chauffeur != null ? chauffeur.getUtilisateur().getTelephone() : null,
                chauffeur != null ? chauffeur.getNoteMoyenne() : null,
                course.getTypeVehicule(),
                vehicule != null ? vehicule.getType().name() : null,
                vehicule != null ? vehicule.getImmatriculation() : null,
                course.getNote(),
                course.getDateDemande(),
                course.getDateAcceptation(),
                course.getDateFin(),
                course.getStatut() == com.malimall.backend.entity.enums.StatutCourse.ACCEPTEE
                        ? com.malimall.backend.dto.PositionDto.recente(chauffeur) : null,
                course.getItinerairePolyline());
    }
}
