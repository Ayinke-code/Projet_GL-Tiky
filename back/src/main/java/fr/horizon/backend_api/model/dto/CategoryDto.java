package fr.horizon.backend_api.model.dto;

import fr.horizon.backend_api.model.enums.CategoryStatus;
import fr.horizon.backend_api.model.enums.CategoryType;
import lombok.Builder;
import lombok.Data;

import java.util.Set;

@Data
@Builder
public class CategoryDto {

    private Long id;
    private String name;
    private String subject;
    private CategoryStatus status;
    private String ownerName;
    private CategoryType categoryType;
    private Long ownerId;
    private Set<Long> followerIds;
}
