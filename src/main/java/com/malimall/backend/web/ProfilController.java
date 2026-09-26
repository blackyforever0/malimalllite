package com.malimall.backend.web;

import com.malimall.backend.dto.ProfilDtos.DevenirChauffeurRequest;
import com.malimall.backend.security.UserPrincipal;
import com.malimall.backend.service.ProfilService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * Un même Utilisateur peut cumuler les spécialisations Client/Vendeur/
 * Chauffeur. NB : le JWT en cours ne se met pas à jour tout seul après un
 * appel ici — le nouveau rôle apparaît au prochain /api/auth/connexion,
 * limitation documentée et acceptable pour ce lot.
 */
@RestController
@RequestMapping("/api/moi")
public class ProfilController {

    private final ProfilService profilService;

    public ProfilController(ProfilService profilService) {
        this.profilService = profilService;
    }

    @PostMapping("/devenir-vendeur")
    public ResponseEntity<Map<String, Long>> devenirVendeur(@AuthenticationPrincipal UserPrincipal principal) {
        var vendeur = profilService.devenirVendeur(principal.getId());
        return ResponseEntity.ok(Map.of("vendeurId", vendeur.getId()));
    }

    @PostMapping("/devenir-chauffeur")
    public ResponseEntity<Map<String, Long>> devenirChauffeur(@AuthenticationPrincipal UserPrincipal principal,
                                                                @Valid @RequestBody DevenirChauffeurRequest req) {
        var chauffeur = profilService.devenirChauffeur(principal.getId(), req);
        return ResponseEntity.ok(Map.of("chauffeurId", chauffeur.getId()));
    }

    /** Photo de profil — accessible à tout utilisateur connecté pour sa propre fiche. */
    @PostMapping(value = "/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> televerserPhoto(@AuthenticationPrincipal UserPrincipal principal,
                                                                 @RequestParam("fichier") MultipartFile fichier) {
        var utilisateur = profilService.televerserPhoto(principal.getId(), fichier);
        return ResponseEntity.ok(Map.of("imageUrl", utilisateur.getImageUrl()));
    }
}
