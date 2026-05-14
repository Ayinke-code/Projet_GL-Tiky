package fr.horizon.backend_api.model;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
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
    static class TestEntity extends BaseEntity {
        @jakarta.persistence.Id
        @jakarta.persistence.GeneratedValue
        static class DummyEntity extends BaseEntity {
            @Id
            @GeneratedValue
            private Long id;
        }

        @Test
        void shouldSetCreatedAtAndUpdatedAtOnPersist() {

            TestEntity entity = new TestEntity();
            DummyEntity entity = new DummyEntity();

            entityManager.persist(entity);
            entityManager.flush();

            assertNotNull(entity.createdAt);
            assertNotNull(entity.updatedAt);
            assertEquals(entity.createdAt, entity.updatedAt);
            assertNotNull(entity.getCreatedAt());
            assertNotNull(entity.getUpdatedAt());
            assertEquals(entity.getCreatedAt(), entity.getUpdatedAt());
        }
    }