package fr.horizon.backend_api.service;

import fr.horizon.backend_api.model.User;
import fr.horizon.backend_api.model.dto.LoginRequest;
import fr.horizon.backend_api.model.dto.LoginResponse;
import fr.horizon.backend_api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Service responsable de l'authentification des utilisateurs.
 * <p>
 * Il gère la vérification des identifiants (email / mot de passe)
 * et la génération du token JWT en cas de succès.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtToken jwtToken;

    /**
     * Authentifie un utilisateur à partir de ses identifiants.
     * <p>
     * Étapes :
     * <ul>
     *     <li>Recherche de l'utilisateur par email</li>
     *     <li>Vérification du mot de passe</li>
     *     <li>Génération d'un token JWT si authentification réussie</li>
     * </ul>
     *
     * @param loginRequest contient l'email et le mot de passe saisis
     * @return une réponse contenant les informations utilisateur et le token JWT
     * @throws ResponseStatusException si l'email n'existe pas ou si le mot de passe est incorrect
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

        return new LoginResponse(
                user.getId(),
                user.getName(),
                jwtToken.generateToken(user.getEmail(), user.getId(), user.getRole().getName()),
                user.getRole().getName(),
                user.getEmail()
        );
    }

}
