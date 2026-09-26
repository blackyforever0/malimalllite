package com.malimall.backend.repository;

import com.malimall.backend.entity.Signalement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SignalementRepository extends JpaRepository<Signalement, Long> {
    List<Signalement> findByCommandeId(Long commandeId);
    List<Signalement> findByStatutOrderByDateCreationAsc(String statut);
}
