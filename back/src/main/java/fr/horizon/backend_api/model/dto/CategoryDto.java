package fr.horizon.backend_api.model.dto;

import fr.horizon.backend_api.model.enums.CategoryStatus;
import fr.horizon.backend_api.model.enums.CategoryType;
import lombok.Builder;
import lombok.Data;

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
    private java.time.LocalDateTime createdAt;
    private java.time.LocalDateTime updatedAt;
    private Boolean followedByCurrentUser;
}
