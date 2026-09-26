package com.malimall.backend.repository;

import com.malimall.backend.entity.Boutique;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BoutiqueRepository extends JpaRepository<Boutique, Long> {
    List<Boutique> findByProprietaireId(Long proprietaireId);
}
