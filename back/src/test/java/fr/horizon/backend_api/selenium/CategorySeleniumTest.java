package fr.horizon.backend_api.selenium;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.junit.jupiter.api.*;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import java.time.Duration;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("selenium")
@DisplayName("Test Selenium - Parcours Création de Catégorie")
class CategorySeleniumTest {

    private WebDriver driver;
    private WebDriverWait wait;
    private final String BASE_URL = "http://localhost:3000";
    private String randomName;

    @BeforeAll
    static void setupClass() {
        WebDriverManager.chromedriver().setup();
    }

    @BeforeEach
    void setupTest() {
        // Supprimez tout ce qui touche à UserRepository, RoleRepository, etc.
        // Si vous devez créer des données de test, faites-le via une API
        // ou via un script SQL lancé au démarrage de votre application.

        this.randomName = "Mathématiques " + System.currentTimeMillis();
        ChromeOptions options = new ChromeOptions();
        // options.addArguments("--headless"); // Laissez commenté tant que ça ne marche
        // pas !
        driver = new ChromeDriver(options);
        wait = new WebDriverWait(driver, Duration.ofSeconds(20));
    }

    @AfterEach
    void teardown() {
        if (driver != null)
            driver.quit();
    }

    @Test
    void testCategoryCreationFlow() {
        // 1. Accès
        driver.get(BASE_URL + "/categorie");

        // 2. Connexion (si votre site est protégé, vous devez le faire ici)
        // ... (Logique de connexion)

        // 3. Action
        wait.until(ExpectedConditions.elementToBeClickable(By.id("category-name-input"))).sendKeys(this.randomName);
        driver.findElement(By.id("category-subject-input")).sendKeys("Géométrie");
        // ... (Reste de votre logique)

        driver.findElement(By.id("category-submit-button")).click();

        // 4. Verification
        WebElement result = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//*[contains(text(), '" + this.randomName + "')]")));

        assertTrue(result.isDisplayed());
    }
}