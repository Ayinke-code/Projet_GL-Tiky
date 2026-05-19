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
    private Set<String> roles;
    // Renseigne sur l'utilisateur a créé une issue qui est 'OUVERT'
    private boolean isContributor;
    // Renseigne si il est follower de la catégorie
    private boolean follower;
}
