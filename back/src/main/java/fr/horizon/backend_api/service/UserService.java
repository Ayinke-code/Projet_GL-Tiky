package fr.horizon.backend_api.service;

import fr.horizon.backend_api.exception.EmailAlreadyUsedException;
import fr.horizon.backend_api.exception.InvalidUserRolesException;
import fr.horizon.backend_api.exception.RoleNotFoundException;
import fr.horizon.backend_api.model.Category;
import fr.horizon.backend_api.model.Role;
import fr.horizon.backend_api.model.User;
import fr.horizon.backend_api.model.dto.UserDto;
import fr.horizon.backend_api.model.dto.UserFollowerDto;
import fr.horizon.backend_api.model.dto.UserRefDto;
import fr.horizon.backend_api.model.dto.UserRequestDto;
import fr.horizon.backend_api.model.enums.IssueStatus;
import fr.horizon.backend_api.repository.CategoryRepository;
import fr.horizon.backend_api.repository.IssueRepository;
import fr.horizon.backend_api.repository.RoleRepository;
import fr.horizon.backend_api.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.ArrayList;
import java.util.HashSet;
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
    private final IssueRepository issueRepository;
    private final CategoryRepository categoryRepository;
    private final JwtToken jwtToken;

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
                .teacherType(dto.getTeacherType())
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
     * Récupère les utilisateurs ayant au moins un des rôles fournis.
     *
     * @param roles liste de noms de rôles (ex: TEACHER, STUDENT)
     * @return liste de UserRefDto
     * @throws IllegalArgumentException si un rôle est invalide
     */
    public List<UserRefDto> findAllByRoles(List<String> roles) {

        Set<String> uniqueRoles = new HashSet<>(roles);

        // Vérifie que tous les rôles existent
        List<Role> existingRoles = roleRepository.findByNameIn(new ArrayList<>(uniqueRoles));

        if (existingRoles.size() != roles.size()) {
            throw new IllegalArgumentException("Un ou plusieurs rôles sont invalides");
        }

        return userRepository.findDistinctByRoles_NameIn(roles)
                .stream()
                .map(this::toUserRefDto)
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
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
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
        boolean hasOpenIssues =
                issueRepository.existsByCreatedByIdAndStatus(
                        user.getId(),
                        IssueStatus.OUVERT
                );

        String wsToken = jwtToken.generateToken(
                user.getEmail(),
                user.getId(),
                user.getRoles()
                        .stream()
                        .map(Role::getName)
                        .collect(Collectors.toSet())
        );

        return UserDto.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .roles(
                        user.getRoles()
                                .stream()
                                .map(Role::getName)
                                .collect(Collectors.toSet())
                )
                .isContributor(hasOpenIssues)
                .teacherType(user.getTeacherType())
                .wsToken(wsToken)
                .build();
    }

    /**
     * Convertit une entité User en UserRefDto.
     *
     * @param user entité utilisateur
     * @return DTO simplifié de l'utilisateur
     */
    public UserRefDto toUserRefDto(User user) {
        return UserRefDto.builder()
                .id(user.getId())
                .name(user.getName())
                .roles(
                        user.getRoles()
                                .stream()
                                .map(Role::getName)
                                .collect(Collectors.toSet())
                )
                .teacherType(user.getTeacherType())
                .build();
    }

    private UserFollowerDto toUserFollowerDto(User user){
        return UserFollowerDto.builder()
                .id(user.getId())
                .name(user.getName())
                .build();
    }


    public List<UserFollowerDto> getAllTeachersWithCategoryDetail(Long categoryId){
        Role teacherRole = roleRepository.findByName("TEACHER")
                .orElseThrow(() -> new EntityNotFoundException("Rôle TEACHER non trouvé en base"));

        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new EntityNotFoundException("Catégorie introuvable"));

        List<User> teachers = userRepository.getAllByRoles(Set.of(teacherRole));

        // Récupère tous les followers de la catégorie
        Set<User> categoryFollowers = category.getFollowers();

        return teachers.stream()
                .map(teacher -> {
                    UserFollowerDto dto = this.toUserFollowerDto(teacher);

                    // Vérifie si ce professeur est dans le Set des followers et met à jour le booléen
                    dto.setFollower(categoryFollowers.contains(teacher));

                    return dto;
                })
                .toList();
    }
}
