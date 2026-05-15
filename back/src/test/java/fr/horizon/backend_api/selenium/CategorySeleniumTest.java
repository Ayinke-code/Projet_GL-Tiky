package fr.horizon.backend_api.selenium;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.junit.jupiter.api.*;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.io.File;

import static org.junit.jupiter.api.Assertions.assertTrue;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class CategorySeleniumTest {

    private WebDriver driver;
    private WebDriverWait wait;
    private final String baseUrl = "http://localhost:3000/categories";

    private Process frontend;

    @BeforeAll
    void setup() throws Exception {
        frontend = new ProcessBuilder("npm", "start")
                .directory(new File("../front"))
                .redirectErrorStream(true)
                .inheritIO()
                .start();

        waitForFrontend();

        WebDriverManager.chromedriver()
                .browserVersion("144")
                .setup();

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");

        driver = new ChromeDriver(options);
        wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    }

    @AfterAll
    void tearDown() {
        if (driver != null) driver.quit();
        if (frontend != null) frontend.destroy();
    }

    private void waitForFrontend() throws InterruptedException {
        int maxRetries = 30;

        for (int i = 0; i < maxRetries; i++) {
            try {
                java.net.HttpURLConnection connection =
                        (java.net.HttpURLConnection) new java.net.URL("http://localhost:3000/categories").openConnection();

                connection.setRequestMethod("GET");
                connection.setConnectTimeout(1000);
                connection.connect();

                int code = connection.getResponseCode();

                if (code >= 200 && code < 400) {
                    return;
                }

            } catch (Exception ignored) {}

            Thread.sleep(1000);
        }

        throw new RuntimeException("Frontend non disponible après timeout");
    }

    @Test
    @Order(1)
    @DisplayName("Devrait afficher la page de gestion des catégories")
    void testPageLoad() {
        driver.get(baseUrl);
        // On attend que le titre apparaisse
        WebElement title = wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//h1[contains(text(), 'Gestion des Catégories')]")));
        assertTrue(title.isDisplayed());
    }

    @Test
    @Order(3)
    @DisplayName("Devrait créer une catégorie (Parcours complet Selenium)")
    void testCreateCategoryFullFlow() {
        driver.get(baseUrl);

        // 1. Ouvrir la modale
        WebElement createBtn = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//button[contains(text(), 'Créer une catégorie')]")));
        createBtn.click();

        // 2. Remplir le formulaire
        WebElement nameInput = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("category-name-input")));
        nameInput.sendKeys("Biologie");

        WebElement subjectInput = driver.findElement(By.id("category-subject-input"));
        subjectInput.sendKeys("Génétique Humaine");

        // 3. Soumettre
        WebElement submitBtn = driver.findElement(By.id("category-submit-button"));
        submitBtn.click();

        // 4. Vérifier que la catégorie apparaît dans la liste
        // On attend que la modale se ferme ou que l'élément apparaisse
        WebElement newCategory = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//h3[contains(text(), 'Génétique Humaine')]")
        ));

        assertTrue(newCategory.isDisplayed());
    }
}