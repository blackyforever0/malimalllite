package com.malimall.backend.service;

import com.malimall.backend.dto.CatalogueDtos.ProduitRequest;
import com.malimall.backend.dto.CatalogueDtos.ProduitResponse;
import com.malimall.backend.entity.Boutique;
import com.malimall.backend.entity.Produit;
import com.malimall.backend.entity.Vendeur;
import com.malimall.backend.exception.AccesRefuseException;
import com.malimall.backend.exception.RessourceIntrouvableException;
import com.malimall.backend.mapper.ProduitMapper;
import com.malimall.backend.repository.BoutiqueRepository;
import com.malimall.backend.repository.LigneCommandeRepository;
import com.malimall.backend.repository.ProduitRepository;
import com.malimall.backend.repository.VendeurRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class ProduitService {

    private final ProduitRepository produitRepository;
    private final BoutiqueRepository boutiqueRepository;
    private final VendeurRepository vendeurRepository;
    private final LigneCommandeRepository ligneCommandeRepository;
    private final SupabaseStorageService supabaseStorageService;

    public ProduitService(ProduitRepository produitRepository,
                           BoutiqueRepository boutiqueRepository,
                           VendeurRepository vendeurRepository,
                           LigneCommandeRepository ligneCommandeRepository,
                           SupabaseStorageService supabaseStorageService) {
        this.produitRepository = produitRepository;
        this.boutiqueRepository = boutiqueRepository;
        this.vendeurRepository = vendeurRepository;
        this.ligneCommandeRepository = ligneCommandeRepository;
        this.supabaseStorageService = supabaseStorageService;
    }

    /**
     * DTO enrichi du vrai compteur de ventes ({@link LigneCommandeRepository#compterQuantiteVendue})
     * — jamais une valeur inventée. À utiliser par les contrôleurs à la
     * place de {@code ProduitMapper.toResponse(produit)} partout où le badge
     * "vendus" est affiché côté mobile (accueil Market, fiche produit).
     */
    public ProduitResponse versReponse(Produit produit) {
        return ProduitMapper.toResponse(produit, ligneCommandeRepository.compterQuantiteVendue(produit.getId()));
    }

    public List<Produit> listerActifs() {
        return produitRepository.findByActifTrue();
    }

    public List<Produit> listerParBoutique(Long boutiqueId) {
        return produitRepository.findByBoutiqueId(boutiqueId);
    }

    public Produit obtenir(Long id) {
        return produitRepository.findById(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Produit introuvable : " + id));
    }

    @Transactional
    public Produit creer(Long utilisateurId, Long boutiqueId, ProduitRequest req) {
        Boutique boutique = boutiqueRepository.findById(boutiqueId)
                .orElseThrow(() -> new RessourceIntrouvableException("Boutique introuvable : " + boutiqueId));
        Vendeur vendeur = vendeurRepository.findByUtilisateurId(utilisateurId)
                .orElseThrow(() -> new IllegalStateException("Il faut être vendeur pour ajouter un produit"));
        if (!boutique.getProprietaire().getId().equals(vendeur.getId())) {
            throw new AccesRefuseException("Cette boutique ne vous appartient pas");
        }

        Produit produit = Produit.builder()
                .boutique(boutique)
                .nom(req.nom())
                .description(req.description())
                .prixMmc(req.prixMmc())
                .stock(req.stock())
                .categorie(req.categorie())
                .poidsKg(req.poidsKg() != null ? req.poidsKg() : java.math.BigDecimal.ONE)
                .actif(true)
                .build();
        return produitRepository.save(produit);
    }

    @Transactional
    public Produit televerserPhoto(Long utilisateurId, Long produitId, MultipartFile fichier) {
        Produit produit = obtenir(produitId);
        verifierProprietaire(utilisateurId, produit);
        String url = supabaseStorageService.televerser("produits", produit.getId(), fichier);
        produit.setImageUrl(url);
        return produitRepository.save(produit);
    }

    /** Réapprovisionnement : ajoute des unités au stock existant (jamais un remplacement). */
    @Transactional
    public Produit reapprovisionner(Long utilisateurId, Long produitId, int quantiteAjoutee) {
        Produit produit = obtenir(produitId);
        verifierProprietaire(utilisateurId, produit);
        produit.setStock(produit.getStock() + quantiteAjoutee);
        return produitRepository.save(produit);
    }

    /** Le prix promo doit toujours être strictement inférieur au prix normal courant. */
    @Transactional
    public Produit mettreEnPromotion(Long utilisateurId, Long produitId, int prixPromoMmc, int dureeJours) {
        Produit produit = obtenir(produitId);
        verifierProprietaire(utilisateurId, produit);
        if (prixPromoMmc >= produit.getPrixMmc()) {
            throw new IllegalArgumentException("Le prix promo doit être inférieur au prix normal (" + produit.getPrixMmc() + " MMC)");
        }
        produit.setEnPromotion(true);
        produit.setPromoPrixMmc(prixPromoMmc);
        produit.setPromoFinAt(Instant.now().plus(dureeJours, ChronoUnit.DAYS));
        return produitRepository.save(produit);
    }

    @Transactional
    public Produit retirerPromotion(Long utilisateurId, Long produitId) {
        Produit produit = obtenir(produitId);
        verifierProprietaire(utilisateurId, produit);
        produit.setEnPromotion(false);
        return produitRepository.save(produit);
    }

    /**
     * Suppression définitive si le produit n'a jamais été commandé (aucun
     * historique à préserver) ; sinon désactivation seule (retiré du
     * catalogue, mais gardé pour ne pas casser les commandes passées qui le
     * référencent — même logique que la contrainte de clé étrangère en base).
     */
    @Transactional
    public boolean supprimer(Long utilisateurId, Long produitId) {
        Produit produit = obtenir(produitId);
        verifierProprietaire(utilisateurId, produit);
        if (ligneCommandeRepository.existsByProduitId(produitId)) {
            produit.setActif(false);
            produit.setStock(0);
            produitRepository.save(produit);
            return false; // désactivé, pas supprimé
        }
        produitRepository.delete(produit);
        return true; // vraiment supprimé
    }

    private void verifierProprietaire(Long utilisateurId, Produit produit) {
        Vendeur vendeur = vendeurRepository.findByUtilisateurId(utilisateurId)
                .orElseThrow(() -> new IllegalStateException("Il faut être vendeur pour modifier un produit"));
        if (!produit.getBoutique().getProprietaire().getId().equals(vendeur.getId())) {
            throw new AccesRefuseException("Ce produit ne vous appartient pas");
        }
    }
}
