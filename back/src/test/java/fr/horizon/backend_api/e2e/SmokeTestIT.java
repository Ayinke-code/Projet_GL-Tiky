package fr.horizon.backend_api.e2e;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;

import io.github.bonigarcia.seljup.SeleniumJupiter;
import io.github.cdimascio.dotenv.Dotenv;

@ExtendWith(SeleniumJupiter.class)
public class SmokeTestIT {

    String frontUrl;

    @BeforeEach
    void setup() {
        frontUrl = System.getenv("FRONT_URL");
        if (frontUrl == null) {
            Dotenv dotenv = Dotenv.load();
            frontUrl = dotenv.get("FRONT_URL");
        }
        // Forcer headless via propriétés système pour SeleniumJupiter
        System.setProperty("sel.jup.chrome.arguments", "--headless --no-sandbox --disable-dev-shm-usage --disable-gpu");
    }

    @Test
    void shouldOpenFrontend(ChromeDriver driver) {

        Assertions.assertNotNull(frontUrl);

        driver.get(frontUrl);

        WebElement brand = driver.findElement(By.cssSelector("span.text-indigo-400"));

        Assertions.assertEquals("tk.", brand.getText());
    }
}