package fr.horizon.backend_api.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CategoryRequestDto {
    private Long id;
    private String name;
    private String subject;
    private String status;
    private Long requesterId;
    private String requesterName;

    private String description; // Optionnel
}
