package com.malimall.backend.web;

import com.malimall.backend.dto.AdminDtos.AdminBoutiqueDetailResponse;
import com.malimall.backend.dto.AdminDtos.AdminBoutiqueResponse;
import com.malimall.backend.dto.AdminDtos.AdminChauffeurDetailResponse;
import com.malimall.backend.dto.AdminDtos.AdminCommandeDetailResponse;
import com.malimall.backend.dto.AdminDtos.AdminCommandeResponse;
import com.malimall.backend.dto.AdminDtos.MotoTaxisResponse;
import com.malimall.backend.dto.AdminDtos.ResoudreLitigeRequest;
import com.malimall.backend.dto.AdminDtos.RevenusResponse;
import com.malimall.backend.dto.AdminDtos.AdminUtilisateurResponse;
import com.malimall.backend.dto.AdminDtos.VueEnsembleResponse;
import com.malimall.backend.dto.PortefeuilleDtos.RechargeRequest;
import com.malimall.backend.dto.PubliciteDtos.PubliciteResponse;
import com.malimall.backend.mapper.PubliciteMapper;
import com.malimall.backend.service.AdminReportingService;
import com.malimall.backend.service.AdminService;
import com.malimall.backend.service.PubliciteService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Réservé à ROLE_ADMIN. Recharge manuelle de portefeuille, validation des
 * demandes de publicité (voir PubliciteService), et toute la console
 * d'administration web (Angular) : vue d'ensemble, boutiques, commandes
 * Market + litiges, MotoTaxis, revenus (voir AdminReportingService).
 */
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;
    private final AdminReportingService adminReportingService;
    private final PubliciteService publiciteService;

    public AdminController(AdminService adminService,
                            AdminReportingService adminReportingService,
                            PubliciteService publiciteService) {
        this.adminService = adminService;
        this.adminReportingService = adminReportingService;
        this.publiciteService = publiciteService;
    }

    @PostMapping("/portefeuilles/{utilisateurId}/recharger")
    public ResponseEntity<Void> recharger(@PathVariable Long utilisateurId, @Valid @RequestBody RechargeRequest req) {
        adminService.rechargerPortefeuille(utilisateurId, req.montantMmc());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/portefeuilles/{utilisateurId}/debiter")
    public ResponseEntity<Void> debiter(@PathVariable Long utilisateurId, @Valid @RequestBody RechargeRequest req) {
        adminService.debiterPortefeuille(utilisateurId, req.montantMmc());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/portefeuilles/{utilisateurId}/geler")
    public ResponseEntity<Void> geler(@PathVariable Long utilisateurId, @Valid @RequestBody RechargeRequest req) {
        adminService.gelerPortefeuille(utilisateurId, req.montantMmc());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/portefeuilles/{utilisateurId}/debloquer")
    public ResponseEntity<Void> debloquer(@PathVariable Long utilisateurId, @Valid @RequestBody RechargeRequest req) {
        adminService.debloquerPortefeuille(utilisateurId, req.montantMmc());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/publicites")
    public List<PubliciteResponse> publicitesEnAttente() {
        return publiciteService.enAttente().stream().map(PubliciteMapper::toResponse).toList();
    }

    @PostMapping("/publicites/{id}/valider")
    public ResponseEntity<PubliciteResponse> validerPublicite(@PathVariable Long id) {
        return ResponseEntity.ok(PubliciteMapper.toResponse(publiciteService.valider(id)));
    }

    @PostMapping("/publicites/{id}/refuser")
    public ResponseEntity<PubliciteResponse> refuserPublicite(@PathVariable Long id) {
        return ResponseEntity.ok(PubliciteMapper.toResponse(publiciteService.refuser(id)));
    }

    // ------------------------------------------------------------------
    // Vue d'ensemble
    // ------------------------------------------------------------------

    @GetMapping("/vue-ensemble")
    public VueEnsembleResponse vueEnsemble() {
        return adminReportingService.vueEnsemble();
    }

    // ------------------------------------------------------------------
    // Utilisateurs
    // ------------------------------------------------------------------

    @GetMapping("/utilisateurs")
    public List<AdminUtilisateurResponse> utilisateurs() {
        return adminReportingService.utilisateurs();
    }

    @PostMapping("/utilisateurs/{id}/suspendre")
    public ResponseEntity<Void> suspendreUtilisateur(@PathVariable Long id) {
        adminReportingService.suspendreUtilisateur(id, true);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/utilisateurs/{id}/reactiver")
    public ResponseEntity<Void> reactiverUtilisateur(@PathVariable Long id) {
        adminReportingService.suspendreUtilisateur(id, false);
        return ResponseEntity.ok().build();
    }

    // ------------------------------------------------------------------
    // Boutiques & produits
    // ------------------------------------------------------------------

    @GetMapping("/boutiques")
    public List<AdminBoutiqueResponse> boutiques() {
        return adminReportingService.boutiques();
    }

    @GetMapping("/boutiques/{id}")
    public AdminBoutiqueDetailResponse boutiqueDetail(@PathVariable Long id) {
        return adminReportingService.boutiqueDetail(id);
    }

    @PostMapping("/boutiques/{id}/suspendre")
    public ResponseEntity<Void> suspendreBoutique(@PathVariable Long id) {
        adminReportingService.suspendreBoutique(id, true);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/boutiques/{id}/reactiver")
    public ResponseEntity<Void> reactiverBoutique(@PathVariable Long id) {
        adminReportingService.suspendreBoutique(id, false);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/boutiques/{id}/certifier")
    public ResponseEntity<Void> certifierBoutique(@PathVariable Long id) {
        adminReportingService.certifierBoutique(id, true);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/boutiques/{id}/decertifier")
    public ResponseEntity<Void> decertifierBoutique(@PathVariable Long id) {
        adminReportingService.certifierBoutique(id, false);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/produits/{id}/activer")
    public ResponseEntity<Void> activerProduit(@PathVariable Long id) {
        adminReportingService.activerProduit(id, true);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/produits/{id}/desactiver")
    public ResponseEntity<Void> desactiverProduit(@PathVariable Long id) {
        adminReportingService.activerProduit(id, false);
        return ResponseEntity.ok().build();
    }

    // ------------------------------------------------------------------
    // Commandes Market & litiges
    // ------------------------------------------------------------------

    @GetMapping("/commandes")
    public List<AdminCommandeResponse> commandes() {
        return adminReportingService.commandes();
    }

    @GetMapping("/commandes/{id}")
    public AdminCommandeDetailResponse commandeDetail(@PathVariable Long id) {
        return adminReportingService.commandeDetail(id);
    }

    @PostMapping("/commandes/{id}/resoudre-litige")
    public ResponseEntity<Void> resoudreLitige(@PathVariable Long id, @Valid @RequestBody ResoudreLitigeRequest req) {
        adminReportingService.resoudreLitige(id, req.action());
        return ResponseEntity.ok().build();
    }

    // ------------------------------------------------------------------
    // MotoTaxis
    // ------------------------------------------------------------------

    @GetMapping("/mototaxis")
    public MotoTaxisResponse motoTaxis() {
        return adminReportingService.motoTaxis();
    }

    @GetMapping("/mototaxis/chauffeurs/{id}")
    public AdminChauffeurDetailResponse chauffeurDetail(@PathVariable Long id) {
        return adminReportingService.chauffeurDetail(id);
    }

    @PostMapping("/mototaxis/chauffeurs/{id}/suspendre")
    public ResponseEntity<Void> suspendreChauffeur(@PathVariable Long id) {
        adminReportingService.suspendreChauffeur(id, true);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/mototaxis/chauffeurs/{id}/reactiver")
    public ResponseEntity<Void> reactiverChauffeur(@PathVariable Long id) {
        adminReportingService.suspendreChauffeur(id, false);
        return ResponseEntity.ok().build();
    }

    // ------------------------------------------------------------------
    // Revenus
    // ------------------------------------------------------------------

    @GetMapping("/revenus")
    public RevenusResponse revenus() {
        return adminReportingService.revenus();
    }
}
