package com.malimall.backend.web;

import com.malimall.backend.dto.CommandeDtos.CommandeResponse;
import com.malimall.backend.dto.CommandeDtos.NoterLivreurRequest;
import com.malimall.backend.dto.CommandeDtos.ScannerCodeRequest;
import com.malimall.backend.dto.CommandeDtos.SignalerRequest;
import com.malimall.backend.security.UserPrincipal;
import com.malimall.backend.service.CommandeService;
import com.malimall.backend.service.LivraisonService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Contrôleur mince : le mapping Entité -&gt; DTO vit dans CommandeService /
 * LivraisonService (voir leur javadoc sur open-in-view=false).
 */
@RestController
@RequestMapping("/api/commandes")
public class CommandeController {

    private final CommandeService commandeService;
    private final LivraisonService livraisonService;

    public CommandeController(CommandeService commandeService, LivraisonService livraisonService) {
        this.commandeService = commandeService;
        this.livraisonService = livraisonService;
    }

    @GetMapping("/{id}")
    public CommandeResponse obtenir(@AuthenticationPrincipal UserPrincipal principal, @PathVariable Long id) {
        return commandeService.obtenir(id, principal.getId(), principal.hasRole("ROLE_ADMIN"));
    }

    /** role=acheteur (défaut) | vendeur | chauffeur — filtre les commandes qui concernent l'appelant sous ce rôle. */
    @GetMapping
    public List<CommandeResponse> mesCommandes(@AuthenticationPrincipal UserPrincipal principal,
                                                @RequestParam(defaultValue = "acheteur") String role) {
        boolean admin = principal.hasRole("ROLE_ADMIN");
        return switch (role) {
            case "vendeur" -> commandeService.mesCommandesVendeur(principal.getId(), admin);
            case "chauffeur" -> commandeService.mesCommandesChauffeur(principal.getId(), admin);
            default -> commandeService.mesCommandesAcheteur(principal.getId(), admin);
        };
    }

    /** Livraisons pas encore prises en charge — permet à un chauffeur de les parcourir avant de s'auto-assigner. */
    @PreAuthorize("hasRole('CHAUFFEUR')")
    @GetMapping("/disponibles")
    public List<CommandeResponse> disponibles(@AuthenticationPrincipal UserPrincipal principal) {
        return commandeService.commandesDisponibles(principal.getId());
    }

    @PreAuthorize("hasRole('CHAUFFEUR')")
    @PostMapping("/{id}/assigner-chauffeur")
    public CommandeResponse assignerChauffeur(@AuthenticationPrincipal UserPrincipal principal, @PathVariable Long id) {
        return livraisonService.assignerChauffeur(id, principal.getId());
    }

    @PreAuthorize("hasRole('CHAUFFEUR')")
    @PostMapping("/{id}/scanner-retrait")
    public CommandeResponse scannerRetrait(@AuthenticationPrincipal UserPrincipal principal,
                                            @PathVariable Long id,
                                            @Valid @RequestBody ScannerCodeRequest req) {
        return livraisonService.scannerCodeRetrait(id, principal.getId(), req.code());
    }

    @PostMapping("/{id}/noter-livreur")
    public CommandeResponse noterLivreur(@AuthenticationPrincipal UserPrincipal principal,
                                         @PathVariable Long id,
                                         @Valid @RequestBody NoterLivreurRequest req) {
        return livraisonService.noterLivreur(id, principal.getId(), req.note());
    }

    @PostMapping("/{id}/signaler")
    public CommandeResponse signaler(@AuthenticationPrincipal UserPrincipal principal,
                                     @PathVariable Long id,
                                     @Valid @RequestBody SignalerRequest req) {
        return livraisonService.signaler(id, principal.getId(), req.motif(), req.description());
    }

    @PostMapping("/{id}/confirmer-livraison")
    public CommandeResponse confirmerLivraison(@AuthenticationPrincipal UserPrincipal principal,
                                                @PathVariable Long id,
                                                @Valid @RequestBody ScannerCodeRequest req) {
        return livraisonService.confirmerLivraison(id, principal.getId(), req.code());
    }

    /**
     * Retrait sur place (Commande.retraitParClient) : le vendeur valide le
     * code que le client lui présente en venant chercher sa commande.
     */
    @PreAuthorize("hasRole('VENDEUR')")
    @PostMapping("/{id}/confirmer-retrait-client")
    public CommandeResponse confirmerRetraitClient(@AuthenticationPrincipal UserPrincipal principal,
                                                    @PathVariable Long id,
                                                    @Valid @RequestBody ScannerCodeRequest req) {
        return livraisonService.confirmerRetraitClient(id, principal.getId(), req.code());
    }
}
