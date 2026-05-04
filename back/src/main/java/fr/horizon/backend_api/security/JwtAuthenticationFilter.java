package fr.horizon.backend_api.security;

import fr.horizon.backend_api.service.JwtToken;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
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

@Slf4j

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtToken jwtToken;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String path = request.getServletPath();
        log.info("JwtAuthenticationFilter: path={}", path);

        // Ignorer les routes publiques (pas besoin de token)
        if (path.startsWith("/auth/login") || path.startsWith("/api/auth/")) {
            log.info("JwtAuthenticationFilter: route publique, skip");
            filterChain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader("Authorization");
        log.info("JwtAuthenticationFilter: authHeader={}", authHeader);

        try {
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                log.info("JwtAuthenticationFilter: parsing token");
                Claims claims = jwtToken.parseToken(token);
                String role = claims.get("role", String.class);
                log.info("JwtAuthenticationFilter: token parsed, role={}", role);

                // Ajouter le préfixe ROLE_ pour que Spring Security reconnaisse les autorités
                List<GrantedAuthority> authorities = List.of(
                        new SimpleGrantedAuthority("ROLE_" + role)
                );

                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(
                                claims.getSubject(),
                                null,
                                authorities
                        );

                SecurityContextHolder.getContext().setAuthentication(auth);
                log.info("JwtAuthenticationFilter: authentication set");
            } else {
                log.warn("JwtAuthenticationFilter: no Bearer token");
            }
        } catch (JwtException e) {
            log.error("JwtAuthenticationFilter: token invalid", e);
            // Si le token est invalide, effacer le contexte de sécurité
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}
