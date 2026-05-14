package fr.horizon.backend_api.service;

import fr.horizon.backend_api.exception.EmailAlreadyUsedException;
import fr.horizon.backend_api.exception.InvalidUserRolesException;
import fr.horizon.backend_api.exception.RoleNotFoundException;
import fr.horizon.backend_api.model.Role;
import fr.horizon.backend_api.model.User;
import fr.horizon.backend_api.model.dto.UserDto;
import fr.horizon.backend_api.model.dto.UserRequestDto;
import fr.horizon.backend_api.repository.RoleRepository;
import fr.horizon.backend_api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service de gestion des utilisateurs.
 * <p>
 * Fournit les opérations métier liées aux utilisateurs :
 * création, récupération, suppression et conversion en DTO.
 * <p>
 * Encapsule la logique de persistance et les règles de base
 * (unicité email, encodage du mot de passe, gestion des rôles).
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Crée et persiste un nouvel utilisateur.
     *
     * @param dto données de création utilisateur
     * @return utilisateur créé sous forme de DTO
     * @throws RuntimeException si l'email est déjà utilisé
     */
    public UserDto createUser(UserRequestDto dto) {

        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new EmailAlreadyUsedException(dto.getEmail());
        }

        if (dto.getRoleIds() == null || dto.getRoleIds().isEmpty()) {
            throw new InvalidUserRolesException("User must have at least one role");
        }

        Set<Role> roles = roleRepository.findAllById(dto.getRoleIds())
                .stream()
                .collect(Collectors.toSet());

        if (roles.size() != dto.getRoleIds().size()) {
            throw new RoleNotFoundException();
        }

        User user = User.builder()
                .name(dto.getName())
                .email(dto.getEmail())
                .password(passwordEncoder.encode(dto.getPassword()))
                .roles(roles)
                .build();

        userRepository.save(user);

        return toDto(user);
    }

    /**
     * Retourne la liste de tous les utilisateurs.
     *
     * @return liste de DTO utilisateurs
     */
    public List<UserDto> findAll() {
        return userRepository.findAll()
                .stream()
                .map(this::toDto)
                .toList();
    }

    /**
     * Recherche un utilisateur par email.
     *
     * @param email email de l'utilisateur
     * @return entité utilisateur
     * @throws RuntimeException si aucun utilisateur n'est trouvé
     */
    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    /**
     * Supprime un utilisateur par son identifiant.
     *
     * @param id identifiant utilisateur
     */
    public void deleteById(Long id) {
        userRepository.deleteById(id);
    }

    /**
     * Convertit une entité User en DTO.
     *
     * @param user entité utilisateur
     * @return DTO utilisateur
     */
    public UserDto toDto(User user) {
        return UserDto.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getDisplayRole())
                .roles(user.getRoleNames())
                .build();
    }
}