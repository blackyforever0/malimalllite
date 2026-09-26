package com.malimall.backend.web;

import com.malimall.backend.dto.PubliciteDtos.CreerPubliciteRequest;
import com.malimall.backend.dto.PubliciteDtos.PubliciteResponse;
import com.malimall.backend.mapper.PubliciteMapper;
import com.malimall.backend.security.UserPrincipal;
import com.malimall.backend.service.PubliciteService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/publicites")
public class PubliciteController {

    private final PubliciteService publiciteService;

    public PubliciteController(PubliciteService publiciteService) {
        this.publiciteService = publiciteService;
    }

    /** Bannières actives à afficher sur l'accueil Market — tout utilisateur connecté. */
    @GetMapping("/actives")
    public List<PubliciteResponse> actives() {
        return publiciteService.actives().stream().map(PubliciteMapper::toResponse).toList();
    }

    @PreAuthorize("hasRole('VENDEUR')")
    @GetMapping("/mes")
    public List<PubliciteResponse> mesPublicites(@AuthenticationPrincipal UserPrincipal principal) {
        return publiciteService.mesPublicites(principal.getId()).stream().map(PubliciteMapper::toResponse).toList();
    }

    /** Débite immédiatement le vendeur (voir PubliciteService) — 422 si solde insuffisant. */
    @PreAuthorize("hasRole('VENDEUR')")
    @PostMapping
    public ResponseEntity<PubliciteResponse> demander(@AuthenticationPrincipal UserPrincipal principal,
                                                        @Valid @RequestBody CreerPubliciteRequest req) {
        var publicite = publiciteService.demander(principal.getId(), req);
        return ResponseEntity.ok(PubliciteMapper.toResponse(publicite));
    }

    /** Réservé au vendeur propriétaire — voir PubliciteService.televerserPhoto. */
    @PreAuthorize("hasRole('VENDEUR')")
    @PostMapping(value = "/{id}/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PubliciteResponse> televerserPhoto(@AuthenticationPrincipal UserPrincipal principal,
                                                               @PathVariable Long id,
                                                               @RequestParam("fichier") MultipartFile fichier) {
        var publicite = publiciteService.televerserPhoto(principal.getId(), id, fichier);
        return ResponseEntity.ok(PubliciteMapper.toResponse(publicite));
    }
}
