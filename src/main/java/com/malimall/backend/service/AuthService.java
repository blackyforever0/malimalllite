package com.malimall.backend.service;

import com.malimall.backend.dto.AuthDtos.AuthResponse;
import com.malimall.backend.dto.AuthDtos.ConnexionRequest;
import com.malimall.backend.dto.AuthDtos.InscriptionRequest;
import com.malimall.backend.entity.Client;
import com.malimall.backend.entity.Utilisateur;
import com.malimall.backend.entity.enums.StatutVerification;
import com.malimall.backend.repository.ClientRepository;
import com.malimall.backend.repository.UtilisateurRepository;
import com.malimall.backend.security.CustomUserDetailsService;
import com.malimall.backend.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AuthService {

    private final UtilisateurRepository utilisateurRepository;
    private final ClientRepository clientRepository;
    private final PortefeuilleService portefeuilleService;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final CustomUserDetailsService userDetailsService;
    private final JwtService jwtService;

    public AuthService(UtilisateurRepository utilisateurRepository,
                        ClientRepository clientRepository,
                        PortefeuilleService portefeuilleService,
                        PasswordEncoder passwordEncoder,
                        AuthenticationManager authenticationManager,
                        CustomUserDetailsService userDetailsService,
                        JwtService jwtService) {
        this.utilisateurRepository = utilisateurRepository;
        this.clientRepository = clientRepository;
        this.portefeuilleService = portefeuilleService;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
        this.jwtService = jwtService;
    }

    /**
     * inscrire() : crée l'Utilisateur, son Portefeuille et sa spécialisation
     * Client automatiquement — la capacité d'achat est le socle commun de
     * tout compte (cf. classe_v4.drawio).
     */
    @Transactional
    public AuthResponse inscrire(InscriptionRequest req) {
        if (utilisateurRepository.existsByTelephone(req.telephone())) {
            throw new IllegalArgumentException("Ce numéro de téléphone est déjà utilisé");
        }

        Utilisateur utilisateur = Utilisateur.builder()
                .nom(req.nom())
                .prenom(req.prenom())
                .telephone(req.telephone())
                .email(req.email())
                .motDePasse(passwordEncoder.encode(req.motDePasse()))
                .statutVerification(StatutVerification.NON_VERIFIE)
                .estAdmin(false)
                .build();
        utilisateur = utilisateurRepository.save(utilisateur);

        portefeuilleService.creerPour(utilisateur);

        Client client = Client.builder().utilisateur(utilisateur).build();
        clientRepository.save(client);

        List<String> roles = List.of("ROLE_CLIENT");
        String token = jwtService.genererToken(utilisateur.getId(), utilisateur.getTelephone(), roles);
        return new AuthResponse(token, utilisateur.getId(), roles);
    }

    public AuthResponse connecter(ConnexionRequest req) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(req.telephone(), req.motDePasse()));
        } catch (DisabledException e) {
            throw new DisabledException("Ce compte a été suspendu par l'administration MaliMall.");
        } catch (Exception e) {
            throw new BadCredentialsException("Téléphone ou mot de passe incorrect");
        }

        Utilisateur utilisateur = utilisateurRepository.findByTelephone(req.telephone())
                .orElseThrow(() -> new BadCredentialsException("Téléphone ou mot de passe incorrect"));
        List<String> roles = userDetailsService.resoudreRoles(utilisateur);
        String token = jwtService.genererToken(utilisateur.getId(), utilisateur.getTelephone(), roles);
        return new AuthResponse(token, utilisateur.getId(), roles);
    }
}
