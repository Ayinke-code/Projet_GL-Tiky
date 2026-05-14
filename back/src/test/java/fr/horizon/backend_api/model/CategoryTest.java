package fr.horizon.backend_api.model;

import fr.horizon.backend_api.model.enums.CategoryStatus;
import org.junit.jupiter.api.Test;

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
        assertEquals(CategoryStatus.ACTIVE, category.getStatus()); // valeur par défaut
        assertNotNull(category.getFollowers());
        assertTrue(category.getFollowers().isEmpty());
    }

    @Test
    void shouldAddFollower() {

        Category category = new Category();
        category.setFollowers(new java.util.HashSet<>());

        User user = new User();

        category.getFollowers().add(user);

        assertEquals(1, category.getFollowers().size());
    }
}