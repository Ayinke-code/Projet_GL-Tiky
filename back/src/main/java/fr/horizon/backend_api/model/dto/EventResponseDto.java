package fr.horizon.backend_api.model.dto;

import fr.horizon.backend_api.model.enums.CategoryType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventResponseDto {
    private Long id;
    private String name;
    private String description;
    private CategoryType categoryType;
    private String categoryName;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private List<ProjectGroupRefDto> projectGroups;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProjectGroupRefDto {
        private Long id;
        private String name;
        private String categoryName;
        private String parentGroupName;
        private Set<TeacherRefDto> teachers;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TeacherRefDto {
        private String name;
        private String teacherType;
    }
}
