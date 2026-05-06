package fr.horizon.backend_api.service;

import fr.horizon.backend_api.model.Role;
import fr.horizon.backend_api.model.User;
import fr.horizon.backend_api.model.dto.UserDto;
import fr.horizon.backend_api.model.dto.UserRequestDto;
import fr.horizon.backend_api.repository.RoleRepository;
import fr.horizon.backend_api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Service de gestion des utilisateurs.
 * <p>
 * Fournit les opérations liées à la création, la recherche et la conversion
 * des entités User en DTO.
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Crée un nouvel utilisateur.
     * <p>
     * Étapes :
     * <ul>
     *     <li>Vérifie si l'email est déjà utilisé</li>
     *     <li>Récupère le rôle associé</li>
     *     <li>Encode le mot de passe</li>
     *     <li>Sauvegarde l'utilisateur en base</li>
     * </ul>
     *
     * @param dto données de création utilisateur
     * @return l'utilisateur créé
     * @throws RuntimeException si l'email existe déjà ou si le rôle est introuvable
     */
    public User createUser(UserRequestDto dto){
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new RuntimeException("Email already used");
        }

        Role role = roleRepository.findById(dto.getRoleId())
                .orElseThrow(() -> new RuntimeException("Role not found"));

        User user = User.builder()
                .name(dto.getName())
                .email(dto.getEmail())
                .password(passwordEncoder.encode(dto.getPassword()))
                .role(role)
                .build();

        return userRepository.save(user);
    }

    /**
     * Recherche un utilisateur par email.
     *
     * @param email email de l'utilisateur
     * @return l'utilisateur correspondant
     * @throws RuntimeException si aucun utilisateur n'est trouvé
     */
    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    /**
     * Convertit une entité User en DTO.
     *
     * @param user entité utilisateur
     * @return DTO contenant les informations publiques de l'utilisateur
     */
    public UserDto toDto(User user) {
        return UserDto.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole().getName())
                .build();
    }
}
