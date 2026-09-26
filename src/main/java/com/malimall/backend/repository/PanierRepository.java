package com.malimall.backend.repository;

import com.malimall.backend.entity.Panier;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PanierRepository extends JpaRepository<Panier, Long> {
    Optional<Panier> findByClientId(Long clientId);
}
