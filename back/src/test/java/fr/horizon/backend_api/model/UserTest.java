package fr.horizon.backend_api.model;

import fr.horizon.backend_api.model.enums.TeacherType;
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
    void shouldCreateTeacherWithTeacherType() {

        User teacher = User.builder()
                .name("Sophie ROUSSEAU")
                .email("teacher@teacher.fr")
                .teacherType(TeacherType.COURSE_LEADER)
                .build();

        assertEquals(TeacherType.COURSE_LEADER, teacher.getTeacherType());
        assertEquals("Sophie ROUSSEAU", teacher.getName());
    }

    @Test
    void shouldCreateTeacherWithOptionLeaderType() {

        User teacher = User.builder()
                .name("Paul MARTIN")
                .email("teacher2@teacher.fr")
                .teacherType(TeacherType.OPTION_LEADER)
                .build();

        assertEquals(TeacherType.OPTION_LEADER, teacher.getTeacherType());
    }

    @Test
    void shouldHaveNullTeacherTypeForStudent() {

        User student = User.builder()
                .name("Raoudate")
                .email("student@student.fr")
                .teacherType(null)
                .build();

        assertNull(student.getTeacherType());
    }
}