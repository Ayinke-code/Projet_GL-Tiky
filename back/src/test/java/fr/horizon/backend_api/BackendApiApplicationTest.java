package fr.horizon.backend_api;

import fr.horizon.backend_api.config.DataInitializer;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(properties = {
        "JWT_SECRET=test-secret",
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