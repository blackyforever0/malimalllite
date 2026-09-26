package com.malimall.backend.repository;

import com.malimall.backend.entity.Client;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ClientRepository extends JpaRepository<Client, Long> {
    Optional<Client> findByUtilisateurId(Long utilisateurId);
}
