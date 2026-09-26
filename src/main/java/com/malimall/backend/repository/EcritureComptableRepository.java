package com.malimall.backend.repository;

import com.malimall.backend.entity.EcritureComptable;
import com.malimall.backend.entity.enums.TypeOperation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EcritureComptableRepository extends JpaRepository<EcritureComptable, Long> {
    List<EcritureComptable> findByUtilisateurIdOrderByDateCreationDesc(Long utilisateurId);
    List<EcritureComptable> findByReferenceTypeAndReferenceId(String referenceType, Long referenceId);

    /** Console admin — Revenus : chaque écriture de commission, la plus récente d'abord. */
    List<EcritureComptable> findByTypeOperationOrderByDateCreationDesc(TypeOperation typeOperation);
}
