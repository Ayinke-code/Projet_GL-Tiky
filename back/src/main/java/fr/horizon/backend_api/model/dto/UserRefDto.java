package fr.horizon.backend_api.model.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Set;

@Builder
@Data
public class UserRefDto {
    private Long id;
    private String name;
    private Set<String> roles;
}
