package fr.horizon.backend_api.model.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class CategoryRequestDto {
    private Long id;
    private String name;
    private String subject;
    private String status;
    private Long requesterId;
    private String requesterName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private String description; // Optionnel
}
