package fr.horizon.backend_api.model.dto;

import fr.horizon.backend_api.model.Role;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Set;

@Getter
@AllArgsConstructor
public class LoginResponse {

    private Long id;
    private String name;
    private String token;
    private Set<String> roles;
    private String email;

}
