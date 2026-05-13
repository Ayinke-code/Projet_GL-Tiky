package fr.horizon.backend_api.model;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class UserTest {

    @Test
    void shouldCreateUserWithBuilder() {

        Role role = Role.builder()
                .name("ADMIN")
                .build();

        User user = User.builder()
                .name("John")
                .email("john@test.com")
                .password("secret")
                .roles(Set.of(role))
                .build();

        assertEquals("John", user.getName());
        assertEquals("john@test.com", user.getEmail());
        assertEquals("secret", user.getPassword());

        assertNotNull(user.getRoles());
        assertEquals(1, user.getRoles().size());
    }

    @Test
    void shouldAddRoleToUser() {

        User user = new User();

        Role role = new Role("ADMIN");

        user.getRoles().add(role);

        assertEquals(1, user.getRoles().size());
    }
}