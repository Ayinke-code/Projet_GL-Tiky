package fr.horizon.backend_api.model.dto;

import fr.horizon.backend_api.model.enums.GroupType;
import lombok.Builder;
import lombok.Data;

import java.util.Set;

@Builder
@Data
public class StudentGroupDto {
    private Long id;
    private String name;
    private GroupType type;
    private Long categoryId;
    private String categoryName;
    private Long parentGroupId;
    private String parentGroupName;
    private Long promotionId;
    private String promotionName;
    private Integer year;
    private UserRefDto createdBy;
    private Set<UserRefDto> members;
    private Set<UserRefDto> teachers;
}
