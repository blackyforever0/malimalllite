package com.malimall.backend.service;

import com.malimall.backend.config.MaliMallProperties;
import com.malimall.backend.dto.PubliciteDtos.CreerPubliciteRequest;
import com.malimall.backend.entity.Boutique;
import com.malimall.backend.entity.EcritureComptable;
import com.malimall.backend.entity.Publicite;
import com.malimall.backend.entity.Utilisateur;
import com.malimall.backend.entity.Vendeur;
import com.malimall.backend.entity.enums.StatutPublicite;
import com.malimall.backend.entity.enums.TypeOperation;
import com.malimall.backend.exception.AccesRefuseException;
import com.malimall.backend.exception.RessourceIntrouvableException;
import com.malimall.backend.exception.TransitionStatutInvalideException;
import com.malimall.backend.repository.BoutiqueRepository;
import com.malimall.backend.repository.EcritureComptableRepository;
import com.malimall.backend.repository.PubliciteRepository;
import com.malimall.backend.repository.UtilisateurRepository;
import com.malimall.backend.repository.VendeurRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Diffusion payante d'une bannière sur l'accueil Market : le vendeur paie
 * au moment de la demande (débit immédiat, pas de gel), la diffusion n'est
 * activée qu'après validation manuelle par un admin.
 *
 * Règles de ce module :
 *  - une seule zone de diffusion : l'accueil Market ;
 *  - la boutique n'a pas besoin d'être "certifiee" (ce champ existe mais
 *    aucun flux admin ne le fait jamais passer à true dans ce lot — l'exiger
 *    rendrait la fonctionnalité totalement inutilisable en démo) ;
 *  - le montant débité est crédité au
 *    portefeuille plateforme (voir demander()) plutôt que de simplement
 *    disparaître du solde MMC total en circulation — plus correct pour un
 *    ledger qui doit s'équilibrer.
 */
@Service
public class PubliciteService {

    /** Tarif journalier d'une bannière — jamais calculé côté client. */
    public static final int PRIX_PAR_JOUR_MMC = 20;

    private final PubliciteRepository publiciteRepository;
    private final BoutiqueRepository boutiqueRepository;
    private final VendeurRepository vendeurRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final EcritureComptableRepository ecritureComptableRepository;
    private final PortefeuilleService portefeuilleService;
    private final SupabaseStorageService supabaseStorageService;
    private final MaliMallProperties properties;

    public PubliciteService(PubliciteRepository publiciteRepository,
                             BoutiqueRepository boutiqueRepository,
                             VendeurRepository vendeurRepository,
                             UtilisateurRepository utilisateurRepository,
                             EcritureComptableRepository ecritureComptableRepository,
                             PortefeuilleService portefeuilleService,
                             SupabaseStorageService supabaseStorageService,
                             MaliMallProperties properties) {
        this.publiciteRepository = publiciteRepository;
        this.boutiqueRepository = boutiqueRepository;
        this.vendeurRepository = vendeurRepository;
        this.utilisateurRepository = utilisateurRepository;
        this.ecritureComptableRepository = ecritureComptableRepository;
        this.portefeuilleService = portefeuilleService;
        this.supabaseStorageService = supabaseStorageService;
        this.properties = properties;
    }

