package fr.horizon.backend_api;

import fr.horizon.backend_api.config.DataInitializer;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
        "spring.datasource.driverClassName=org.h2.Driver",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "cors.allowed-origins=http://localhost",
        "JWT_SECRET=test-secret-key-that-is-long-enough-32chars",
        "JWT_EXPIRATION=3600000",
        "DEFAULT_PASSWORD=test",
        "app.default-password=test"
})
class BackendApiApplicationTest {

    @MockitoBean
    private DataInitializer dataInitializer;

    @Test
    void contextLoads() {}
}