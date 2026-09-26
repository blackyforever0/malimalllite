package com.malimall.backend.web;

import com.malimall.backend.dto.AuthDtos.*;
import com.malimall.backend.entity.Utilisateur;
import com.malimall.backend.entity.enums.StatutVerification;
import com.malimall.backend.exception.RessourceIntrouvableException;
import com.malimall.backend.repository.ChauffeurRepository;
import com.malimall.backend.repository.UtilisateurRepository;
import com.malimall.backend.repository.VendeurRepository;
import com.malimall.backend.security.CustomUserDetailsService;
import com.malimall.backend.security.UserPrincipal;
import com.malimall.backend.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class AuthController {

    private final AuthService authService;
    private final UtilisateurRepository utilisateurRepository;
    private final CustomUserDetailsService userDetailsService;
    private final VendeurRepository vendeurRepository;
    private final ChauffeurRepository chauffeurRepository;

    public AuthController(AuthService authService,
                           UtilisateurRepository utilisateurRepository,
                           CustomUserDetailsService userDetailsService,
                           VendeurRepository vendeurRepository,
                           ChauffeurRepository chauffeurRepository) {
        this.authService = authService;
        this.utilisateurRepository = utilisateurRepository;
        this.userDetailsService = userDetailsService;
        this.vendeurRepository = vendeurRepository;
        this.chauffeurRepository = chauffeurRepository;
    }

    @PostMapping("/auth/inscription")
    public ResponseEntity<AuthResponse> inscription(@Valid @RequestBody InscriptionRequest req) {
        return ResponseEntity.ok(authService.inscrire(req));
    }

    @PostMapping("/auth/connexion")
    public ResponseEntity<AuthResponse> connexion(@Valid @RequestBody ConnexionRequest req) {
        return ResponseEntity.ok(authService.connecter(req));
    }

    @GetMapping("/moi")
    public ResponseEntity<MoiResponse> moi(@AuthenticationPrincipal UserPrincipal principal) {
        Utilisateur u = utilisateurRepository.findById(principal.getId())
                .orElseThrow(() -> new RessourceIntrouvableException("Utilisateur introuvable"));
        Long vendeurId = vendeurRepository.findByUtilisateurId(u.getId()).map(v -> v.getId()).orElse(null);
        Long chauffeurId = chauffeurRepository.findByUtilisateurId(u.getId()).map(c -> c.getId()).orElse(null);
        return ResponseEntity.ok(new MoiResponse(
                u.getId(), u.getNom(), u.getPrenom(), u.getTelephone(), u.getEmail(),
                userDetailsService.resoudreRoles(u), vendeurId, chauffeurId,
                u.getImageUrl(), u.getStatutVerification() == StatutVerification.VERIFIE, u.getDateCreation()));
    }
}
