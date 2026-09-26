package com.malimall.backend.web;

import com.malimall.backend.dto.CommandeDtos.CommandeResponse;
import com.malimall.backend.dto.PanierDtos.AjouterLigneRequest;
import com.malimall.backend.dto.PanierDtos.PanierResponse;
import com.malimall.backend.dto.PanierDtos.ValiderPanierRequest;
import com.malimall.backend.security.UserPrincipal;
import com.malimall.backend.service.PanierService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Contrôleur volontairement mince : tout le mapping Entité -&gt; DTO est fait
 * dans PanierService, à l'intérieur des transactions (voir la javadoc de
 * PanierService sur open-in-view=false).
 */
@RestController
@RequestMapping("/api/panier")
public class PanierController {

    private final PanierService panierService;

    public PanierController(PanierService panierService) {
        this.panierService = panierService;
    }

    @GetMapping
    public PanierResponse voir(@AuthenticationPrincipal UserPrincipal principal) {
        return panierService.obtenirPanier(principal.getId());
    }

    @PostMapping("/lignes")
    public PanierResponse ajouter(@AuthenticationPrincipal UserPrincipal principal,
                                   @Valid @RequestBody AjouterLigneRequest req) {
        return panierService.ajouterLigne(principal.getId(), req.produitId(), req.quantite());
    }

    @DeleteMapping("/lignes/{ligneId}")
    public PanierResponse retirer(@AuthenticationPrincipal UserPrincipal principal, @PathVariable Long ligneId) {
        return panierService.retirerLigne(principal.getId(), ligneId);
    }

    /** Checkout : peut créer plusieurs Commandes (une par boutique représentée dans le panier). */
    @PostMapping("/valider")
    public ResponseEntity<List<CommandeResponse>> valider(@AuthenticationPrincipal UserPrincipal principal,
                                                          @Valid @RequestBody(required = false) ValiderPanierRequest req) {
        if (req == null) {
            return ResponseEntity.ok(panierService.validerPanier(principal.getId(), null));
        }
        return ResponseEntity.ok(panierService.validerPanier(
                principal.getId(), req.adresseLivraison(), req.latitude(), req.longitude()));
    }
}
