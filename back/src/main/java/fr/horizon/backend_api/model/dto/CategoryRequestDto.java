package fr.horizon.backend_api.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class CategoryRequestDto {

    @NotBlank
    private Long id;
    private String name;

    @NotBlank
    private String subject;
    private String status;
    private Long requesterId;
    private String requesterName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @NotNull
    private Long ownerId;
    private String description; // Optionnel
}