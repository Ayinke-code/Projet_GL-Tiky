package fr.horizon.backend_api.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RoleTest {

    @Test
    void shouldCreateRoleWithConstructor() {

        Role role = new Role("ADMIN");

        assertEquals("ADMIN", role.getName());
        assertNotNull(role.getUsers());
        assertTrue(role.getUsers().isEmpty());
    }

    @Test
    void shouldCreateRoleWithBuilder() {

        Role role = Role.builder()
                .name("TEACHER")
                .build();

        assertEquals("TEACHER", role.getName());
        assertNotNull(role.getUsers());
        assertTrue(role.getUsers().isEmpty());
    }

    @Test
    void shouldAddUserToRole() {

        Role role = Role.builder()
                .name("ADMIN")
                .build();

        User user = new User();

        role.getUsers().add(user);

        assertEquals(1, role.getUsers().size());
    }
}