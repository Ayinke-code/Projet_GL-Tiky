package fr.horizon.backend_api.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CategoryRequestDto {

    @NotBlank
    private String name;

    @NotBlank
    private String subject;

    @NotNull
    private Long ownerId;
}
