package fr.horizon.backend_api.e2e;

import io.github.cdimascio.dotenv.Dotenv;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

abstract class BaseSeleniumIT {

    protected WebDriver driver;
    protected String frontUrl;

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
                "--window-size=1920,1080",
                "--ignore-certificate-errors"
        );
        driver = new ChromeDriver(options);
    }

    @AfterEach
    void tearDown() {
        if (driver != null) driver.quit();
    }
}
