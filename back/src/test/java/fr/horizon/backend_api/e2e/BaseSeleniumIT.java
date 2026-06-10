package fr.horizon.backend_api.e2e;


import io.github.cdimascio.dotenv.Dotenv;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;


import java.time.Duration;

abstract class BaseSeleniumIT {

    protected WebDriver driver;
    protected WebDriverWait wait;
    protected String frontUrl;
    protected String password;

    @BeforeEach
    void setUp() {
        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();

        frontUrl = getConfig("FRONT_URL", dotenv);
        password = getConfig("DEFAULT_PASSWORD", dotenv);

        Assertions.assertNotNull(frontUrl, "FRONT_URL est introuvable (ni env, ni .env)");
        Assertions.assertNotNull(password, "DEFAULT_PASSWORD est introuvable (ni env, ni .env)");

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

    /**
     * Variable d'environnement réelle prioritaire, sinon repli sur le fichier .env.
     */
    private String getConfig(String key, Dotenv dotenv) {
        String value = System.getenv(key);
        return value != null ? value : dotenv.get(key);
    }

    @AfterEach
    void tearDown() {
        if (driver != null) driver.quit();
    }
}
