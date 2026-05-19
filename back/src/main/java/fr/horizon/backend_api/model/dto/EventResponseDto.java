package fr.horizon.backend_api.model.dto;

import fr.horizon.backend_api.model.enums.EventType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventResponseDto {
    private Long id;
    private String name;
    private String description;
    private EventType eventType;
    private String categoryName;
    private String createdBy;
    private LocalDateTime createdAt;
}
