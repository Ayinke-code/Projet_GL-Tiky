package fr.horizon.backend_api.model.dto;

import fr.horizon.backend_api.model.enums.TeacherType;
import lombok.Builder;
import lombok.Data;

import java.util.Set;

@Builder
@Data
public class UserRefDto {
    private Long id;
    private String name;
    private Set<String> roles;
    private TeacherType teacherType;
    private boolean isIssueManagerInCategory;
}
