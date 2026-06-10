package fr.horizon.backend_api.model.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class UpdateRolesDto {

    private List<String> roles;
}
