package com.malimall.backend.repository;

import com.malimall.backend.entity.Course;
import com.malimall.backend.entity.enums.StatutCourse;
import com.malimall.backend.entity.enums.TypeVehicule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CourseRepository extends JpaRepository<Course, Long> {
    List<Course> findByClientIdOrderByDateDemandeDesc(Long clientId);
    List<Course> findByChauffeurIdOrderByDateDemandeDesc(Long chauffeurId);
    List<Course> findByStatutOrderByDateDemandeAsc(StatutCourse statut);
    long countByStatut(StatutCourse statut);
    /** Demandes en attente dans une catégorie de véhicule donnée (badge du tableau de bord chauffeur). */
    long countByStatutAndTypeVehicule(StatutCourse statut, TypeVehicule typeVehicule);
}
