package fr.horizon.backend_api.model;

import fr.horizon.backend_api.model.enums.GroupType;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class StudentGroupTest {

    @Test
    void shouldCreateStudentGroupWithBuilder() {
        User creator = new User();
        creator.setId(1L);
        creator.setName("Sophie ROUSSEAU");

        Category category = new Category();
        category.setId(10L);
        category.setName("Projet Horizon");

        StudentGroup group = StudentGroup.builder()
                .name("Groupe Horizon A")
                .type(GroupType.PROJECT)
                .createdBy(creator)
                .category(category)
                .build();

        assertEquals("Groupe Horizon A", group.getName());
        assertEquals(GroupType.PROJECT, group.getType());
        assertEquals(creator, group.getCreatedBy());
        assertEquals(category, group.getCategory());
        assertNotNull(group.getMembers());
        assertTrue(group.getMembers().isEmpty());
        assertNull(group.getParentGroup());
    }

    @Test
    void shouldInitializeMembersAsEmptySetByDefault() {
        StudentGroup group = StudentGroup.builder()
                .name("Groupe LD")
                .type(GroupType.OPTION)
                .build();

        assertNotNull(group.getMembers());
        assertTrue(group.getMembers().isEmpty());
    }

    @Test
    void shouldAcceptMembersSet() {
        User s1 = new User();
        s1.setId(1L);
        User s2 = new User();
        s2.setId(2L);

        StudentGroup group = StudentGroup.builder()
                .name("Groupe B")
                .type(GroupType.PROJECT)
                .members(Set.of(s1, s2))
                .build();

        assertEquals(2, group.getMembers().size());
    }

    @Test
    void shouldAcceptParentGroupForProjectType() {
        StudentGroup optionGroup = StudentGroup.builder()
                .name("LD — Promo 2026")
                .type(GroupType.OPTION)
                .build();

        StudentGroup projectGroup = StudentGroup.builder()
                .name("Projet Horizon A")
                .type(GroupType.PROJECT)
                .parentGroup(optionGroup)
                .build();

        assertNotNull(projectGroup.getParentGroup());
        assertEquals("LD — Promo 2026", projectGroup.getParentGroup().getName());
        assertEquals(GroupType.OPTION, projectGroup.getParentGroup().getType());
    }

    @Test
    void shouldHaveNullParentGroupByDefault() {
        StudentGroup group = StudentGroup.builder()
                .name("Groupe X")
                .type(GroupType.OPTION)
                .build();

        assertNull(group.getParentGroup());
    }
}
