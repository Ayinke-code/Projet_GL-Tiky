package fr.horizon.backend_api.security;

import fr.horizon.backend_api.service.JwtToken;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Filtre d’authentification JWT exécuté une fois par requête HTTP.
 * <p>
 * Extrait le token JWT depuis l’en-tête {@code Authorization: Bearer <token>}
 * ou depuis le cookie {@code token} si l’en-tête est absent.
 * <p>
 * Si le token est valide, le contexte de sécurité Spring est alimenté avec
 * les informations de l’utilisateur (email + rôles).
 * En cas de token invalide ou absent, la requête continue sans authentification.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtToken jwtToken;

    /**
     * Traite chaque requête HTTP entrante pour en extraire et valider le token JWT.
     * <p>
     * Ordre de priorité : en-tête {@code Authorization} &gt; cookie {@code token}.
     *
     * @param request     requête HTTP entrante
     * @param response    réponse HTTP
     * @param filterChain chaîne de filtres Spring Security
     * @throws ServletException en cas d’erreur de filtre
     * @throws IOException      en cas d’erreur d’entrée/sortie
     */
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String token = null;

        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
        }

        if (token == null && request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("token".equals(cookie.getName())) {
                    token = cookie.getValue();
                }
            }
        }

        if (token != null) {
            try {
                Claims claims = jwtToken.parseToken(token);

                List<String> roles = claims.get("roles", List.class);

                if (roles == null) {
                    roles = List.of();
                }

                List<GrantedAuthority> authorities =
                        roles.stream()
                                .map(SimpleGrantedAuthority::new)
                                .map(g -> (GrantedAuthority) g)
                                .toList();

                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(
                                claims.getSubject(),
                                null,
                                authorities
                        );
                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch (Exception e) {
                log.debug("JWT token invalide ou expiré : {}", e.getMessage());
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }
}