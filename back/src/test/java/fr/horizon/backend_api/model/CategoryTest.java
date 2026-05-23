package fr.horizon.backend_api.model;

import fr.horizon.backend_api.model.enums.CategoryStatus;
import org.junit.jupiter.api.Test;

import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.*;

class CategoryTest {

    @Test
    void shouldCreateCategoryWithBuilder() {

        Category category = Category.builder()
                .name("IT")
                .subject("Réseau")
                .build();

        assertEquals("IT", category.getName());
        assertEquals("Réseau", category.getSubject());
        assertEquals(CategoryStatus.ACTIVE, category.getStatus());
        assertNotNull(category.getFollowers());
        assertTrue(category.getFollowers().isEmpty());
    }

    @Test
    void shouldInitializeFollowersIfNotProvided() {

        Category category = new Category();

        assertNotNull(category.getFollowers());
        assertTrue(category.getFollowers().isEmpty());
    }

    @Test
    void shouldAddFollowerToCategory() {

        Category category = new Category();
        category.setFollowers(new HashSet<>());

        User user = new User();
        user.setId(1L);

        category.getFollowers().add(user);

        assertEquals(1, category.getFollowers().size());
        assertTrue(category.getFollowers().contains(user));
    }

    @Test
    void shouldRemoveFollowerFromCategory() {

        Category category = new Category();
        category.setFollowers(new HashSet<>());

        User user = new User();
        user.setId(1L);

        category.getFollowers().add(user);
        category.getFollowers().remove(user);

        assertTrue(category.getFollowers().isEmpty());
    }
}