package fr.horizon.backend_api.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CategoryRequestDto {

    @NotBlank
    private String name;

    @NotBlank
    private String subject;

    @NotNull
    private Long ownerId;
}
