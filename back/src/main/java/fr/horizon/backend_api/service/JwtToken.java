package fr.horizon.backend_api.service;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import io.jsonwebtoken.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.security.Keys;

/**
 * Service de gestion des tokens JWT.
 * <p>
 * Encapsule la génération, la signature et la validation des tokens JWT
 * utilisés
 * pour l'authentification des utilisateurs.
 * <p>
 * Les tokens contiennent les informations essentielles de session (id
 * utilisateur,
 * email, rôles) et sont signés avec une clé secrète configurable.
 */
@Service
public class JwtToken {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expiration;

    /**
     * Génère un token JWT pour un utilisateur authentifié.
     *
     * @param email email utilisé comme subject du token
     * @param id    identifiant utilisateur stocké dans les claims
     * @param roles ensemble des rôles associés à l'utilisateur
     * @return token JWT signé
     */
    public String generateToken(String email, Long id, Set<String> roles) {

        Map<String, Object> claims = new HashMap<>();
        claims.put("id", id);
        claims.put("roles", roles);

        return createToken(claims, email);
    }

    /**
     * Construit un token JWT à partir de claims et d'un subject.
     *
     * @param claims données embarquées dans le token
     * @param email  subject (email utilisateur)
     * @return token JWT signé et encodé
     */
    private String createToken(Map<String, Object> claims, String email) {
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(email)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSignKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Génère la clé de signature HMAC à partir du secret applicatif.
     *
     * @return clé utilisée pour signer et valider les tokens
     */
    private Key getSignKey() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Parse et valide un token JWT.
     *
     * @param token token JWT à analyser
     * @return claims extraits du token
     * @throws RuntimeException si le token est expiré ou invalide
     */
    public Claims parseToken(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(getSignKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException e) {
            throw new RuntimeException("Token expiré", e);
        } catch (JwtException e) {
            throw new RuntimeException("Token invalide", e);
        }
    }
}
