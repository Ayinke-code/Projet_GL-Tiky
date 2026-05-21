package fr.horizon.backend_api.controller;

import java.util.List;

import fr.horizon.backend_api.model.dto.UserDto;
import fr.horizon.backend_api.model.dto.UserFollowerDto;
import fr.horizon.backend_api.model.dto.UserRefDto;
import fr.horizon.backend_api.model.dto.UserRequestDto;
import fr.horizon.backend_api.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Contrôleur REST de gestion des utilisateurs.
 * <p>
 * Expose les endpoints CRUD permettant de gérer les utilisateurs.
 * Toute la logique métier est déléguée à {@link UserService}.
 */
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * Récupère l'ensemble des utilisateurs.
     *
     * @return liste des utilisateurs sous forme de DTO
     */
    @GetMapping
    public List<UserDto> getAllUsers() {
        return userService.findAll();
    }

    /**
     * Récupère les utilisateurs filtrés par rôles.
     */
    @PostMapping("/byRoles")
    public List<UserRefDto> getAllUsersByRole(@RequestBody List<String> roles) {
        return userService.findAllByRoles(roles);
    }

    /**
     * Crée un nouvel utilisateur.
     *
     * @param dto données de création utilisateur
     * @return utilisateur créé sous forme de DTO
     */
    @PostMapping
    public UserDto createUser(@Valid @RequestBody UserRequestDto dto) {
        return userService.createUser(dto);
    }

    /**
     * Supprime un utilisateur à partir de son identifiant.
     *
     * @param id identifiant de l'utilisateur
     */
    @DeleteMapping("/{id}")
    public void deleteUser(@PathVariable Long id){
        userService.deleteById(id);
    }


    /**
     * Retourne la liste des teachers
     *
     * @return la liste des teachers en marquant qui est follower d'une catégorie avec le role de professeur
     */
    @GetMapping("/teachers/{categoryId}")
    public ResponseEntity<List<UserFollowerDto>> getAllTeacherWithCategoryDetail(@PathVariable Long categoryId) {
        return ResponseEntity.ok(userService.getAllTeachersWithCategoryDetail(categoryId));
    }
}
