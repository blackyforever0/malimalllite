package com.malimall.backend.web;

import com.malimall.backend.dto.CatalogueDtos.*;
import com.malimall.backend.mapper.BoutiqueMapper;
import com.malimall.backend.mapper.ProduitMapper;
import com.malimall.backend.security.UserPrincipal;
import com.malimall.backend.service.BoutiqueService;
import com.malimall.backend.service.ProduitService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/boutiques")
public class BoutiqueController {

    private final BoutiqueService boutiqueService;
    private final ProduitService produitService;

    public BoutiqueController(BoutiqueService boutiqueService, ProduitService produitService) {
        this.boutiqueService = boutiqueService;
        this.produitService = produitService;
    }

    @GetMapping
    public List<BoutiqueResponse> lister() {
        return boutiqueService.lister().stream().map(BoutiqueMapper::toResponse).toList();
    }

    @GetMapping("/{id}")
    public BoutiqueResponse obtenir(@PathVariable Long id) {
        return BoutiqueMapper.toResponse(boutiqueService.obtenir(id));
    }

    @PreAuthorize("hasRole('VENDEUR')")
    @PostMapping
    public ResponseEntity<BoutiqueResponse> creer(@AuthenticationPrincipal UserPrincipal principal,
                                                   @Valid @RequestBody BoutiqueRequest req) {
        var boutique = boutiqueService.creer(principal.getId(), req);
        return ResponseEntity.ok(BoutiqueMapper.toResponse(boutique));
    }

    @GetMapping("/{id}/produits")
    public List<ProduitResponse> produitsDeLaBoutique(@PathVariable Long id) {
        return produitService.listerParBoutique(id).stream().map(produitService::versReponse).toList();
    }

    @PreAuthorize("hasRole('VENDEUR')")
    @PostMapping("/{id}/produits")
    public ResponseEntity<ProduitResponse> ajouterProduit(@AuthenticationPrincipal UserPrincipal principal,
                                                            @PathVariable Long id,
                                                            @Valid @RequestBody ProduitRequest req) {
        var produit = produitService.creer(principal.getId(), id, req);
        return ResponseEntity.ok(ProduitMapper.toResponse(produit));
    }

    /** Réservé au vendeur propriétaire — voir BoutiqueService.televerserPhoto. */
    @PreAuthorize("hasRole('VENDEUR')")
    @PostMapping(value = "/{id}/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<BoutiqueResponse> televerserPhoto(@AuthenticationPrincipal UserPrincipal principal,
                                                              @PathVariable Long id,
                                                              @RequestParam("fichier") MultipartFile fichier) {
        var boutique = boutiqueService.televerserPhoto(principal.getId(), id, fichier);
        return ResponseEntity.ok(BoutiqueMapper.toResponse(boutique));
    }
}
