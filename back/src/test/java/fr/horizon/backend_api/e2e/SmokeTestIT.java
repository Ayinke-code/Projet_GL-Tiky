package fr.horizon.backend_api.e2e;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;

import io.github.cdimascio.dotenv.Dotenv;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

public class SmokeTestIT {

    private WebDriver driver;
    private String frontUrl;

    @BeforeEach
    void setUp() {
        frontUrl = System.getenv("FRONT_URL");
        if (frontUrl == null) {
            Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
            frontUrl = dotenv.get("FRONT_URL");
        }
        Assertions.assertNotNull(frontUrl);

        ChromeOptions options = new ChromeOptions();
        options.addArguments(
                "--headless=new",
                "--no-sandbox",
                "--disable-dev-shm-usage",
                "--disable-gpu",
                "--remote-debugging-port=0",
                "--window-size=1920,1080"
        );
        driver = new ChromeDriver(options);
    }

    @AfterEach
    void tearDown() {
        if (driver != null) driver.quit();
    }

    @Test
    void shouldOpenFrontend() {
        driver.get(frontUrl);
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        WebElement brand = wait.until(
                ExpectedConditions.visibilityOfElementLocated(By.cssSelector("span.text-indigo-400"))
        );
        Assertions.assertEquals("tk.", brand.getText());
    }
}