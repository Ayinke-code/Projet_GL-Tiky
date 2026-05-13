package fr.horizon.backend_api.model.dto;

import fr.horizon.backend_api.model.enums.CategoryStatus;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.Set;

@Data
@Builder
public class CategoryDto {
    private Long id;
    private String name;
    private String subject;
    private CategoryStatus status;
    private String ownerName;
    private Long ownerId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Set<UserDto> followers;
}
