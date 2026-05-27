package fr.horizon.backend_api.e2e;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

public class SmokeTestIT extends BaseSeleniumIT {

    @Test
    void shouldOpenFrontend() {
        driver.get(frontUrl);
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        WebElement brand = wait.until(
                ExpectedConditions.visibilityOfElementLocated(By.cssSelector("span.text-indigo-400"))
        );
        Assertions.assertEquals("tk.", brand.getText());
    }

    @Test
    void shouldNavigateToLogin() {
        driver.get(frontUrl);
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        WebElement loginButton = wait.until(
                ExpectedConditions.elementToBeClickable(By.cssSelector("a[href='/login']"))
        );
        loginButton.click();
        wait.until(ExpectedConditions.urlContains("/login"));
        Assertions.assertTrue(driver.getCurrentUrl().contains("/login"));
    }
}