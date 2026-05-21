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

    @Test
    void isTeacher_shouldReturnFalse_whenRolesIsNull() {
        User user = new User();
        user.setRoles(null);

        assertFalse(user.isTeacher());
    }

    @Test
    void isTeacher_shouldReturnTrue_withTeacherRoleAndPrefix() {
        User user = new User();
        user.setRoles(Set.of(new Role("ROLE_TEACHER")));

        assertTrue(user.isTeacher());
    }

    @Test
    void getRoleNames_shouldUppercaseRoleNames() {
        User user = new User();
        user.setRoles(Set.of(new Role("teacher"), new Role("admin")));

        Set<String> roleNames = user.getRoleNames();

        assertEquals(Set.of("TEACHER", "ADMIN"), roleNames);
    }

    @Test
    void getRoleNames_shouldReturnEmptySet_whenRolesIsNull() {
        User user = new User();
        user.setRoles(null);

        Set<String> roleNames = user.getRoleNames();

        assertNotNull(roleNames);
        assertTrue(roleNames.isEmpty());
    }

    @Test
    void getDisplayRole_shouldReturnAdmin_whenAdminAndTeacherArePresent() {
        User user = new User();
        user.setRoles(Set.of(new Role("ROLE_ADMIN"), new Role("TEACHER")));

        assertEquals("ADMIN", user.getDisplayRole());
    }

    @Test
    void getDisplayRole_shouldReturnTeacher_whenTeacherIsPresentWithoutAdmin() {
        User user = new User();
        user.setRoles(Set.of(new Role("role_teacher")));

        assertEquals("TEACHER", user.getDisplayRole());
    }

    @Test
    void getDisplayRole_shouldReturnStudent_whenNoRoles() {
        User user = new User();
        user.setRoles(Set.of());

        assertEquals("STUDENT", user.getDisplayRole());
    }

    @Test
    void getDisplayRole_shouldReturnStudent_whenRolesIsNull() {
        User user = new User();
        user.setRoles(null);

        assertEquals("STUDENT", user.getDisplayRole());
    }
}