    public Publicite obtenir(Long id) {
        // findByIdAvecBoutique (JOIN FETCH boutique) plutôt que findById : le
        // mapper accède à boutique.getNom() en dehors de la transaction, dans
        // le contrôleur — un simple findById renvoie un proxy paresseux qui
        // provoque un LazyInitializationException ("no Session") à ce moment-là.
        return publiciteRepository.findByIdAvecBoutique(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Publicité introuvable : " + id));
    }

    public List<Publicite> mesPublicites(Long utilisateurId) {
        return publiciteRepository.findByBoutiqueProprietaireUtilisateurIdOrderByDateCreationDesc(utilisateurId);
    }

    /** Bannières à afficher sur l'accueil Market — ACTIVE et pas encore expirée. */
    public List<Publicite> actives() {
        return publiciteRepository.findByStatutAndDateExpirationAfterOrderByDateCreationDesc(
                StatutPublicite.ACTIVE, Instant.now());
    }

    public List<Publicite> enAttente() {
        return publiciteRepository.findByStatutOrderByDateCreationAsc(StatutPublicite.EN_ATTENTE);
    }

    /** Historique des publicités d'une boutique donnée — fiche boutique côté admin. */
    public List<Publicite> parBoutique(Long boutiqueId) {
        return publiciteRepository.findByBoutiqueIdOrderByDateCreationDesc(boutiqueId);
    }

    @Transactional
    public Publicite demander(Long utilisateurId, CreerPubliciteRequest req) {
        Boutique boutique = boutiqueRepository.findById(req.boutiqueId())
                .orElseThrow(() -> new RessourceIntrouvableException("Boutique introuvable : " + req.boutiqueId()));
        Vendeur vendeur = vendeurRepository.findByUtilisateurId(utilisateurId)
                .orElseThrow(() -> new IllegalStateException("Il faut être vendeur pour créer une publicité"));
        if (!boutique.getProprietaire().getId().equals(vendeur.getId())) {
            throw new AccesRefuseException("Cette boutique ne vous appartient pas");
        }

        int prix = PRIX_PAR_JOUR_MMC * req.dureeJours();
        Long plateformeId = plateformeUtilisateurId();

        // Débit direct (pas de gel préalable, contrairement au parcours
        // Market) : le vendeur paie au moment de la demande, avant même la
        // validation admin — voir la note de classe ci-dessus.
        portefeuilleService.debiterDirect(utilisateurId, prix);
        portefeuilleService.crediter(plateformeId, prix);

        Publicite publicite = publiciteRepository.save(Publicite.builder()
                .boutique(boutique)
                .titre(req.titre())
                .dureeJours(req.dureeJours())
                .prixPayeMmc(prix)
                .statut(StatutPublicite.EN_ATTENTE)
                .build());

        ecrire(utilisateurId, TypeOperation.PUBLICITE, prix, publicite.getId());
        ecrire(plateformeId, TypeOperation.COMMISSION, prix, publicite.getId());

        return publicite;
    }

    @Transactional
    public Publicite televerserPhoto(Long utilisateurId, Long publiciteId, MultipartFile fichier) {
        Publicite publicite = obtenir(publiciteId);
        verifierProprietaire(publicite, utilisateurId);
        String url = supabaseStorageService.televerser("publicites", publicite.getId(), fichier);
        publicite.setImageUrl(url);
        return publiciteRepository.save(publicite);
    }

    @Transactional
    public Publicite valider(Long publiciteId) {
        Publicite publicite = obtenir(publiciteId);
        if (publicite.getStatut() != StatutPublicite.EN_ATTENTE) {
            throw new TransitionStatutInvalideException("Cette publicité n'est plus en attente de validation");
        }
        Instant maintenant = Instant.now();
        publicite.setStatut(StatutPublicite.ACTIVE);
        publicite.setDateActivation(maintenant);
        publicite.setDateExpiration(maintenant.plus(publicite.getDureeJours(), ChronoUnit.DAYS));
        return publiciteRepository.save(publicite);
    }

    /**
     * Pas de remboursement au refus — même comportement que le vrai
     * MaliMall (requestAd débite immédiatement, aucune fonction de
     * remboursement n'existe côté serveur pour un "ad" rejeté).
     */
    @Transactional
    public Publicite refuser(Long publiciteId) {
        Publicite publicite = obtenir(publiciteId);
        if (publicite.getStatut() != StatutPublicite.EN_ATTENTE) {
            throw new TransitionStatutInvalideException("Cette publicité n'est plus en attente de validation");
        }
        publicite.setStatut(StatutPublicite.REFUSEE);
        return publiciteRepository.save(publicite);
    }

    private void verifierProprietaire(Publicite publicite, Long utilisateurId) {
        Vendeur vendeur = vendeurRepository.findByUtilisateurId(utilisateurId)
                .orElseThrow(() -> new IllegalStateException("Il faut être vendeur"));
        if (!publicite.getBoutique().getProprietaire().getId().equals(vendeur.getId())) {
            throw new AccesRefuseException("Cette publicité ne vous appartient pas");
        }
    }

    private void ecrire(Long utilisateurId, TypeOperation type, int montant, Long publiciteId) {
        Utilisateur utilisateur = utilisateurRepository.findById(utilisateurId)
                .orElseThrow(() -> new RessourceIntrouvableException("Utilisateur introuvable : " + utilisateurId));
        ecritureComptableRepository.save(EcritureComptable.builder()
                .utilisateur(utilisateur)
                .typeOperation(type)
                .montantMmc(montant)
                .referenceType("PUBLICITE")
                .referenceId(publiciteId)
                .build());
    }

    private Long plateformeUtilisateurId() {
        return utilisateurRepository.findByTelephone(properties.plateforme().telephone())
                .orElseThrow(() -> new IllegalStateException(
                        "Utilisateur plateforme introuvable (téléphone " + properties.plateforme().telephone()
                                + ") — vérifier V2__seed_demo_data.sql"))
                .getId();
    }
}
