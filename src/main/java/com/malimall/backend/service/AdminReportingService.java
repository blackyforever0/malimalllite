package com.malimall.backend.service;

import com.malimall.backend.config.MaliMallProperties;
import com.malimall.backend.dto.AdminDtos.ActionLitige;
import com.malimall.backend.dto.AdminDtos.AdminBoutiqueDetailResponse;
import com.malimall.backend.dto.AdminDtos.AdminBoutiqueResponse;
import com.malimall.backend.dto.AdminDtos.AdminProduitResponse;
import com.malimall.backend.dto.AdminDtos.AdminChauffeurDetailResponse;
import com.malimall.backend.dto.AdminDtos.AdminChauffeurResponse;
import com.malimall.backend.dto.AdminDtos.AdminCommandeDetailResponse;
import com.malimall.backend.dto.AdminDtos.AdminCommandeResponse;
import com.malimall.backend.dto.AdminDtos.CommandeApercuResponse;
import com.malimall.backend.dto.AdminDtos.CourseApercuResponse;
import com.malimall.backend.dto.AdminDtos.EtapeLivraison;
import com.malimall.backend.dto.AdminDtos.MotoTaxisResponse;
import com.malimall.backend.dto.AdminDtos.PointJournalier;
import com.malimall.backend.dto.AdminDtos.RevenusResponse;
import com.malimall.backend.dto.AdminDtos.SignalementResponse;
import com.malimall.backend.dto.AdminDtos.TopContributeur;
import com.malimall.backend.dto.AdminDtos.TransactionCommission;
import com.malimall.backend.dto.AdminDtos.AdminUtilisateurResponse;
import com.malimall.backend.dto.AdminDtos.VueEnsembleResponse;
import com.malimall.backend.dto.PubliciteDtos.PubliciteResponse;
import com.malimall.backend.mapper.PubliciteMapper;
import com.malimall.backend.entity.Boutique;
import com.malimall.backend.entity.Chauffeur;
import com.malimall.backend.entity.Produit;
import com.malimall.backend.entity.Commande;
import com.malimall.backend.entity.Course;
import com.malimall.backend.entity.EcritureComptable;
import com.malimall.backend.entity.Utilisateur;
import com.malimall.backend.entity.Vehicule;
import com.malimall.backend.entity.enums.StatutCommande;
import com.malimall.backend.entity.enums.StatutLivraison;
import com.malimall.backend.entity.enums.TypeOperation;
import com.malimall.backend.entity.enums.TypeVehicule;
import com.malimall.backend.exception.RessourceIntrouvableException;
import com.malimall.backend.exception.TransitionStatutInvalideException;
import com.malimall.backend.repository.BoutiqueRepository;
import com.malimall.backend.repository.ChauffeurRepository;
import com.malimall.backend.repository.ClientRepository;
import com.malimall.backend.repository.CommandeRepository;
import com.malimall.backend.repository.CourseRepository;
import com.malimall.backend.repository.EcritureComptableRepository;
import com.malimall.backend.repository.PortefeuilleRepository;
import com.malimall.backend.repository.ProduitRepository;
import com.malimall.backend.repository.SignalementRepository;
import com.malimall.backend.repository.UtilisateurRepository;
import com.malimall.backend.repository.VendeurRepository;
import com.malimall.backend.service.CommissionCalculator.CommissionSplit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Agrégations et actions de la console d'administration (web, Angular),
 * /api/admin/**. Toujours en lecture à partir des mêmes tables que le reste
 * de l'application — aucune table dédiée à l'admin, sauf les deux drapeaux
 * de suspension (V7__admin_flags.sql). Les seules mutations d'ici sont :
 * suspendre/réactiver une boutique ou un chauffeur, et résoudre un litige
 * (voir {@link #resoudreLitige}).
 */
