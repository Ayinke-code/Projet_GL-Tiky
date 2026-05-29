package fr.horizon.backend_api.e2e;

import fr.horizon.backend_api.repository.CategoryRepository;
import fr.horizon.backend_api.repository.IssueRepository;
import io.github.cdimascio.dotenv.Dotenv;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.safari.SafariDriver;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Duration;

abstract class BaseSeleniumIT {

    protected WebDriver driver;
    protected WebDriverWait wait;
    protected String frontUrl;
    protected String password;

    @BeforeEach
    void setUp() {
        frontUrl = System.getenv("FRONT_URL");
        password = System.getenv("DEFAULT_PASSWORD");
        if (frontUrl == null) {
            Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
            frontUrl = dotenv.get("FRONT_URL");
            password = dotenv.get("DEFAULT_PASSWORD");
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
        wait = new WebDriverWait(driver, Duration.ofSeconds(5));
    }

    @AfterEach
    void tearDown() {
        if (driver != null) driver.quit();
    }
}
