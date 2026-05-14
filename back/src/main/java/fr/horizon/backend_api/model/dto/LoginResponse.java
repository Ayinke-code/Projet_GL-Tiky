package fr.horizon.backend_api.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Set;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class LoginResponse {

    private Long id;
    private String name;
    private String token;
    private String role;
    private Set<String> roles;
    private String email;

}