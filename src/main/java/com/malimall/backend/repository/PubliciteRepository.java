package com.malimall.backend.repository;

import com.malimall.backend.entity.Publicite;
import com.malimall.backend.entity.enums.StatutPublicite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface PubliciteRepository extends JpaRepository<Publicite, Long> {

    // JOIN FETCH sur boutique : le mapper lit p.getBoutique().getNom() en
    // dehors de toute transaction (dans le contrôleur), donc le proxy paresseux
    // de Boutique doit être chargé ici — sinon LazyInitializationException
    // ("no Session") au moment du mapping.

    @Query("select p from Publicite p join fetch p.boutique where p.id = :id")
    Optional<Publicite> findByIdAvecBoutique(Long id);

    @Query("select p from Publicite p join fetch p.boutique where p.boutique.proprietaire.utilisateur.id = :utilisateurId "
            + "order by p.dateCreation desc")
    List<Publicite> findByBoutiqueProprietaireUtilisateurIdOrderByDateCreationDesc(Long utilisateurId);

    @Query("select p from Publicite p join fetch p.boutique where p.statut = :statut order by p.dateCreation asc")
    List<Publicite> findByStatutOrderByDateCreationAsc(StatutPublicite statut);

    @Query("select p from Publicite p join fetch p.boutique where p.statut = :statut and p.dateExpiration > :maintenant "
            + "order by p.dateCreation desc")
    List<Publicite> findByStatutAndDateExpirationAfterOrderByDateCreationDesc(StatutPublicite statut, Instant maintenant);

    /** Historique des publicités d'une boutique donnée — fiche boutique côté admin. */
    @Query("select p from Publicite p join fetch p.boutique where p.boutique.id = :boutiqueId order by p.dateCreation desc")
    List<Publicite> findByBoutiqueIdOrderByDateCreationDesc(Long boutiqueId);
}
