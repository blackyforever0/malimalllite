package com.malimall.backend.security;

import com.malimall.backend.entity.Utilisateur;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Un utilisateur peut cumuler plusieurs rôles (Client/Vendeur/Chauffeur/Admin
 * sont des spécialisations additives, pas un choix exclusif) — les autorités
 * sont donc calculées à la connexion en fonction des spécialisations
 * réellement présentes, jamais un rôle unique figé dans le token.
 */
public class UserPrincipal implements UserDetails {

    private final Long id;
    private final String telephone;
    private final String motDePasseHache;
    private final List<String> roles;
    private final boolean suspendu;

    public UserPrincipal(Utilisateur utilisateur, List<String> roles, String motDePasseHache) {
        this.id = utilisateur.getId();
        this.telephone = utilisateur.getTelephone();
        this.motDePasseHache = motDePasseHache;
        this.roles = roles;
        this.suspendu = utilisateur.isSuspendu();
    }

    public Long getId() {
        return id;
    }

    public List<String> getRoles() {
        return roles;
    }

    public boolean hasRole(String role) {
        return roles.contains(role);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles.stream().map(SimpleGrantedAuthority::new).toList();
    }

    @Override
    public String getPassword() {
        return motDePasseHache;
    }

    @Override
    public String getUsername() {
        return telephone;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return !suspendu;
    }
}
