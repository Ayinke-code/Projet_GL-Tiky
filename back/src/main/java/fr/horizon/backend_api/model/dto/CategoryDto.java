package fr.horizon.backend_api.model.dto;

import fr.horizon.backend_api.model.enums.CategoryStatus;
import fr.horizon.backend_api.model.enums.EventType;
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
    private EventType eventType;
    private Long ownerId;
}
