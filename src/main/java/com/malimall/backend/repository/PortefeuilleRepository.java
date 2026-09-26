package com.malimall.backend.repository;

import com.malimall.backend.entity.Portefeuille;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface PortefeuilleRepository extends JpaRepository<Portefeuille, Long> {

    Optional<Portefeuille> findByUtilisateurId(Long utilisateurId);

    /**
     * Verrou pessimiste (SELECT ... FOR UPDATE) : à utiliser pour toute
     * opération qui gèle/débloque/crédite/débite un portefeuille, afin
     * d'éviter une race entre deux règlements concurrents sur le même compte.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Portefeuille p where p.utilisateur.id = :utilisateurId")
    Optional<Portefeuille> findByUtilisateurIdForUpdate(Long utilisateurId);
}
