package fr.horizon.backend_api.model;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class BaseEntityTest {

    @PersistenceContext
    private EntityManager entityManager;

    @Entity
    static class TestEntity extends BaseEntity {
        @jakarta.persistence.Id
        @jakarta.persistence.GeneratedValue
        private Long id;
    }

    @Test
    void shouldSetCreatedAtAndUpdatedAtOnPersist() {

        TestEntity entity = new TestEntity();

        entityManager.persist(entity);
        entityManager.flush();

        assertNotNull(entity.createdAt);
        assertNotNull(entity.updatedAt);
        assertEquals(entity.createdAt, entity.updatedAt);
    }
}