@Service
public class AdminReportingService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final ZoneOffset ZONE = ZoneOffset.UTC;

    private final UtilisateurRepository utilisateurRepository;
    private final BoutiqueRepository boutiqueRepository;
    private final ProduitRepository produitRepository;
    private final CommandeRepository commandeRepository;
    private final CourseRepository courseRepository;
    private final ChauffeurRepository chauffeurRepository;
    private final VendeurRepository vendeurRepository;
    private final ClientRepository clientRepository;
    private final SignalementRepository signalementRepository;
    private final EcritureComptableRepository ecritureComptableRepository;
    private final PortefeuilleRepository portefeuilleRepository;
    private final PortefeuilleService portefeuilleService;
    private final CommissionCalculator commissionCalculator;
    private final PubliciteService publiciteService;
    private final MaliMallProperties properties;

    public AdminReportingService(UtilisateurRepository utilisateurRepository,
                                  BoutiqueRepository boutiqueRepository,
                                  ProduitRepository produitRepository,
                                  CommandeRepository commandeRepository,
                                  CourseRepository courseRepository,
                                  ChauffeurRepository chauffeurRepository,
                                  VendeurRepository vendeurRepository,
                                  ClientRepository clientRepository,
                                  SignalementRepository signalementRepository,
                                  EcritureComptableRepository ecritureComptableRepository,
                                  PortefeuilleRepository portefeuilleRepository,
                                  PortefeuilleService portefeuilleService,
                                  CommissionCalculator commissionCalculator,
                                  PubliciteService publiciteService,
                                  MaliMallProperties properties) {
        this.utilisateurRepository = utilisateurRepository;
        this.boutiqueRepository = boutiqueRepository;
        this.produitRepository = produitRepository;
        this.commandeRepository = commandeRepository;
        this.courseRepository = courseRepository;
        this.chauffeurRepository = chauffeurRepository;
        this.vendeurRepository = vendeurRepository;
        this.clientRepository = clientRepository;
        this.signalementRepository = signalementRepository;
        this.ecritureComptableRepository = ecritureComptableRepository;
        this.portefeuilleRepository = portefeuilleRepository;
        this.portefeuilleService = portefeuilleService;
        this.commissionCalculator = commissionCalculator;
        this.publiciteService = publiciteService;
        this.properties = properties;
    }

    // ------------------------------------------------------------------
    // Vue d'ensemble
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public VueEnsembleResponse vueEnsemble() {
        List<Commande> commandes = commandeRepository.findAll();
        List<Course> courses = courseRepository.findAll();

        Instant debutMoisCourant = debutMois(0);
        Instant debutMoisPrecedent = debutMois(-1);

        long commandesMoisCourant = commandes.stream().filter(c -> !c.getDateCommande().isBefore(debutMoisCourant)).count();
        long commandesMoisPrecedent = commandes.stream()
                .filter(c -> !c.getDateCommande().isBefore(debutMoisPrecedent) && c.getDateCommande().isBefore(debutMoisCourant))
                .count();
        long coursesMoisCourant = courses.stream().filter(c -> !c.getDateDemande().isBefore(debutMoisCourant)).count();
        long coursesMoisPrecedent = courses.stream()
                .filter(c -> !c.getDateDemande().isBefore(debutMoisPrecedent) && c.getDateDemande().isBefore(debutMoisCourant))
                .count();

        long volumeMmcMoisCourant = commandes.stream()
                .filter(c -> !c.getDateCommande().isBefore(debutMoisCourant))
                .mapToLong(Commande::montantTotalAvecLivraison)
                .sum()
                + courses.stream()
                .filter(c -> !c.getDateDemande().isBefore(debutMoisCourant))
                .mapToLong(Course::getPrixMmc)
                .sum();

        List<PointJournalier> commandesParJour = derniersJours(7, commandes, Commande::getDateCommande);
        List<PointJournalier> coursesParJour = derniersJours(7, courses, Course::getDateDemande);

        List<CommandeApercuResponse> dernieresCommandes = commandes.stream()
                .sorted(Comparator.comparing(Commande::getDateCommande).reversed())
                .limit(5)
                .map(this::apercuCommande)
                .toList();
        List<CourseApercuResponse> dernieresCourses = courses.stream()
                .sorted(Comparator.comparing(Course::getDateDemande).reversed())
                .limit(5)
                .map(this::apercuCourse)
                .toList();

        return new VueEnsembleResponse(
                utilisateurRepository.count(),
                commandesMoisCourant,
                coursesMoisCourant,
                volumeMmcMoisCourant,
                variationPourcent(commandesMoisPrecedent, commandesMoisCourant),
                variationPourcent(coursesMoisPrecedent, coursesMoisCourant),
                commandesParJour,
                coursesParJour,
                dernieresCommandes,
                dernieresCourses);
    }

    // ------------------------------------------------------------------
    // Utilisateurs (annuaire — clients, vendeurs, chauffeurs, admins)
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<AdminUtilisateurResponse> utilisateurs() {
        return utilisateurRepository.findAll().stream()
                .sorted(Comparator.comparing(Utilisateur::getDateCreation).reversed())
                .map(u -> {
                    List<String> roles = new ArrayList<>();
                    if (clientRepository.findByUtilisateurId(u.getId()).isPresent()) roles.add("CLIENT");
                    if (vendeurRepository.findByUtilisateurId(u.getId()).isPresent()) roles.add("VENDEUR");
                    if (chauffeurRepository.findByUtilisateurId(u.getId()).isPresent()) roles.add("CHAUFFEUR");
                    if (u.isEstAdmin()) roles.add("ADMIN");
                    if (roles.isEmpty()) roles.add("CLIENT");

                    int soldeMmc = 0;
                    int soldeGeleMmc = 0;
                    var portefeuille = portefeuilleRepository.findByUtilisateurId(u.getId());
                    if (portefeuille.isPresent()) {
                        soldeMmc = portefeuille.get().getSoldeMmc();
                        soldeGeleMmc = portefeuille.get().getSoldeGeleMmc();
                    }

                    return new AdminUtilisateurResponse(
                            u.getId(),
                            nomComplet(u),
                            u.getTelephone(),
                            u.getEmail(),
                            roles,
                            u.getStatutVerification() == com.malimall.backend.entity.enums.StatutVerification.VERIFIE,
                            u.isSuspendu(),
                            soldeMmc,
                            soldeGeleMmc,
                            u.getDateCreation());
                })
                .toList();
    }

    /**
     * Suspend ou réactive un compte utilisateur directement (client, ou tout
     * autre rôle non-admin) — en plus des suspensions déjà possibles sur une
     * Boutique ou un Chauffeur. Un compte suspendu ne peut plus se connecter
     * (voir UserPrincipal.isEnabled()). Un compte admin ne peut pas être
     * suspendu par ce biais, pour éviter tout risque de blocage de la console.
     */
    @Transactional
    public void suspendreUtilisateur(Long utilisateurId, boolean suspendu) {
        Utilisateur u = utilisateurRepository.findById(utilisateurId)
                .orElseThrow(() -> new RessourceIntrouvableException("Utilisateur introuvable : " + utilisateurId));
        if (u.isEstAdmin()) {
            throw new IllegalArgumentException("Impossible de suspendre un compte administrateur");
        }
        u.setSuspendu(suspendu);
        utilisateurRepository.save(u);
    }

    // ------------------------------------------------------------------
    // Boutiques & produits
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<AdminBoutiqueResponse> boutiques() {
        return boutiqueRepository.findAll().stream()
                .map(b -> new AdminBoutiqueResponse(
                        b.getId(),
                        b.getNom(),
                        nomComplet(b.getProprietaire().getUtilisateur()),
                        b.getProprietaire().getUtilisateur().getTelephone(),
                        produitRepository.findByBoutiqueId(b.getId()).size(),
                        ventesTotalesMmc(b.getId()),
                        b.isCertifiee(),
                        b.isSuspendue(),
                        b.getDateCreation()))
                .toList();
    }

    @Transactional(readOnly = true)
    public AdminBoutiqueDetailResponse boutiqueDetail(Long boutiqueId) {
        Boutique b = boutiqueRepository.findById(boutiqueId)
                .orElseThrow(() -> new RessourceIntrouvableException("Boutique introuvable : " + boutiqueId));
        List<Produit> produits = produitRepository.findByBoutiqueId(boutiqueId);
        List<Commande> commandes = commandeRepository.findByBoutiqueId(boutiqueId);

        List<AdminProduitResponse> produitsResp = produits.stream()
                .map(p -> new AdminProduitResponse(p.getId(), p.getNom(), p.getPrixMmc(), p.getStock(), p.isActif()))
                .toList();
        List<AdminCommandeResponse> commandesResp = commandes.stream()
                .sorted(Comparator.comparing(Commande::getDateCommande).reversed())
                .limit(10)
                .map(c -> new AdminCommandeResponse(
                        c.getId(), nomComplet(c.getAcheteur()), c.getBoutique().getNom(), c.montantTotalAvecLivraison(),
                        c.getStatut(), c.isSignalee(), c.getDateCommande()))
                .toList();

        List<PubliciteResponse> publicitesResp = publiciteService.parBoutique(boutiqueId).stream()
                .map(PubliciteMapper::toResponse)
                .toList();

        return new AdminBoutiqueDetailResponse(
                b.getId(),
                b.getNom(),
                nomComplet(b.getProprietaire().getUtilisateur()),
                b.getProprietaire().getUtilisateur().getTelephone(),
                b.isCertifiee(),
                b.isSuspendue(),
                b.getDateCreation(),
                produits.size(),
                commandes.size(),
                ventesTotalesMmc(boutiqueId),
                produitsResp,
                commandesResp,
                publicitesResp);
    }

    @Transactional
    public void suspendreBoutique(Long boutiqueId, boolean suspendue) {
        Boutique b = boutiqueRepository.findById(boutiqueId)
                .orElseThrow(() -> new RessourceIntrouvableException("Boutique introuvable : " + boutiqueId));
        b.setSuspendue(suspendue);
        boutiqueRepository.save(b);
    }

    /** Certifie (ou retire la certification d'une) boutique — badge "Certifiée" affiché côté Market. */
    @Transactional
    public void certifierBoutique(Long boutiqueId, boolean certifiee) {
        Boutique b = boutiqueRepository.findById(boutiqueId)
                .orElseThrow(() -> new RessourceIntrouvableException("Boutique introuvable : " + boutiqueId));
        b.setCertifiee(certifiee);
        boutiqueRepository.save(b);
    }

    /** Active/désactive un produit précis, sans avoir à suspendre toute la boutique. */
    @Transactional
    public void activerProduit(Long produitId, boolean actif) {
        Produit p = produitRepository.findById(produitId)
                .orElseThrow(() -> new RessourceIntrouvableException("Produit introuvable : " + produitId));
        p.setActif(actif);
        produitRepository.save(p);
    }

    private int ventesTotalesMmc(Long boutiqueId) {
        return commandeRepository.findByBoutiqueId(boutiqueId).stream()
                .filter(c -> c.getStatut() == StatutCommande.LIVREE)
                .mapToInt(Commande::montantTotalAvecLivraison)
                .sum();
    }

    // ------------------------------------------------------------------
    // Commandes Market & litiges
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<AdminCommandeResponse> commandes() {
        return commandeRepository.findAll().stream()
                .sorted(Comparator.comparing(Commande::getDateCommande).reversed())
                .map(c -> new AdminCommandeResponse(
                        c.getId(),
                        nomComplet(c.getAcheteur()),
                        c.getBoutique().getNom(),
                        c.montantTotalAvecLivraison(),
                        c.getStatut(),
                        c.isSignalee(),
                        c.getDateCommande()))
                .toList();
    }

    @Transactional(readOnly = true)
    public AdminCommandeDetailResponse commandeDetail(Long commandeId) {
        Commande c = chargerCommande(commandeId);
        List<EtapeLivraison> pipeline = new ArrayList<>();
        pipeline.add(new EtapeLivraison("Commande passée", c.getDateCommande(), false));
        if (c.getDateAssignation() != null) {
            pipeline.add(new EtapeLivraison("Livreur assigné", c.getDateAssignation(), false));
        }
        if (c.getDateRetrait() != null) {
            pipeline.add(new EtapeLivraison("Colis récupéré chez le vendeur", c.getDateRetrait(), false));
        }
        if (c.getDateLivraison() != null) {
            pipeline.add(new EtapeLivraison("Livré à l'acheteur", c.getDateLivraison(), false));
        }
        if (c.isSignalee()) {
            pipeline.add(new EtapeLivraison("Signalement en cours — fonds bloqués", Instant.now(), true));
        }

        List<SignalementResponse> signalements = signalementRepository.findByCommandeId(commandeId).stream()
                .map(s -> new SignalementResponse(
                        s.getId(), nomComplet(s.getAuteur()), s.getMotif().name(), s.getDescription(), s.getStatut(), s.getDateCreation()))
                .toList();

        String produitNom = c.getLignes().isEmpty() ? "—" : c.getLignes().get(0).getProduit().getNom();
        String livreurNom = c.getChauffeur() != null ? nomComplet(c.getChauffeur().getUtilisateur()) : "—";

        return new AdminCommandeDetailResponse(
                c.getId(), produitNom, nomComplet(c.getAcheteur()), c.getBoutique().getNom(), livreurNom,
                c.montantTotalAvecLivraison(), c.getStatut(), c.isSignalee(), pipeline, signalements);
    }

    /**
     * Traite un litige (commande signalée) selon l'action choisie par l'admin :
     * <ul>
     *   <li>CONFIRMER : règle les fonds comme une livraison normale (mêmes
     *       calculs que {@link LivraisonService#confirmerLivraison}) mais sans
     *       exiger le code acheteur — l'admin se porte garant.</li>
     *   <li>REGENERER_CODE : émet un nouveau code de livraison et lève le
     *       signalement, pour que l'acheteur retente le scan lui-même. Les
     *       fonds restent gelés.</li>
     *   <li>REMBOURSER : débloque les fonds gelés vers l'acheteur (jamais
     *       débités, donc rien à reverser côté vendeur/livreur), annule la
     *       commande. Aucune commission n'est prélevée.</li>
     * </ul>
     */
    @Transactional
    public void resoudreLitige(Long commandeId, ActionLitige action) {
        Commande c = chargerCommande(commandeId);
        if (!c.isSignalee()) {
            throw new TransitionStatutInvalideException("Cette commande n'a pas de signalement ouvert");
        }

        switch (action) {
            case CONFIRMER -> {
                if (c.getStatutLivraison() != StatutLivraison.RECUPEREE && c.getStatutLivraison() != StatutLivraison.EN_ROUTE) {
                    throw new TransitionStatutInvalideException("Cette commande n'est pas encore prête à être livrée");
                }
                Long acheteurId = c.getAcheteur().getId();
                Long vendeurUtilisateurId = c.getBoutique().getProprietaire().getUtilisateur().getId();
                Long chauffeurUtilisateurId = c.getChauffeur().getUtilisateur().getId();
                Long plateformeUtilisateurId = plateformeUtilisateurId();

                CommissionSplit split = commissionCalculator.splitLivraisonNormale(c.getMontantTotalMmc(), c.getFraisLivraisonMmc());

                portefeuilleService.debiter(acheteurId, c.montantTotalAvecLivraison());
                portefeuilleService.crediter(vendeurUtilisateurId, split.sellerReceives());
                portefeuilleService.crediter(chauffeurUtilisateurId, split.driverReceives());
                portefeuilleService.crediter(plateformeUtilisateurId, split.platformCommission());

                ecrire(vendeurUtilisateurId, TypeOperation.VENTE, split.sellerReceives(), c.getId());
                ecrire(chauffeurUtilisateurId, TypeOperation.FRAIS_LIVRAISON, split.driverReceives(), c.getId());
                ecrire(plateformeUtilisateurId, TypeOperation.COMMISSION, split.platformCommission(), c.getId());

                c.setStatut(StatutCommande.LIVREE);
                c.setStatutLivraison(StatutLivraison.LIVREE);
                c.setDateLivraison(Instant.now());
            }
            case REGENERER_CODE -> c.setCodeLivraison(String.format("%06d", RANDOM.nextInt(1_000_000)));
            case REMBOURSER -> {
                portefeuilleService.debloquer(c.getAcheteur().getId(), c.montantTotalAvecLivraison());
                c.setStatut(StatutCommande.ANNULEE);
            }
        }

        c.setSignalee(false);
        commandeRepository.save(c);
        signalementRepository.findByCommandeId(commandeId).stream()
                .filter(s -> "OUVERT".equals(s.getStatut()))
                .forEach(s -> {
                    s.setStatut("RESOLU");
                    signalementRepository.save(s);
                });
    }

    private Commande chargerCommande(Long commandeId) {
        return commandeRepository.findById(commandeId)
                .orElseThrow(() -> new RessourceIntrouvableException("Commande introuvable : " + commandeId));
    }

    private void ecrire(Long utilisateurId, TypeOperation type, int montant, Long commandeId) {
        Utilisateur utilisateur = utilisateurRepository.findById(utilisateurId)
                .orElseThrow(() -> new RessourceIntrouvableException("Utilisateur introuvable : " + utilisateurId));
        ecritureComptableRepository.save(EcritureComptable.builder()
                .utilisateur(utilisateur)
                .typeOperation(type)
                .montantMmc(montant)
                .referenceType("COMMANDE")
                .referenceId(commandeId)
                .build());
    }

    private Long plateformeUtilisateurId() {
        return utilisateurRepository.findByTelephone(properties.plateforme().telephone())
                .orElseThrow(() -> new IllegalStateException(
                        "Utilisateur plateforme introuvable (téléphone " + properties.plateforme().telephone() + ")"))
                .getId();
    }

    // ------------------------------------------------------------------
    // MotoTaxis
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public MotoTaxisResponse motoTaxis() {
        List<Chauffeur> chauffeurs = chauffeurRepository.findAll();

        long nbScooter = chauffeurs.stream().filter(c -> typeVehicule(c) == TypeVehicule.SCOOTER).count();
        long nbTelimani = chauffeurs.stream().filter(c -> typeVehicule(c) == TypeVehicule.TELIMANI).count();
        long nbTricycle = chauffeurs.stream().filter(c -> typeVehicule(c) == TypeVehicule.TRICYCLE).count();

        List<AdminChauffeurResponse> reponses = chauffeurs.stream().map(this::apercuChauffeur).toList();

        List<CourseApercuResponse> coursesRecentes = courseRepository.findAll().stream()
                .sorted(Comparator.comparing(Course::getDateDemande).reversed())
                .limit(5)
                .map(this::apercuCourse)
                .toList();
        List<CommandeApercuResponse> livraisonsRecentes = commandeRepository.findAll().stream()
                .filter(c -> c.getChauffeur() != null)
                .sorted(Comparator.comparing(Commande::getDateCommande).reversed())
                .limit(5)
                .map(this::apercuCommande)
                .toList();

        return new MotoTaxisResponse(nbScooter, nbTelimani, nbTricycle, chauffeurs.size(), reponses, coursesRecentes, livraisonsRecentes);
    }

    @Transactional(readOnly = true)
    public AdminChauffeurDetailResponse chauffeurDetail(Long chauffeurId) {
        Chauffeur c = chauffeurRepository.findById(chauffeurId)
                .orElseThrow(() -> new RessourceIntrouvableException("Chauffeur introuvable : " + chauffeurId));
        Vehicule v = c.getVehicule();
        List<Course> courses = courseRepository.findByChauffeurIdOrderByDateDemandeDesc(c.getId());
        List<Commande> livraisons = commandeRepository.findByChauffeurId(c.getId());

        int revenusGeneresMmc = ecritureComptableRepository.findByUtilisateurIdOrderByDateCreationDesc(c.getUtilisateur().getId()).stream()
                .filter(e -> e.getTypeOperation() == TypeOperation.FRAIS_LIVRAISON || e.getTypeOperation() == TypeOperation.COURSE_REVENU)
                .mapToInt(EcritureComptable::getMontantMmc)
                .sum();

        List<CourseApercuResponse> coursesRecentes = courses.stream().limit(5).map(this::apercuCourse).toList();
        List<CommandeApercuResponse> livraisonsRecentes = livraisons.stream()
                .sorted(Comparator.comparing(Commande::getDateCommande).reversed())
                .limit(5)
                .map(this::apercuCommande)
                .toList();

        return new AdminChauffeurDetailResponse(
                c.getId(),
                nomComplet(c.getUtilisateur()),
                c.getUtilisateur().getTelephone(),
                v != null ? v.getType().name() : "—",
                v != null ? v.getImmatriculation() : "—",
                c.getNoteMoyenne(),
                c.isDisponible(),
                c.isSuspendu(),
                c.getUtilisateur().getDateCreation(),
                courses.size(),
                livraisons.size(),
                revenusGeneresMmc,
                coursesRecentes,
                livraisonsRecentes);
    }

    @Transactional
    public void suspendreChauffeur(Long chauffeurId, boolean suspendu) {
        Chauffeur c = chauffeurRepository.findById(chauffeurId)
                .orElseThrow(() -> new RessourceIntrouvableException("Chauffeur introuvable : " + chauffeurId));
        c.setSuspendu(suspendu);
        if (suspendu) {
            c.setDisponible(false);
        }
        chauffeurRepository.save(c);
    }

    private TypeVehicule typeVehicule(Chauffeur c) {
        return c.getVehicule() != null ? c.getVehicule().getType() : null;
    }

    private AdminChauffeurResponse apercuChauffeur(Chauffeur c) {
        Vehicule v = c.getVehicule();
        return new AdminChauffeurResponse(
                c.getId(),
                nomComplet(c.getUtilisateur()),
                c.getUtilisateur().getTelephone(),
                v != null ? v.getType().name() : "—",
                v != null ? v.getImmatriculation() : "—",
                c.getNoteMoyenne(),
                courseRepository.findByChauffeurIdOrderByDateDemandeDesc(c.getId()).size(),
                commandeRepository.findByChauffeurId(c.getId()).size(),
                c.isDisponible(),
                c.isSuspendu());
    }

    // ------------------------------------------------------------------
    // Revenus
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public RevenusResponse revenus() {
        List<EcritureComptable> commissionsMarket = ecritureComptableRepository
                .findByTypeOperationOrderByDateCreationDesc(TypeOperation.COMMISSION).stream()
                .filter(e -> "COMMANDE".equals(e.getReferenceType()))
                .toList();
        List<EcritureComptable> commissionsMotoTaxis = ecritureComptableRepository
                .findByTypeOperationOrderByDateCreationDesc(TypeOperation.COMMISSION).stream()
                .filter(e -> "COURSE".equals(e.getReferenceType()))
                .toList();

        Instant debutMoisCourant = debutMois(0);
        int commissionsMarketMmc = sommeDuMois(commissionsMarket, debutMoisCourant);
        int commissionsMotoTaxisMmc = sommeDuMois(commissionsMotoTaxis, debutMoisCourant);
        int totalMoisMmc = commissionsMarketMmc + commissionsMotoTaxisMmc;

        int soldePlateformeMmc = portefeuilleService.soldeDisponible(portefeuilleService.obtenirPourUtilisateur(plateformeUtilisateurId()));

        List<EcritureComptable> toutesCommissions = new ArrayList<>();
        toutesCommissions.addAll(commissionsMarket);
        toutesCommissions.addAll(commissionsMotoTaxis);
        List<PointJournalier> revenusParJour = derniersJours(7, toutesCommissions, EcritureComptable::getDateCreation);

        List<Commande> commandesLivrees = commandeRepository.findAll().stream()
                .filter(c -> c.getStatut() == StatutCommande.LIVREE)
                .toList();
        int partVentes = commandesLivrees.stream().mapToInt(Commande::getMontantTotalMmc).sum();
        int partLivraisons = commandesLivrees.stream().mapToInt(Commande::getFraisLivraisonMmc).sum();
        int partCourses = courseRepository.findAll().stream().mapToInt(Course::getPrixMmc).sum();
        int totalParts = Math.max(1, partVentes + partLivraisons + partCourses);

        Map<Long, List<Commande>> parBoutique = commandesLivrees.stream()
                .collect(Collectors.groupingBy(c -> c.getBoutique().getId()));
        List<TopContributeur> topBoutiques = parBoutique.entrySet().stream()
                .map(e -> {
                    Commande premiere = e.getValue().get(0);
                    int total = e.getValue().stream().mapToInt(Commande::montantTotalAvecLivraison).sum();
                    return new TopContributeur(premiere.getBoutique().getNom(), premiere.getBoutique().getQuartier(), total);
                })
                .sorted(Comparator.comparingInt(TopContributeur::montantMmc).reversed())
                .limit(5)
                .toList();

        Map<Long, List<Commande>> parChauffeur = commandesLivrees.stream()
                .filter(c -> c.getChauffeur() != null)
                .collect(Collectors.groupingBy(c -> c.getChauffeur().getId()));
        List<TopContributeur> topChauffeurs = parChauffeur.entrySet().stream()
                .map(e -> {
                    Chauffeur ch = e.getValue().get(0).getChauffeur();
                    int total = e.getValue().stream().mapToInt(Commande::getFraisLivraisonMmc).sum();
                    return new TopContributeur(nomComplet(ch.getUtilisateur()), "Livraisons", total);
                })
                .sorted(Comparator.comparingInt(TopContributeur::montantMmc).reversed())
                .limit(5)
                .toList();

        List<TransactionCommission> transactionsMarket = commandeRepository.findAll().stream()
                .sorted(Comparator.comparing(Commande::getDateCommande).reversed())
                .limit(10)
                .map(c -> new TransactionCommission(
                        "Market",
                        c.getBoutique().getNom(),
                        c.montantTotalAvecLivraison(),
                        c.getStatut() == StatutCommande.LIVREE
                                ? commissionCalculator.splitLivraisonNormale(c.getMontantTotalMmc(), c.getFraisLivraisonMmc()).platformCommission()
                                : null,
                        c.isSignalee(),
                        c.getDateCommande()))
                .toList();

        return new RevenusResponse(
                totalMoisMmc,
                commissionsMarketMmc,
                commissionsMotoTaxisMmc,
                soldePlateformeMmc,
                revenusParJour,
                Math.round(partVentes * 100f / totalParts),
                Math.round(partLivraisons * 100f / totalParts),
                Math.round(partCourses * 100f / totalParts),
                topBoutiques,
                topChauffeurs,
                transactionsMarket);
    }

    private int sommeDuMois(List<EcritureComptable> ecritures, Instant debutMois) {
        return ecritures.stream()
                .filter(e -> !e.getDateCreation().isBefore(debutMois))
                .mapToInt(EcritureComptable::getMontantMmc)
                .sum();
    }

    // ------------------------------------------------------------------
    // Aides communes
    // ------------------------------------------------------------------

    private String nomComplet(Utilisateur u) {
        return u.getPrenom() + " " + u.getNom();
    }

    private CommandeApercuResponse apercuCommande(Commande c) {
        return new CommandeApercuResponse(c.getId(), c.getBoutique().getNom(), nomComplet(c.getAcheteur()), c.montantTotalAvecLivraison(), c.getDateCommande());
    }

    private CourseApercuResponse apercuCourse(Course c) {
        String nomChauffeur = c.getChauffeur() != null ? nomComplet(c.getChauffeur().getUtilisateur()) : "—";
        return new CourseApercuResponse(c.getId(), nomChauffeur, c.getDepart() + " → " + c.getDestination(), c.getPrixMmc(), c.getDateDemande());
    }

    private double variationPourcent(long precedent, long courant) {
        if (precedent == 0) {
            return courant == 0 ? 0 : 100;
        }
        return Math.round((courant - precedent) * 1000.0 / precedent) / 10.0;
    }

    /** Début (00:00 UTC, jour 1) du mois courant + {@code decalage} mois (négatif = mois précédent). */
    private Instant debutMois(int decalage) {
        YearMonth mois = YearMonth.now(ZONE).plusMonths(decalage);
        return mois.atDay(1).atStartOfDay(ZONE).toInstant();
    }

    /** Compte, jour par jour (libellé français à 3 lettres), sur les {@code n} derniers jours inclus aujourd'hui. */
    private <T> List<PointJournalier> derniersJours(int n, List<T> elements, java.util.function.Function<T, Instant> dateExtractor) {
        LocalDate aujourdHui = LocalDate.now(ZONE);
        List<PointJournalier> resultat = new ArrayList<>();
        for (int i = n - 1; i >= 0; i--) {
            LocalDate jour = aujourdHui.minusDays(i);
            Instant debut = jour.atStartOfDay(ZONE).toInstant();
            Instant fin = jour.plusDays(1).atStartOfDay(ZONE).toInstant();
            int total = (int) elements.stream()
                    .map(dateExtractor)
                    .filter(d -> !d.isBefore(debut) && d.isBefore(fin))
                    .count();
            String libelle = jour.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.FRENCH);
            resultat.add(new PointJournalier(libelle, total));
        }
        return resultat;
    }
}
