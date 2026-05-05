package fr.horizon.backend_api.service;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import io.jsonwebtoken.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.security.Keys;

/**
 * Service de génération et de validation des tokens JWT.
 * <p>
 * Responsable de :
 * <ul>
 *     <li>La création de tokens JWT contenant les informations utilisateur</li>
 *     <li>La signature des tokens avec une clé secrète</li>
 *     <li>La validation et l'extraction des claims depuis un token</li>
 * </ul>
 */
@Service
public class JwtToken {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expiration;

    /**
     * Génère un token JWT pour un utilisateur.
     *
     * @param email email utilisé comme subject du token
     * @param id identifiant utilisateur ajouté dans les claims
     * @param role rôle utilisateur ajouté dans les claims
     * @return token JWT signé
     */
    public String generateToken(String email, Long id, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("id", id);
        claims.put("role", role);
        return createToken(claims, email);
    }

    /**
     * Construit le token JWT avec les claims et le sujet.
     *
     * @param claims données supplémentaires du token
     * @param email sujet (email utilisateur)
     * @return token JWT signé et compacté
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
     * Génère la clé de signature HMAC à partir du secret configuré.
     *
     * @return clé de signature utilisée pour JWT
     */
    private Key getSignKey() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Analyse et valide un token JWT.
     *
     * @param token token JWT à parser
     * @return claims contenus dans le token
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
