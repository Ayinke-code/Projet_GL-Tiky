package fr.horizon.backend_api.exception;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CategoryNotFoundExceptionTest {

    @Test
    void shouldBuildCorrectMessage() {

        CategoryNotFoundException ex = new CategoryNotFoundException(42L);

        assertEquals("Category not found with id: 42", ex.getMessage());
    }
}
