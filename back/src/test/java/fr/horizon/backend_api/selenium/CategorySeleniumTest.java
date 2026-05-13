//package fr.horizon.backend_api.selenium;
//
//import io.github.bonigarcia.wdm.WebDriverManager;
//import org.junit.jupiter.api.*;
//import org.openqa.selenium.By;
//import org.openqa.selenium.WebDriver;
//import org.openqa.selenium.WebElement;
//import org.openqa.selenium.chrome.ChromeDriver;
//import org.openqa.selenium.chrome.ChromeOptions;
//import org.openqa.selenium.support.ui.ExpectedConditions;
//import org.openqa.selenium.support.ui.WebDriverWait;
//
//import java.time.Duration;
//
//import static org.junit.jupiter.api.Assertions.assertTrue;
//
//@TestInstance(TestInstance.Lifecycle.PER_CLASS)
//@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
//public class CategorySeleniumTest {
//
//    private WebDriver driver;
//    private WebDriverWait wait;
//    private final String baseUrl = "http://localhost:3000/categorie";
//
//    @BeforeAll
//    void setup() {
//        WebDriverManager.chromedriver().setup();
//        ChromeOptions options = new ChromeOptions();
//        options.addArguments("--headless");
//        options.addArguments("--no-sandbox");
//        options.addArguments("--disable-dev-shm-usage");
//        driver = new ChromeDriver(options);
//        wait = new WebDriverWait(driver, Duration.ofSeconds(10));
//    }
//
//    @AfterAll
//    void tearDown() {
//        if (driver != null) {
//            driver.quit();
//        }
//    }
//
//    @Test
//    @Order(1)
//    @DisplayName("Devrait afficher la page de gestion des catégories")
//    void testPageLoad() {
//        driver.get(baseUrl);
//        // On attend que le titre apparaisse
//        WebElement title = wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//h1[contains(text(), 'Gestion des Catégories')]")));
//        assertTrue(title.isDisplayed());
//    }
//
//    @Test
//    @Order(3)
//    @DisplayName("Devrait créer une catégorie (Parcours complet Selenium)")
//    void testCreateCategoryFullFlow() {
//        driver.get(baseUrl);
//
//        // 1. Ouvrir la modale
//        WebElement createBtn = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//button[contains(text(), 'Créer une catégorie')]")));
//        createBtn.click();
//
//        // 2. Remplir le formulaire
//        WebElement nameInput = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("category-name-input")));
//        nameInput.sendKeys("Biologie");
//
//        WebElement subjectInput = driver.findElement(By.id("category-subject-input"));
//        subjectInput.sendKeys("Génétique Humaine");
//
//        // 3. Soumettre
//        WebElement submitBtn = driver.findElement(By.id("category-submit-button"));
//        submitBtn.click();
//
//        // 4. Vérifier que la catégorie apparaît dans la liste
//        // On attend que la modale se ferme ou que l'élément apparaisse
//        WebElement newCategory = wait.until(ExpectedConditions.visibilityOfElementLocated(
//            By.xpath("//h3[contains(text(), 'Génétique Humaine')]")
//        ));
//
//        assertTrue(newCategory.isDisplayed());
//    }
//}
