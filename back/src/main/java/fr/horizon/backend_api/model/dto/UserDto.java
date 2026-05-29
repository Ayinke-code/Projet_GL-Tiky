package fr.horizon.backend_api.model.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Set;

import fr.horizon.backend_api.model.enums.TeacherType;

@Data
@Builder
public class UserDto {

    private Long id;
    private String name;
    private String email;
    private Set<String> roles;
    // Renseigne sur l'utilisateur a créé une issue qui est 'OUVERT'
    private boolean isContributor;
    private TeacherType teacherType;
    // Token pour WebSocket
    private String wsToken;
}
