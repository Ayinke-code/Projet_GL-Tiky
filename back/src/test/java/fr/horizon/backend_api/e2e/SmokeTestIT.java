package fr.horizon.backend_api.e2e;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

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
    }

    static ChromeOptions options() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless", "--no-sandbox", "--disable-dev-shm-usage");
        return options;
    }

    @Test
    void shouldOpenFrontend(ChromeDriver driver) {

        Assertions.assertNotNull(frontUrl);

        driver.get(frontUrl);

        WebElement brand = driver.findElement(By.cssSelector("span.text-indigo-400"));

        Assertions.assertEquals("tk.", brand.getText());
    }
}