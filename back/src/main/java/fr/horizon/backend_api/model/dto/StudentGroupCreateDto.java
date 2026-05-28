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
    private Long parentGroupId;  // optionnel, uniquement pour les groupes PROJECT
    private Long promotionId;    // optionnel, pour les groupes OPTION
    private Integer year;        // uniquement pour les groupes PROMOTION
    private Set<Long> memberIds;
}
