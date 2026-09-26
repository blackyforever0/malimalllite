package com.malimall.backend.integration;

import com.malimall.backend.dto.CommandeDtos.CommandeResponse;
import com.malimall.backend.dto.PanierDtos.PanierResponse;
import com.malimall.backend.entity.Boutique;
import com.malimall.backend.entity.Chauffeur;
import com.malimall.backend.entity.Client;
import com.malimall.backend.entity.EcritureComptable;
import com.malimall.backend.entity.Portefeuille;
import com.malimall.backend.entity.Produit;
import com.malimall.backend.entity.Utilisateur;
import com.malimall.backend.entity.Vehicule;
import com.malimall.backend.entity.Vendeur;
import com.malimall.backend.entity.enums.MotifSignalement;
import com.malimall.backend.entity.enums.StatutCommande;
import com.malimall.backend.entity.enums.StatutLivraison;
import com.malimall.backend.entity.enums.TypeOperation;
import com.malimall.backend.entity.enums.TypeVehicule;
import com.malimall.backend.exception.CodeInvalideException;
import com.malimall.backend.exception.FondsInsuffisantsException;
import com.malimall.backend.exception.TransitionStatutInvalideException;
import com.malimall.backend.repository.BoutiqueRepository;
import com.malimall.backend.repository.ChauffeurRepository;
import com.malimall.backend.repository.ClientRepository;
import com.malimall.backend.repository.EcritureComptableRepository;
import com.malimall.backend.repository.ProduitRepository;
import com.malimall.backend.repository.SignalementRepository;
import com.malimall.backend.repository.UtilisateurRepository;
import com.malimall.backend.repository.VehiculeRepository;
import com.malimall.backend.repository.VendeurRepository;
import com.malimall.backend.service.CommandeService;
import com.malimall.backend.service.LivraisonService;
import com.malimall.backend.service.PanierService;
import com.malimall.backend.service.PortefeuilleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Rejoue, de bout en bout et avec les mêmes montants, le parcours Market déjà
 * vérifié manuellement via Swagger sur la machine de l'utilisateur : ajout au
 * panier -> validation (gel des fonds) -> assignation d'un chauffeur -> scan
 * du code de retrait -> scan du code de livraison -> règlement des fonds
 * (vendeur/chauffeur/plateforme). Fige ce comportement dans un test
 * automatisé plutôt que de dépendre d'une vérification manuelle à chaque
 * changement, et vérifie au passage le modèle de sécurité à deux codes
 * (codeRetrait/codeLivraison masqués selon le rôle du demandeur).
 *
 * Important (Spring Test) : chaque scénario qui déclenche volontairement une
 * exception métier (mauvais code, double scan...) est un @Test À PART — une
 * fois qu'un appel transactionnel imbriqué a levé une exception, la
 * transaction (partagée avec celle du test, ouverte par @Transactional
 * ci-dessous) est marquée rollback-only, et le PROCHAIN appel transactionnel
 * qui se termine normalement lèverait alors UnexpectedRollbackException — pas
 * une erreur du code testé, un piège classique de Spring Test. On ne
 * déclenche donc jamais une exception attendue puis d'autres appels normaux
 * dans le même test.
 *
 * Tourne sur H2 en mémoire, schéma généré depuis les entités JPA (Flyway
 * désactivé pour ce profil — voir application-test.yml) : ni PostgreSQL ni
 * Docker ne sont nécessaires pour lancer {@code mvn test}.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ParcoursMarketIntegrationTest {

    @Autowired private UtilisateurRepository utilisateurRepository;
    @Autowired private ClientRepository clientRepository;
    @Autowired private VendeurRepository vendeurRepository;
    @Autowired private ChauffeurRepository chauffeurRepository;
    @Autowired private VehiculeRepository vehiculeRepository;
    @Autowired private BoutiqueRepository boutiqueRepository;
    @Autowired private ProduitRepository produitRepository;
    @Autowired private EcritureComptableRepository ecritureComptableRepository;
    @Autowired private PortefeuilleService portefeuilleService;
    @Autowired private PanierService panierService;
    @Autowired private CommandeService commandeService;
    @Autowired private LivraisonService livraisonService;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private SignalementRepository signalementRepository;

    private Long acheteurId;
    private Long vendeurUtilisateurId;
    private Long chauffeurUtilisateurId;
    private Long produitId;

    /** Reproduit exactement les comptes de démo de V2__seed_demo_data.sql, construits ici via les repositories. */
    @BeforeEach
    void seedDonneesDeDemo() {
        creerUtilisateurAvecClientEtPortefeuille("MaliMall", "Plateforme", "+000000000", 0);

        acheteurId = creerUtilisateurAvecClientEtPortefeuille("Diarra", "Aïcha", "+22370000002", 50_000);

        vendeurUtilisateurId = creerUtilisateurAvecClientEtPortefeuille("Traoré", "Moussa", "+22370000003", 0);
        Vendeur vendeur = vendeurRepository.save(Vendeur.builder()
                .utilisateur(utilisateurRepository.findById(vendeurUtilisateurId).orElseThrow())
                .build());
        Boutique boutique = boutiqueRepository.save(Boutique.builder()
                .proprietaire(vendeur)
                .nom("Boutique Traoré")
                .categorie("Alimentation")
                .certifiee(true)
                .build());
        Produit produit = produitRepository.save(Produit.builder()
                .boutique(boutique)
                .nom("Sac de riz 25kg")
                .prixMmc(1800)
                .stock(40)
                .categorie("Alimentation")
                .actif(true)
                .build());
        produitId = produit.getId();

        chauffeurUtilisateurId = creerUtilisateurAvecClientEtPortefeuille("Coulibaly", "Ibrahim", "+22370000004", 0);
        Chauffeur chauffeur = chauffeurRepository.save(Chauffeur.builder()
                .utilisateur(utilisateurRepository.findById(chauffeurUtilisateurId).orElseThrow())
                .disponible(true)
                .build());
        Vehicule vehicule = vehiculeRepository.save(Vehicule.builder()
                .chauffeur(chauffeur)
                .type(TypeVehicule.TELIMANI)
                .immatriculation("BKO-1234-A")
                .build());
        // Côté inverse de la relation, renseigné à la main dans ce contexte de persistance de test.
        chauffeur.setVehicule(vehicule);
    }

    private Long creerUtilisateurAvecClientEtPortefeuille(String nom, String prenom, String telephone, int soldeInitial) {
        Utilisateur utilisateur = utilisateurRepository.save(Utilisateur.builder()
                .nom(nom)
                .prenom(prenom)
                .telephone(telephone)
                .motDePasse(passwordEncoder.encode("password123"))
                .build());
        clientRepository.save(Client.builder().utilisateur(utilisateur).build());
        portefeuilleService.creerPour(utilisateur);
        if (soldeInitial > 0) {
            portefeuilleService.crediter(utilisateur.getId(), soldeInitial);
        }
        return utilisateur.getId();
    }

    /** Panier validé + chauffeur assigné — état de départ commun aux tests qui exercent scannerCodeRetrait(). */
    private CommandeResponse creerCommandeAssigneeAuChauffeur() {
        panierService.ajouterLigne(acheteurId, produitId, 1);
        CommandeResponse commande = panierService.validerPanier(acheteurId).get(0);
        livraisonService.assignerChauffeur(commande.id(), chauffeurUtilisateurId);
        return commande;
    }

    @Test
    void parcoursCompletAchatLivraisonEtReglementDesFonds() {
        // 1) Ajout au panier : 1 x "Sac de riz 25kg" (1800 MMC)
        PanierResponse panier = panierService.ajouterLigne(acheteurId, produitId, 1);
        assertThat(panier.totalMmc()).isEqualTo(1800);

        // 2) Validation du panier -> une seule Commande (un seul vendeur représenté), fonds gelés côté acheteur.
        // Vue acheteur à ce stade : ni codeRetrait (détenu par le vendeur) ni codeLivraison
        // (détenu par le livreur) ne doivent apparaître — cf. CommandeMapper.
        List<CommandeResponse> commandes = panierService.validerPanier(acheteurId);
        assertThat(commandes).hasSize(1);
        CommandeResponse commande = commandes.get(0);
        assertThat(commande.montantTotalMmc()).isEqualTo(1800);
        assertThat(commande.fraisLivraisonMmc()).isEqualTo(300);
        assertThat(commande.statut()).isEqualTo(StatutCommande.CONFIRMEE);
        assertThat(commande.statutLivraison()).isEqualTo(StatutLivraison.EN_ATTENTE_LIVREUR);
        assertThat(commande.codeRetrait()).isNull();
        assertThat(commande.codeLivraison()).isNull();

        Portefeuille acheteurApresValidation = portefeuilleService.obtenirPourUtilisateur(acheteurId);
        assertThat(acheteurApresValidation.getSoldeMmc()).isEqualTo(50_000); // rien débité, juste gelé
        assertThat(acheteurApresValidation.getSoldeGeleMmc()).isEqualTo(2_100); // 1800 + 300

        assertThat(produitRepository.findById(produitId).orElseThrow().getStock()).isEqualTo(39); // décrémenté

        // 3) Le chauffeur prend la livraison
        CommandeResponse apresAssignation = livraisonService.assignerChauffeur(commande.id(), chauffeurUtilisateurId);
        assertThat(apresAssignation.statutLivraison()).isEqualTo(StatutLivraison.ASSIGNEE);
        assertThat(apresAssignation.chauffeurId()).isNotNull();

        // 4) Le vendeur (propriétaire de la boutique) est le seul à voir codeRetrait
        String codeRetrait = commandeService.obtenir(commande.id(), vendeurUtilisateurId, false).codeRetrait();
        assertThat(codeRetrait).hasSize(6);
        assertThat(commandeService.obtenir(commande.id(), vendeurUtilisateurId, false).codeLivraison())
                .as("le vendeur ne doit jamais voir codeLivraison")
                .isNull();

        // 5) Scan du code de retrait chez le vendeur
        CommandeResponse apresRetrait = livraisonService.scannerCodeRetrait(commande.id(), chauffeurUtilisateurId, codeRetrait);
        assertThat(apresRetrait.statutLivraison()).isEqualTo(StatutLivraison.RECUPEREE);

        // 6) Le colis étant récupéré, le livreur détient maintenant codeLivraison (il le présente
        // à l'acheteur, qui le scanne) — l'acheteur, lui, ne le reçoit jamais de l'API.
        String codeLivraison = commandeService.obtenir(commande.id(), chauffeurUtilisateurId, false).codeLivraison();
        assertThat(codeLivraison).hasSize(6);
        assertThat(commandeService.obtenir(commande.id(), acheteurId, false).codeLivraison())
                .as("l'acheteur ne doit jamais recevoir codeLivraison : il le scanne chez le livreur")
                .isNull();
        assertThat(commandeService.obtenir(commande.id(), vendeurUtilisateurId, false).chauffeurNom())
                .as("le vendeur voit qui vient récupérer le colis")
                .isNotBlank();

        // 7) L'acheteur scanne le code présenté par le livreur -> règlement des fonds
        CommandeResponse apresLivraison = livraisonService.confirmerLivraison(commande.id(), acheteurId, codeLivraison);
        assertThat(apresLivraison.statut()).isEqualTo(StatutCommande.LIVREE);
        assertThat(apresLivraison.statutLivraison()).isEqualTo(StatutLivraison.LIVREE);
        assertThat(apresLivraison.dateLivraison()).isNotNull();

        // 8) Soldes finaux — exactement les montants vérifiés manuellement via Swagger
        assertThat(portefeuilleService.obtenirPourUtilisateur(acheteurId).getSoldeMmc()).isEqualTo(47_900); // 50000 - 2100
        assertThat(portefeuilleService.obtenirPourUtilisateur(acheteurId).getSoldeGeleMmc()).isZero();
        assertThat(portefeuilleService.obtenirPourUtilisateur(vendeurUtilisateurId).getSoldeMmc()).isEqualTo(1_656); // 1800 - 144
        assertThat(portefeuilleService.obtenirPourUtilisateur(chauffeurUtilisateurId).getSoldeMmc()).isEqualTo(276); // 300 - 24
        Long plateformeId = utilisateurRepository.findByTelephone("+000000000").orElseThrow().getId();
        assertThat(portefeuilleService.obtenirPourUtilisateur(plateformeId).getSoldeMmc()).isEqualTo(168); // 144 + 24

        // 9) Trois écritures comptables tracent le règlement (vente, frais de livraison, commission)
        List<EcritureComptable> ecritures = ecritureComptableRepository.findByReferenceTypeAndReferenceId("COMMANDE", commande.id());
        assertThat(ecritures).hasSize(3);
        assertThat(ecritures).extracting(EcritureComptable::getTypeOperation)
                .containsExactlyInAnyOrder(TypeOperation.VENTE, TypeOperation.FRAIS_LIVRAISON, TypeOperation.COMMISSION);

        // 10) La commande apparaît dans les trois vues par rôle
        assertThat(commandeService.mesCommandesAcheteur(acheteurId, false)).hasSize(1);
        assertThat(commandeService.mesCommandesVendeur(vendeurUtilisateurId, false)).hasSize(1);
        assertThat(commandeService.mesCommandesChauffeur(chauffeurUtilisateurId, false)).hasSize(1);

        // 11) Idempotence : rejouer confirmerLivraison() renvoie normalement (garde interne, pas d'exception)
        // et ne doit rien re-créditer — confirmerLivraison() renvoie tel quel dès que statut == LIVREE.
        CommandeResponse rejoue = livraisonService.confirmerLivraison(commande.id(), acheteurId, codeLivraison);
        assertThat(rejoue.statut()).isEqualTo(StatutCommande.LIVREE);
        assertThat(portefeuilleService.obtenirPourUtilisateur(vendeurUtilisateurId).getSoldeMmc()).isEqualTo(1_656);
    }

    @Test
    void scannerCodeRetrait_refuseUnCodeInvalide() {
        CommandeResponse commande = creerCommandeAssigneeAuChauffeur();

        assertThatThrownBy(() -> livraisonService.scannerCodeRetrait(commande.id(), chauffeurUtilisateurId, "000000"))
                .isInstanceOf(CodeInvalideException.class);
    }

    @Test
    void scannerCodeRetrait_refuseUnDeuxiemeScanApresLePremier() {
        // Reproduit le double-clic déjà observé lors du test manuel via Swagger : le deuxième
        // scan doit être bloqué par la garde d'état, pas rejoué silencieusement.
        CommandeResponse commande = creerCommandeAssigneeAuChauffeur();
        String codeRetrait = commandeService.obtenir(commande.id(), vendeurUtilisateurId, false).codeRetrait();

        livraisonService.scannerCodeRetrait(commande.id(), chauffeurUtilisateurId, codeRetrait); // premier scan, OK

        assertThatThrownBy(() -> livraisonService.scannerCodeRetrait(commande.id(), chauffeurUtilisateurId, codeRetrait))
                .isInstanceOf(TransitionStatutInvalideException.class);
    }

    @Test
    void commandesDisponibles_montreLesLivraisonsPasEncoreAssigneesPuisLesRetireUneFoisUnChauffeurAssigne() {
        panierService.ajouterLigne(acheteurId, produitId, 1);
        CommandeResponse commande = panierService.validerPanier(acheteurId).get(0);

        // Fraîchement créée (EN_ATTENTE_LIVREUR) : visible dans la liste "disponibles" du chauffeur.
        List<CommandeResponse> disponibles = commandeService.commandesDisponibles(chauffeurUtilisateurId);
        assertThat(disponibles).extracting(CommandeResponse::id).contains(commande.id());

        // Le chauffeur qui parcourt ne voit ni codeRetrait ni codeLivraison (il n'est pas encore
        // assigné) — la commande reste masquée par CommandeMapper.
        CommandeResponse vueChauffeur = disponibles.stream()
                .filter(c -> c.id().equals(commande.id()))
                .findFirst().orElseThrow();
        assertThat(vueChauffeur.codeRetrait()).isNull();
        assertThat(vueChauffeur.codeLivraison()).isNull();

        // Une fois assignée, elle disparaît de la liste des livraisons disponibles.
        livraisonService.assignerChauffeur(commande.id(), chauffeurUtilisateurId);
        assertThat(commandeService.commandesDisponibles(chauffeurUtilisateurId))
                .extracting(CommandeResponse::id)
                .doesNotContain(commande.id());
    }

    @Test
    void validerPanier_refuseQuandLeStockEstInsuffisant() {
        Produit produit = produitRepository.findById(produitId).orElseThrow();
        produit.setStock(0);
        produitRepository.save(produit);

        panierService.ajouterLigne(acheteurId, produitId, 1);

        assertThatThrownBy(() -> panierService.validerPanier(acheteurId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Stock insuffisant");
    }

    @Test
    void validerPanier_refuseQuandLesFondsSontInsuffisants() {
        // Un acheteur sans solde ne doit jamais pouvoir geler des fonds qu'il n'a pas.
        Long acheteurSansFonds = creerUtilisateurAvecClientEtPortefeuille("Sanogo", "Oumar", "+22370000099", 0);

        panierService.ajouterLigne(acheteurSansFonds, produitId, 1);

        assertThatThrownBy(() -> panierService.validerPanier(acheteurSansFonds))
                .isInstanceOf(FondsInsuffisantsException.class);
    }

    @Test
    void assignerChauffeur_refuseUnColisTropLourdPourLeVehicule() {
        // Telimani : 20 kg maximum — 2 sacs de 15 kg dépassent sa capacité.
        Produit produit = produitRepository.findById(produitId).orElseThrow();
        produit.setPoidsKg(new BigDecimal("15.00"));
        produitRepository.save(produit);
        panierService.ajouterLigne(acheteurId, produitId, 2);
        CommandeResponse commande = panierService.validerPanier(acheteurId, "Hamdallaye").get(0);
        assertThat(commande.poidsTotalKg()).isEqualByComparingTo("30.00");
        assertThat(commande.adresseLivraison()).isEqualTo("Hamdallaye");

        assertThatThrownBy(() -> livraisonService.assignerChauffeur(commande.id(), chauffeurUtilisateurId))
                .isInstanceOf(TransitionStatutInvalideException.class)
                .hasMessageContaining("trop lourd");
    }

    @Test
    void signalement_bloqueLaConfirmationDeLivraison() {
        CommandeResponse commande = creerCommandeAssigneeAuChauffeur();
        String codeRetrait = commandeService.obtenir(commande.id(), vendeurUtilisateurId, false).codeRetrait();
        livraisonService.scannerCodeRetrait(commande.id(), chauffeurUtilisateurId, codeRetrait);
        String codeLivraison = commandeService.obtenir(commande.id(), chauffeurUtilisateurId, false).codeLivraison();

        CommandeResponse signalee = livraisonService.signaler(
                commande.id(), acheteurId, MotifSignalement.COLIS_ENDOMMAGE, "Carton déchiré");
        assertThat(signalee.signalee()).isTrue();
        assertThat(signalementRepository.findByCommandeId(commande.id())).hasSize(1);

        assertThatThrownBy(() -> livraisonService.confirmerLivraison(commande.id(), acheteurId, codeLivraison))
                .isInstanceOf(TransitionStatutInvalideException.class)
                .hasMessageContaining("signalement");
    }

    @Test
    void noterLivreur_metAJourLaNoteMoyenneDuChauffeur() {
        CommandeResponse commande = creerCommandeAssigneeAuChauffeur();
        assertThat(commande.dateAssignation()).isNull(); // réponse de validerPanier, avant assignation
        String codeRetrait = commandeService.obtenir(commande.id(), vendeurUtilisateurId, false).codeRetrait();
        CommandeResponse apresRetrait = livraisonService.scannerCodeRetrait(commande.id(), chauffeurUtilisateurId, codeRetrait);
        assertThat(apresRetrait.dateAssignation()).isNotNull();
        assertThat(apresRetrait.dateRetrait()).isNotNull();
        String codeLivraison = commandeService.obtenir(commande.id(), chauffeurUtilisateurId, false).codeLivraison();
        livraisonService.confirmerLivraison(commande.id(), acheteurId, codeLivraison);

        CommandeResponse notee = livraisonService.noterLivreur(commande.id(), acheteurId, 3);

        assertThat(notee.noteLivreur()).isEqualTo(3);
        Chauffeur chauffeur = chauffeurRepository.findByUtilisateurId(chauffeurUtilisateurId).orElseThrow();
        assertThat(chauffeur.getNombreNotes()).isEqualTo(1);
        assertThat(chauffeur.getNoteMoyenne()).isEqualByComparingTo("3.0");
    }
}
