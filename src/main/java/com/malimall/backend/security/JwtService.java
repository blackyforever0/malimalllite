package com.malimall.backend.security;

import com.malimall.backend.config.MaliMallProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;

@Service
public class JwtService {

    private final SecretKey key;
    private final long expirationMinutes;

    public JwtService(MaliMallProperties properties) {
        this.key = Keys.hmacShaKeyFor(properties.jwt().secret().getBytes(StandardCharsets.UTF_8));
        this.expirationMinutes = properties.jwt().expirationMinutes();
    }

    public String genererToken(Long utilisateurId, String telephone, List<String> roles) {
        Instant maintenant = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(utilisateurId))
                .claim("telephone", telephone)
                .claim("roles", roles)
                .issuedAt(Date.from(maintenant))
                .expiration(Date.from(maintenant.plusSeconds(expirationMinutes * 60)))
                .signWith(key)
                .compact();
    }

    public Claims extraireClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public Long extraireUtilisateurId(String token) {
        return Long.valueOf(extraireClaims(token).getSubject());
    }

    @SuppressWarnings("unchecked")
    public List<String> extraireRoles(String token) {
        return (List<String>) extraireClaims(token).get("roles", List.class);
    }

    public boolean estValide(String token) {
        try {
            Date expiration = extraireClaims(token).getExpiration();
            return expiration.after(new Date());
        } catch (Exception e) {
            return false;
        }
    }
}
