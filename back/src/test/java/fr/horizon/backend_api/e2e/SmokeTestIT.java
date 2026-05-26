package fr.horizon.backend_api.e2e;

import io.github.bonigarcia.seljup.SeleniumJupiter;

import io.github.cdimascio.dotenv.Dotenv;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;

@ExtendWith(SeleniumJupiter.class)
public class SmokeTestIT {

    Dotenv dotenv = Dotenv.load();
    String frontUrl = dotenv.get("FRONT_URL");

    @Test
    void shouldOpenFrontend(ChromeDriver driver) {

        Assertions.assertNotNull(frontUrl);

        driver.get(frontUrl);

        WebElement brand = driver.findElement(By.cssSelector("span.text-indigo-400"));

        Assertions.assertEquals("tk.", brand.getText());
    }
}