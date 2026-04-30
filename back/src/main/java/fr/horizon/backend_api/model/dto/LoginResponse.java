package fr.horizon.backend_api.model.dto;

import fr.horizon.backend_api.model.Role;

public class LoginResponse {

    private Long id;
    private String name;
    private String token;
    private String role;
    private String email;

    public LoginResponse(Long id, String name, String token, String role, String email) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.token = token;
        this.role = role;
    }

    public Long getId() { return this.id; }
    public String getName() { return this.name; }
    public String getToken() { return this.token; }
    public String getRole() { return this.role; }
    public String getEmail() { return this.email; }

}
