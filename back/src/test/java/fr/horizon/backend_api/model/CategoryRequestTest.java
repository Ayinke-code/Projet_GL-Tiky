package fr.horizon.backend_api.model;

import fr.horizon.backend_api.model.enums.CategoryRequestStatus;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CategoryRequestTest {

    @Test
    void shouldCreateCategoryRequestWithBuilder() {

        CategoryRequest request = CategoryRequest.builder()
                .name("IT")
                .subject("Réseau")
                .description("Demande de catégorie réseau informatique")
                .status(CategoryRequestStatus.PENDING)
                .build();

        assertEquals("IT", request.getName());
        assertEquals("Réseau", request.getSubject());
        assertEquals("Demande de catégorie réseau informatique", request.getDescription());
        assertEquals(CategoryRequestStatus.PENDING, request.getStatus());

        assertNull(request.getRequester());
        assertNull(request.getValidator());
        assertNull(request.getCategory());
    }

    @Test
    void shouldAssignRequester() {

        CategoryRequest request = new CategoryRequest();
        request.setRequester(new User());

        assertNotNull(request.getRequester());
    }

    @Test
    void shouldAssignValidator() {

        CategoryRequest request = new CategoryRequest();
        request.setValidator(new User());

        assertNotNull(request.getValidator());
    }

    @Test
    void shouldAssignCategoryWhenApproved() {

        CategoryRequest request = new CategoryRequest();
        Category category = new Category();

        request.setCategory(category);

        assertNotNull(request.getCategory());
    }
}