package fr.horizon.backend_api.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

import fr.horizon.backend_api.model.enums.CategoryType;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventRequestDto {
    private Long categoryId;
    private String name;
    private String description;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private CategoryType categoryType;
    /** IDs des groupes projet à affecter (AUDIT / DEFENSE uniquement) */
    private Set<Long> projectGroupIds;
}
