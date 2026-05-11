package fr.horizon.backend_api.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO (Data Transfer Object) pour la création/modification d'une catégorie.
 * 
 * @author Tiky Team
 * @version 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateCategoryDTO {

    /**
     * Nom de la catégorie.
     * Doit être non vide et contenir entre 3 et 100 caractères.
     */
    @NotBlank(message = "Le nom de la catégorie ne peut pas être vide")
    @Size(min = 3, max = 100, message = "Le nom doit contenir entre 3 et 100 caractères")
    private String name;

    /**
     * Sujet de la catégorie.
     */
    @NotBlank(message = "Le sujet ne peut pas être vide")
    @Size(max = 200, message = "Le sujet ne doit pas dépasser 200 caractères")
    private String subject;
 
    /**
     * Identifiant de l'enseignant (utilisé pour la création initiale ou le transfert de propriété).
     */
    private String teacher;
}
