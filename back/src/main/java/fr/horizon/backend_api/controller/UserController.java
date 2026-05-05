package fr.horizon.backend_api.controller;

import java.util.List;

import fr.horizon.backend_api.model.dto.UserRequestDto;
import fr.horizon.backend_api.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import fr.horizon.backend_api.model.User;
import fr.horizon.backend_api.repository.UserRepository;

/**
 * Contrôleur de gestion des utilisateurs.
 * <p>
 * Expose les endpoints CRUD permettant de récupérer, créer et supprimer des utilisateurs.
 * Certaines opérations délèguent la logique métier au {@link UserService}.
 */
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;
    private final UserService userService;

    /**
     * Récupère la liste de tous les utilisateurs.
     *
     * @return liste complète des utilisateurs en base
     */
    @GetMapping
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    /**
     * Crée un nouvel utilisateur à partir d'un DTO validé.
     *
     * @param dto données de création utilisateur
     * @return l'utilisateur créé
     */
    @PostMapping("/add")
    public User createUser(@Valid @RequestBody UserRequestDto dto) {
        return userService.createUser(dto);
    }

    /**
     * Supprime un utilisateur à partir de son identifiant.
     *
     * @param id identifiant de l'utilisateur à supprimer
     */
    @DeleteMapping("/{id}")
    public void deleteUser(@PathVariable Long id){
        userRepository.deleteById(id);
    }
}
