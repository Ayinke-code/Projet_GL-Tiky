package fr.horizon.backend_api.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.Set;

/**
 * DTO (Data Transfer Object) pour la réponse d'une catégorie.
 * Utilisé pour retourner les données de catégorie au frontend.
 * 
 * @author Tiky Team
 * @version 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoryResponseDTO {

    /**
     * Identifiant unique de la catégorie.
     */
    private Long id;

    /**
     * Nom de la catégorie.
     */
    private String name;

    /**
     * Statut de la catégorie.
     */
    private String status;

    /**
     * Identifiant de l'enseignant propriétaire.
     */
    private String owner;

    /**
     * Date de création.
     */
    private LocalDateTime createdAt;

    /**
     * Date de la dernière modification.
     */
    private LocalDateTime updatedAt;

    private String subject;

    private Set<UserDto> followers; 
}
