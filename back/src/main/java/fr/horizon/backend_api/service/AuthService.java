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

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtToken jwtToken;

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
