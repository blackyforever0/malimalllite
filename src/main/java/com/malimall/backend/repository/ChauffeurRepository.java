package com.malimall.backend.repository;

import com.malimall.backend.entity.Chauffeur;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChauffeurRepository extends JpaRepository<Chauffeur, Long> {
    Optional<Chauffeur> findByUtilisateurId(Long utilisateurId);
    List<Chauffeur> findByDisponibleTrue();
}
