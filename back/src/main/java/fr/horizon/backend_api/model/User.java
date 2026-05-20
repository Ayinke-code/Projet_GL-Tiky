package fr.horizon.backend_api.model;

import java.util.stream.Collectors;
import java.util.HashSet;
import java.util.Set;

import fr.horizon.backend_api.model.enums.TeacherType;
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

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = true)
    private TeacherType teacherType;

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
        if (this.roles == null) {
            return false;
        }
        return this.roles.stream()
                .anyMatch(role -> role.getName().equalsIgnoreCase("ROLE_TEACHER") || role.getName().equalsIgnoreCase("TEACHER"));
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
        if (roleNames.contains("ROLE_ADMIN") || roleNames.contains("ADMIN")) {
            return "ADMIN";
        }

        // Priorité 2 : TEACHER
        if (roleNames.contains("ROLE_TEACHER") || roleNames.contains("TEACHER")) {
            return "TEACHER";
        }

        // Par défaut : STUDENT
        return "STUDENT";
    }
}