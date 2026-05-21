package fr.horizon.backend_api.model;

import fr.horizon.backend_api.model.enums.RoleEnum;

import java.util.stream.Collectors;
import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "users")
public class User extends BaseEntity {

    private static final String ROLE_PREFIX = "ROLE_";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    @Builder.Default
    private Set<Role> roles = new HashSet<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<Message> messages = new HashSet<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<ConversationUser> conversations = new HashSet<>();

    public boolean isTeacher() {
        return hasRole(getRoleNames(), RoleEnum.TEACHER);
    }

    public Set<String> getRoleNames() {
        if (this.roles == null) {
            return new HashSet<>();
        }
        return this.roles.stream()
                .map(role -> role.getName().toUpperCase())
                .collect(Collectors.toSet());
    }

    public String getDisplayRole() {
        Set<String> roleNames = getRoleNames();

        // Priorité 1 : ADMIN (Si l'utilisateur a le rôle ADMIN, il est ADMIN avant tout)
        if (hasRole(roleNames, RoleEnum.ADMIN)) {
            return RoleEnum.ADMIN.value();
        }

        // Priorité 2 : TEACHER
        if (hasRole(roleNames, RoleEnum.TEACHER)) {
            return RoleEnum.TEACHER.value();
        }

        // Par défaut : STUDENT
        return RoleEnum.STUDENT.value();
    }

    private boolean hasRole(Set<String> roleNames, RoleEnum role) {
        if (roleNames == null || roleNames.isEmpty()) {
            return false;
        }
        String normalizedRole = role.value().toUpperCase();
        return roleNames.contains(normalizedRole) || roleNames.contains(ROLE_PREFIX + normalizedRole);
    }
}