package com.malimall.backend.security;

import com.malimall.backend.entity.Utilisateur;
import com.malimall.backend.repository.ChauffeurRepository;
import com.malimall.backend.repository.ClientRepository;
import com.malimall.backend.repository.UtilisateurRepository;
import com.malimall.backend.repository.VendeurRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UtilisateurRepository utilisateurRepository;
    private final ClientRepository clientRepository;
    private final VendeurRepository vendeurRepository;
    private final ChauffeurRepository chauffeurRepository;

    public CustomUserDetailsService(UtilisateurRepository utilisateurRepository,
                                     ClientRepository clientRepository,
                                     VendeurRepository vendeurRepository,
                                     ChauffeurRepository chauffeurRepository) {
        this.utilisateurRepository = utilisateurRepository;
        this.clientRepository = clientRepository;
        this.vendeurRepository = vendeurRepository;
        this.chauffeurRepository = chauffeurRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String telephone) throws UsernameNotFoundException {
        Utilisateur utilisateur = utilisateurRepository.findByTelephone(telephone)
                .orElseThrow(() -> new UsernameNotFoundException("Aucun compte pour ce numéro"));
        return new UserPrincipal(utilisateur, resoudreRoles(utilisateur), utilisateur.getMotDePasse());
    }

    public List<String> resoudreRoles(Utilisateur utilisateur) {
        List<String> roles = new ArrayList<>();
        if (clientRepository.findByUtilisateurId(utilisateur.getId()).isPresent()) {
            roles.add("ROLE_CLIENT");
        }
        if (vendeurRepository.findByUtilisateurId(utilisateur.getId()).isPresent()) {
            roles.add("ROLE_VENDEUR");
        }
        if (chauffeurRepository.findByUtilisateurId(utilisateur.getId()).isPresent()) {
            roles.add("ROLE_CHAUFFEUR");
        }
        if (utilisateur.isEstAdmin()) {
            roles.add("ROLE_ADMIN");
        }
        return roles;
    }
}
