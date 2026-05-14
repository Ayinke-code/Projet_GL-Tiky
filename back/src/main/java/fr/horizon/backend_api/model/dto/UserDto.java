package fr.horizon.backend_api.model.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Set;

@Data
@Builder
public class UserDto {

    private Long id;
    private String name;
    private String email;
    private String role;
    private Set<String> roles;
    // Renseigne sur l'utilisateur a créé une issue qui est 'OUVERT'
    private boolean isContributor;
    private String wsToken;
}