package fr.horizon.backend_api.model.dto;

import fr.horizon.backend_api.model.enums.GroupType;
import lombok.Data;

import java.util.Set;

@Data
public class StudentGroupCreateDto {
    private String name;
    private GroupType type;
    private Long categoryId;
    private String customCategoryName;
    private Long parentGroupId; // optionnel, uniquement pour les groupes PROJECT
    private Set<Long> memberIds;
}
