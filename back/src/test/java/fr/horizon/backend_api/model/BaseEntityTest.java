package fr.horizon.backend_api.model;

import jakarta.persistence.*;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class BaseEntityTest {

    @PersistenceContext
    private EntityManager entityManager;

    @Entity
    static class DummyEntity extends BaseEntity {
        @Id
        @GeneratedValue
        private Long id;
    }

    @Test
    void shouldSetCreatedAtAndUpdatedAtOnPersist() {

        DummyEntity entity = new DummyEntity();

        entityManager.persist(entity);
        entityManager.flush();

        assertNotNull(entity.getCreatedAt());
        assertNotNull(entity.getUpdatedAt());
        assertEquals(entity.getCreatedAt(), entity.getUpdatedAt());
    }
}