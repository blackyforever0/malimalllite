package com.malimall.backend.web;

import com.malimall.backend.dto.LivreurDtos.DisponibiliteRequest;
import com.malimall.backend.dto.LivreurDtos.EspaceLivreurResponse;
import com.malimall.backend.dto.LivreurDtos.PositionRequest;
import com.malimall.backend.security.UserPrincipal;
import com.malimall.backend.service.EspaceLivreurService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/moi/chauffeur")
@PreAuthorize("hasRole('CHAUFFEUR')")
public class EspaceLivreurController {

    private final EspaceLivreurService espaceLivreurService;

    public EspaceLivreurController(EspaceLivreurService espaceLivreurService) {
        this.espaceLivreurService = espaceLivreurService;
    }

    @GetMapping
    public EspaceLivreurResponse resume(@AuthenticationPrincipal UserPrincipal principal) {
        return espaceLivreurService.resume(principal.getId());
    }

    @PatchMapping("/disponibilite")
    public EspaceLivreurResponse disponibilite(@AuthenticationPrincipal UserPrincipal principal,
                                               @RequestBody DisponibiliteRequest req) {
        return espaceLivreurService.changerDisponibilite(principal.getId(), req.disponible());
    }

    @PutMapping("/position")
    public ResponseEntity<Void> position(@AuthenticationPrincipal UserPrincipal principal,
                                         @RequestBody PositionRequest req) {
        espaceLivreurService.mettreAJourPosition(principal.getId(), req.latitude(), req.longitude());
        return ResponseEntity.noContent().build();
    }
}
