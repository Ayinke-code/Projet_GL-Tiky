package fr.horizon.backend_api.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CategoryRequestDto {

    @NotBlank
    private String name;

    @NotBlank
    private String subject;

    @NotNull
    private Long ownerId;
}
