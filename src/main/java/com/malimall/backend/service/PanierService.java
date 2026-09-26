package com.malimall.backend.service;

import com.malimall.backend.config.MaliMallProperties;
import com.malimall.backend.dto.CommandeDtos.CommandeResponse;
import com.malimall.backend.dto.PanierDtos.PanierResponse;
import com.malimall.backend.entity.*;
import com.malimall.backend.entity.enums.StatutCommande;
import com.malimall.backend.exception.RessourceIntrouvableException;
import com.malimall.backend.mapper.CommandeMapper;
import com.malimall.backend.mapper.PanierMapper;
import com.malimall.backend.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Panier "courant" (ajout/retrait de lignes) + checkout
 * ({@link #validerPanier}). Le checkout est LA pièce la plus importante du
 * lot Market : il regroupe les lignes par boutique, vérifie stock/actif,
 * crée une Commande par boutique, gèle les fonds correspondants, génère les
 * deux codes, décrémente les stocks et vide le panier — tout dans une seule
 * transaction (tout-ou-rien).
 *
 * Important (JPA) : {@code spring.jpa.open-in-view=false} ferme la session
 * Hibernate dès que la méthode @Transactional retourne — un contrôleur qui
 * mapperait l'entité APRÈS l'appel planterait en LazyInitializationException
 * dès qu'il toucherait autre chose que l'id d'une association @ManyToOne/
 * @OneToOne (getId() seul ne déclenche jamais de chargement, tout le reste
 * si). Le mapping DTO se fait donc ICI, à l'intérieur des méthodes
 * transactionnelles — les contrôleurs ne reçoivent jamais l'entité brute.
 */
@Service
public class PanierService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final PanierRepository panierRepository;
    private final CommandeRepository commandeRepository;
    private final ProduitRepository produitRepository;
    private final ClientRepository clientRepository;
    private final LigneCommandeRepository ligneCommandeRepository;
    private final PortefeuilleService portefeuilleService;
    private final MaliMallProperties properties;

    public PanierService(PanierRepository panierRepository,
                          CommandeRepository commandeRepository,
                          ProduitRepository produitRepository,
                          ClientRepository clientRepository,
                          LigneCommandeRepository ligneCommandeRepository,
                          PortefeuilleService portefeuilleService,
                          MaliMallProperties properties) {
        this.panierRepository = panierRepository;
        this.commandeRepository = commandeRepository;
        this.produitRepository = produitRepository;
        this.clientRepository = clientRepository;
        this.ligneCommandeRepository = ligneCommandeRepository;
        this.portefeuilleService = portefeuilleService;
        this.properties = properties;
    }

    /** Récupère l'entité panier courante du Client, ou en crée une vide à la demande — usage interne uniquement. */
    @Transactional
    public Panier panierDe(Long utilisateurId) {
        Client client = clientRepository.findByUtilisateurId(utilisateurId)
                .orElseThrow(() -> new RessourceIntrouvableException("Aucun profil client pour cet utilisateur"));
        return panierRepository.findByClientId(client.getId())
                .orElseGet(() -> panierRepository.save(Panier.builder().client(client).build()));
    }

    /** Point d'entrée pour les contrôleurs : mappe en DTO pendant que la session est encore ouverte. */
    @Transactional
    public PanierResponse obtenirPanier(Long utilisateurId) {
        return PanierMapper.toResponse(panierDe(utilisateurId));
    }

    @Transactional
    public PanierResponse ajouterLigne(Long utilisateurId, Long produitId, int quantite) {
        Panier panier = panierDe(utilisateurId);
        Produit produit = produitRepository.findById(produitId)
                .orElseThrow(() -> new RessourceIntrouvableException("Produit introuvable : " + produitId));
        if (!produit.isActif()) {
            throw new IllegalStateException("Ce produit n'est plus disponible");
        }

        Optional<LigneCommande> ligneExistante = panier.getLignes().stream()
                .filter(l -> l.getProduit().getId().equals(produitId))
                .findFirst();

        if (ligneExistante.isPresent()) {
            LigneCommande ligne = ligneExistante.get();
            ligne.setQuantite(ligne.getQuantite() + quantite);
            ligneCommandeRepository.save(ligne);
        } else {
            LigneCommande ligne = LigneCommande.builder()
                    .panier(panier)
                    .produit(produit)
                    .quantite(quantite)
                    .prixUnitaireMmc(produit.prixEffectifMmc())
                    .build();
            panier.getLignes().add(ligneCommandeRepository.save(ligne));
        }
        return PanierMapper.toResponse(panier);
    }

    /** Suppression explicite (jamais via un simple retrait de la collection, cf. Panier.lignes). */
    @Transactional
    public PanierResponse retirerLigne(Long utilisateurId, Long ligneId) {
        Panier panier = panierDe(utilisateurId);
        LigneCommande ligne = panier.getLignes().stream()
                .filter(l -> l.getId().equals(ligneId))
                .findFirst()
                .orElseThrow(() -> new RessourceIntrouvableException("Cette ligne n'existe pas dans votre panier"));
        panier.getLignes().remove(ligne);
        ligneCommandeRepository.delete(ligne);
        return PanierMapper.toResponse(panier);
    }

    /** Checkout sans adresse de livraison précisée (conservé pour les appels existants). */
    @Transactional
    public List<CommandeResponse> validerPanier(Long utilisateurId) {
        return validerPanier(utilisateurId, null);
    }

    /** Checkout — le viewer des CommandeResponse renvoyées est toujours l'acheteur lui-même. */
    @Transactional
    public List<CommandeResponse> validerPanier(Long utilisateurId, String adresseLivraison) {
        return validerPanier(utilisateurId, adresseLivraison, null, null);
    }

    @Transactional
    public List<CommandeResponse> validerPanier(Long utilisateurId, String adresseLivraison,
                                                Double latitude, Double longitude) {
        if ((latitude == null) != (longitude == null)
                || (latitude != null && (Math.abs(latitude) > 90 || Math.abs(longitude) > 180))) {
            throw new IllegalArgumentException("Position de livraison invalide");
        }
        Panier panier = panierDe(utilisateurId);
        if (panier.getLignes().isEmpty()) {
            throw new IllegalStateException("Le panier est vide");
        }

        // Vérification intégrale AVANT toute mutation, pour rester tout-ou-rien.
        for (LigneCommande ligne : panier.getLignes()) {
            Produit produit = ligne.getProduit();
            if (!produit.isActif()) {
                throw new IllegalStateException("Produit indisponible : " + produit.getNom());
            }
            if (produit.getStock() < ligne.getQuantite()) {
                throw new IllegalStateException("Stock insuffisant pour : " + produit.getNom());
            }
        }

        Map<Boutique, List<LigneCommande>> lignesParBoutique = panier.getLignes().stream()
                .collect(Collectors.groupingBy(l -> l.getProduit().getBoutique()));

        Utilisateur acheteur = panier.getClient().getUtilisateur();
        int fraisLivraison = properties.livraison().fraisFixeMmc();
        // Adresse saisie, sinon l'adresse par défaut du client (puis mémorisée pour la prochaine fois).
        String adresse = adresseLivraison != null && !adresseLivraison.isBlank()
                ? adresseLivraison.trim()
                : panier.getClient().getAdresseLivraisonDefaut();
        if (adresse != null) {
            panier.getClient().setAdresseLivraisonDefaut(adresse);
        }

        List<Commande> commandesCreees = new ArrayList<>();
        for (Map.Entry<Boutique, List<LigneCommande>> entree : lignesParBoutique.entrySet()) {
            Boutique boutique = entree.getKey();
            List<LigneCommande> lignes = entree.getValue();
            int totalProduits = lignes.stream().mapToInt(LigneCommande::sousTotal).sum();

            Commande commande = Commande.builder()
                    .acheteur(acheteur)
                    .boutique(boutique)
                    .montantTotalMmc(totalProduits)
                    .fraisLivraisonMmc(fraisLivraison)
                    .statut(StatutCommande.CONFIRMEE)
                    .codeRetrait(genererCode())
                    .codeLivraison(genererCode())
                    .adresseLivraison(adresse)
                    .livraisonLatitude(latitude)
                    .livraisonLongitude(longitude)
                    .build();
            commande = commandeRepository.save(commande);

            for (LigneCommande ligne : lignes) {
                ligne.getProduit().decrementerStock(ligne.getQuantite());
                ligne.setPanier(null);
                ligne.setCommande(commande);
                ligneCommandeRepository.save(ligne);
                commande.getLignes().add(ligne);
            }

            portefeuilleService.geler(acheteur.getId(), commande.montantTotalAvecLivraison());
            commandesCreees.add(commande);
        }

        panier.getLignes().clear();

        return commandesCreees.stream()
                .map(c -> CommandeMapper.toResponse(c, utilisateurId, false))
                .toList();
    }

    private String genererCode() {
        return String.format("%06d", RANDOM.nextInt(1_000_000));
    }
}
