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
        assertEquals(CategoryStatus.ACTIVE, category.getStatus());
    }

    @Test
    void shouldSetOwner() {

        Category category = new Category();
        User user = new User();
        user.setEmail("owner@test.fr");

        category.setOwner(user);

        assertNotNull(category.getOwner());
        assertEquals("owner@test.fr", category.getOwner().getEmail());
    }
}
