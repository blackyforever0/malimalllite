package com.malimall.backend.web;

import com.malimall.backend.dto.CatalogueDtos.MettreEnPromotionRequest;
import com.malimall.backend.dto.CatalogueDtos.ProduitResponse;
import com.malimall.backend.dto.CatalogueDtos.ReapprovisionnerRequest;
import com.malimall.backend.security.UserPrincipal;
import com.malimall.backend.service.ProduitService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/produits")
public class ProduitController {

    private final ProduitService produitService;

    public ProduitController(ProduitService produitService) {
        this.produitService = produitService;
    }

    @GetMapping
    public List<ProduitResponse> lister() {
        return produitService.listerActifs().stream().map(produitService::versReponse).toList();
    }

    @GetMapping("/{id}")
    public ProduitResponse obtenir(@PathVariable Long id) {
        return produitService.versReponse(produitService.obtenir(id));
    }

    /** Réservé au vendeur propriétaire de la boutique du produit — voir ProduitService.televerserPhoto. */
    @PreAuthorize("hasRole('VENDEUR')")
    @PostMapping(value = "/{id}/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProduitResponse> televerserPhoto(@AuthenticationPrincipal UserPrincipal principal,
                                                             @PathVariable Long id,
                                                             @RequestParam("fichier") MultipartFile fichier) {
        var produit = produitService.televerserPhoto(principal.getId(), id, fichier);
        return ResponseEntity.ok(produitService.versReponse(produit));
    }

    /** Réapprovisionnement — ajoute des unités au stock existant. Réservé au vendeur propriétaire. */
    @PreAuthorize("hasRole('VENDEUR')")
    @PostMapping("/{id}/stock")
    public ResponseEntity<ProduitResponse> reapprovisionner(@AuthenticationPrincipal UserPrincipal principal,
                                                              @PathVariable Long id,
                                                              @Valid @RequestBody ReapprovisionnerRequest req) {
        var produit = produitService.reapprovisionner(principal.getId(), id, req.quantite());
        return ResponseEntity.ok(produitService.versReponse(produit));
    }

    /** Active une promotion (prix réduit temporaire). Réservé au vendeur propriétaire. */
    @PreAuthorize("hasRole('VENDEUR')")
    @PostMapping("/{id}/promotion")
    public ResponseEntity<ProduitResponse> mettreEnPromotion(@AuthenticationPrincipal UserPrincipal principal,
                                                               @PathVariable Long id,
                                                               @Valid @RequestBody MettreEnPromotionRequest req) {
        var produit = produitService.mettreEnPromotion(principal.getId(), id, req.prixMmc(), req.dureeJours());
        return ResponseEntity.ok(produitService.versReponse(produit));
    }

    /** Retire une promotion active. Réservé au vendeur propriétaire. */
    @PreAuthorize("hasRole('VENDEUR')")
    @DeleteMapping("/{id}/promotion")
    public ResponseEntity<ProduitResponse> retirerPromotion(@AuthenticationPrincipal UserPrincipal principal,
                                                              @PathVariable Long id) {
        var produit = produitService.retirerPromotion(principal.getId(), id);
        return ResponseEntity.ok(produitService.versReponse(produit));
    }

    /**
     * Supprime le produit — vraiment supprimé s'il n'a jamais été commandé,
     * sinon simplement désactivé (retiré du catalogue) pour préserver
     * l'historique des commandes déjà passées. Réservé au vendeur propriétaire.
     */
    @PreAuthorize("hasRole('VENDEUR')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Boolean>> supprimer(@AuthenticationPrincipal UserPrincipal principal,
                                                            @PathVariable Long id) {
        boolean vraimentSupprime = produitService.supprimer(principal.getId(), id);
        return ResponseEntity.ok(Map.of("supprime", vraimentSupprime));
    }
}
