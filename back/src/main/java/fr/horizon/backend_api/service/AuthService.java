package fr.horizon.backend_api.service;

import fr.horizon.backend_api.model.Role;
import fr.horizon.backend_api.model.User;
import fr.horizon.backend_api.model.dto.LoginRequest;
import fr.horizon.backend_api.model.dto.LoginResponse;
import fr.horizon.backend_api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service d'authentification des utilisateurs.
 * <p>
 * Gère la vérification des identifiants (email / mot de passe)
 * et la génération du token JWT lors d'une authentification réussie.
 * <p>
 * Expose uniquement la logique métier, sans gestion HTTP directe.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtToken jwtToken;

    /**
     * Authentifie un utilisateur à partir de ses identifiants.
     *
     * <p>Processus :
     * <ul>
     *     <li>Recherche de l'utilisateur par email</li>
     *     <li>Vérification du mot de passe</li>
     *     <li>Extraction des rôles utilisateur</li>
     *     <li>Génération d'un token JWT</li>
     *     <li>Retour des informations utilisateur + token</li>
     * </ul>
     *
     * @param loginRequest identifiants de connexion (email, password)
     * @return réponse contenant les informations utilisateur et le JWT
     * @throws ResponseStatusException si les identifiants sont invalides
     */
    public LoginResponse login(LoginRequest loginRequest) {
        String email = loginRequest.getEmail();
        String password = loginRequest.getPassword();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Email ou mot de passe incorrect"
                ));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Email ou mot de passe incorrect"
            );
        }

        Set<String> roles = user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toSet());

        return new LoginResponse(
                user.getId(),
                user.getName(),
                jwtToken.generateToken(
                        user.getEmail(),
                        user.getId(),
                        roles
                ),
                user.getDisplayRole(),
                user.getRoleNames(),
                user.getEmail()
        );
    }
}