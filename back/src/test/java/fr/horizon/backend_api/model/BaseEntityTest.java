package fr.horizon.backend_api.model;

import fr.horizon.backend_api.repository.CategoryRepository;
import fr.horizon.backend_api.model.enums.CategoryStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class BaseEntityTest {

    @Autowired
    private CategoryRepository categoryRepository;

    @Test
    void shouldSetCreatedAtAndUpdatedAtOnPersist() {
        Category entity = Category.builder()
                .name("Test")
                .subject("Test subject")
                .status(CategoryStatus.ACTIVE)
                .build();

        Category saved = categoryRepository.saveAndFlush(entity);

        assertNotNull(saved.getCreatedAt());
        assertNotNull(saved.getUpdatedAt());
        assertEquals(saved.getCreatedAt(), saved.getUpdatedAt());
    }
}