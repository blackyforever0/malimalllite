package com.malimall.backend.repository;

import com.malimall.backend.entity.Vendeur;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VendeurRepository extends JpaRepository<Vendeur, Long> {
    Optional<Vendeur> findByUtilisateurId(Long utilisateurId);
}
