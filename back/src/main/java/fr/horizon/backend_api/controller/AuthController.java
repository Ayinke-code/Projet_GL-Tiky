package fr.horizon.backend_api.controller;

import fr.horizon.backend_api.service.NotificationService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import fr.horizon.backend_api.model.User;
import fr.horizon.backend_api.model.dto.LoginRequest;
import fr.horizon.backend_api.model.dto.LoginResponse;
import fr.horizon.backend_api.model.dto.UserDto;
import fr.horizon.backend_api.service.AuthService;
import fr.horizon.backend_api.service.UserService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/**
 * Contrôleur d'authentification.
 * <p>
 * Fournit les endpoints liés à la connexion, récupération de l'utilisateur courant
 * et déconnexion. Gère également la création et suppression du cookie JWT.
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final NotificationService notificationService;
    private final UserService userService;
    @Value("${jwt.expiration}")
    private long expiration;

    /**
     * Authentifie un utilisateur et génère un cookie contenant le JWT.
     *
     * @param loginRequest identifiants utilisateur (email, mot de passe)
     * @param response réponse HTTP utilisée pour ajouter le cookie
     * @return réponse HTTP 200 si authentification réussie
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @RequestBody LoginRequest loginRequest,
            HttpServletResponse response
    ) {
        LoginResponse loginResponse = authService.login(loginRequest);

        // Stock le token dans un Cookie en HTTPOnly
        ResponseCookie cookie = ResponseCookie.from("token", loginResponse.getToken())
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")                     // protection CSRF, d'ou la désactivation dans le JwtAuthentificationFilter
                .path("/")
                .maxAge(expiration / 1000)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .build();
    }

    /**
     * Retourne les informations de l'utilisateur actuellement authentifié.
     *
     * @param authentication contexte de sécurité Spring contenant l'utilisateur connecté
     * @return DTO de l'utilisateur courant
     */
    @GetMapping("/me")
    public ResponseEntity<UserDto> me(Authentication authentication) {
        String email = authentication.getName();
        User user = userService.findByEmail(email);
        return ResponseEntity.ok(userService.toDto(user));
    }

    /**
     * Déconnecte l'utilisateur en supprimant le cookie JWT.
     *
     * @param response réponse HTTP utilisée pour supprimer le cookie
     * @return réponse HTTP 200 sans contenu
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletResponse response) {

        ResponseCookie cookie = ResponseCookie.from("token", "")
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path("/")
                .maxAge(0) // supprime le cookie
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .build();
    }

}