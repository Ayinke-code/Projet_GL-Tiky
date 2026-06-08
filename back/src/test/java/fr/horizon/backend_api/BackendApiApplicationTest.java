package fr.horizon.backend_api;

import fr.horizon.backend_api.config.DataInitializer;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb",
        "spring.datasource.driverClassName=org.h2.Driver",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "JWT_SECRET=test-secret",
        "JWT_EXPIRATION=3600000",
        "DEFAULT_PASSWORD=test",
        "app.default-password=test",
        "app.upload-dir=./uploads"
})
class BackendApiApplicationTest {

    @MockitoBean
    private DataInitializer dataInitializer;

    @Test
    void contextLoads() {}
}