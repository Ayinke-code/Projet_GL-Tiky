package fr.horizon.backend_api.e2e;

import io.github.cdimascio.dotenv.Dotenv;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;

import java.time.Duration;
import java.util.List;


class Sprint2Test extends BaseSeleniumIT{

    static final Dotenv dotenv = Dotenv.load();

    static final String EMAIL_TEACHER_TEST = "admin@admin.fr";
    static final String EMAIL_STUDENT_TEST = "student@student.fr";

    static final String PASSWORD_TEST = dotenv.get("DEFAULT_PASSWORD");

    private static final String URL_ERROR = "l'URL ne correspond pas";
    private static final String ISSUE_PAGE_TITLE = "Tickets";

    private static final String TITRE_TICKET= "titre-test";
    private static final String DESCRIPTION = "description-test";

    private static final String STATUT_TICKET_EN_ATTENTE = "En attente";
    private static final String STAUT_TICKET_INCOMPLET = "Incomplet";

    private static final String TITRE_CATEGORIE= "categorie-test";
    private static final String SUBJECT_CATEGORIE= "sujet-test";
    private static final String CATEGORIE_PROPRIETAIRE = "Propriétaire";

    private static final String MESSAGE = "MESSAGE TEST";
    private static final String EMPTY_ASSIGNATION = "Aucune demande d'affectation à compléter.";

    @Test
    @DisplayName("Scénario 1.1 Connexion")
    void connexionTest(){
        connexion(false);
    }

    @Test
    @DisplayName("Scénario 1.2 Déconnexion")
    void deconnexionTest(){
        // précondtion
        connexion(false);

        deconnexion();

    }

    @Test
    @DisplayName("Scénario 1.3.1 Crée un ticket")
    void creeTicketSansMarquerIncompletTest(){
        // précondition
        connexion(false);
        creeTicket(false);
    }

    @Test
    @DisplayName("Scénario 1.3.2 Crée un ticket incomplet")
    void creeTicketMarquerIncompletTest(){
        // précondition
        connexion(false);

        creeTicket(true);
    }

    @Test
    @DisplayName("Scénario 1.5.1 proposer une catégorie")
    void proposeCategorieAsTeacherTest(){
        //précondition
        connexion(false);

        allerPageCategories();
        creeCategorie(true);
    }

    @Test
    @Disabled("Test en échec à cause de clic fantôme")
    @DisplayName("Scénario 1.5.2 proposer une catégorie")
    void proposeCategorieAsStudentTest(){
        //précondition
        connexion(true);

        allerPageCategories();
        creeCategorie(false);
    }

    @Test
    @DisplayName("Scénario 1.6.1 envoyer un message")
    void envoyerMessageTest(){
        //préconditon
        connexion(false);
        creeTicket(false);

        allerDansMessage();
        envoyerMessage();
    }

    @Test
    @DisplayName("Scénario 1.7.1 Affecter un manager")
    void affecterManagerDifferentTest(){
        //précondition
        connexion(false);
        creeTicket(false);

        affecterManager(false);
    }

    @Test
    @DisplayName("Scénario 1.7.2 Affecter un manager (sois-même)")
    void affecterManagerSelfTest(){
        //précondition
        connexion(false);
        creeTicket(false);

        affecterManager(true);
    }




    private void connexion(boolean isStudent){
        String email = EMAIL_TEACHER_TEST;
        if(isStudent){
            email = EMAIL_STUDENT_TEST;
        }
        driver.navigate().to(frontUrl);
        Assertions.assertEquals(frontUrl+"/",driver.getCurrentUrl(), URL_ERROR);
        WebElement buttonGoToLogin = driver.findElement(By.id("btn-go-login"));
        buttonGoToLogin.click();
        wait.until(ExpectedConditions.urlToBe(frontUrl + "/login"));
        WebElement inputEmail = wait.until(
                ExpectedConditions.visibilityOfElementLocated(By.id("input-email"))
        );
        WebElement inputPassword = wait.until(
                ExpectedConditions.visibilityOfElementLocated(By.id("input-password"))
        );
        // action
        inputEmail.sendKeys(email);
        inputPassword.sendKeys(PASSWORD_TEST);
        WebElement btnConnexion = wait.until(
                ExpectedConditions.elementToBeClickable(By.id("btn-connexion"))
        );
        btnConnexion.click();
        //Verification
        wait.until(ExpectedConditions.urlToBe(frontUrl + "/issues"));
        Assertions.assertEquals(frontUrl + "/issues", driver.getCurrentUrl(), URL_ERROR);
        WebElement pageIssuesTitle = driver.findElement(By.xpath("//*[@id=\"__nuxt\"]/div/div/main/div/div[1]/h1"));
        Assertions.assertEquals(ISSUE_PAGE_TITLE,pageIssuesTitle.getText().trim(), "le titre de la page ne correspond pas");
    }

    private void deconnexion(){
        WebElement btnLogout = driver.findElement(By.id("btn-logout"));
        btnLogout.click();
        wait.until(ExpectedConditions.urlToBe(frontUrl + "/login"));
        WebElement inputEmail = wait.until(
                ExpectedConditions.visibilityOfElementLocated(By.id("input-email"))
        );
        WebElement inputPassword = wait.until(
                ExpectedConditions.visibilityOfElementLocated(By.id("input-password"))
        );

        Assertions.assertTrue(inputEmail.getText().isEmpty(),"le champ email est pas vide");
        Assertions.assertTrue(inputPassword.getText().isEmpty(),"le champ mdp est pas vide");
    }

    private void creeTicket(boolean cocherCase){
        WebElement btnCreeTicket = driver.findElement(By.id("btn-create-issue"));
        btnCreeTicket.click();
        wait.until(ExpectedConditions.urlToBe(frontUrl + "/issues/create"));
        WebElement createIssueTitle = driver.findElement(By.xpath("//*[@id=\"__nuxt\"]/div/div/main/div/div/div[1]/div/h1"));
        Assertions.assertEquals("Créer un ticket", createIssueTitle.getText().trim(), "le texte de la page ne correspond pas");

        WebElement issueTitle = driver.findElement(By.id("input-issue-title"));
        WebElement issueDescription = driver.findElement(By.id("input-issue-description"));
        WebElement issueCategory = driver.findElement(By.id("selector-issue-categorie"));
        Select menuCategory = new Select(issueCategory);
        WebElement caseAcocherIncomplet = driver.findElement(By.id("checkbox-issue-incomplet"));

        WebElement issueBtnCreate = driver.findElement(By.id("btn-issue-create"));

        issueTitle.sendKeys(TITRE_TICKET);
        issueDescription.sendKeys(DESCRIPTION);
        menuCategory.selectByIndex(1);
        if(cocherCase){
            caseAcocherIncomplet.click();
        }

        issueBtnCreate.click();

        wait.until(ExpectedConditions.urlToBe(frontUrl + "/issues"));

        WebElement nouveauTicket = wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        By.xpath("//*[contains(normalize-space(.), '" + TITRE_TICKET + "')]")
                )
        );

        WebElement btnCategorie = wait.until(
                ExpectedConditions.elementToBeClickable(By.xpath("//*[@id=\"__nuxt\"]/div/div/main/div/div[4]/div[1]/div/button"))
        );
        btnCategorie.click();



        WebElement statutNouveauTicket = wait.until(
                ExpectedConditions.visibilityOfElementLocated(By.id("issue-card-statut-" + TITRE_TICKET))
        );

        if(cocherCase){
            Assertions.assertEquals(STAUT_TICKET_INCOMPLET, statutNouveauTicket.getText().trim(), "le statut est pas bon");
        }else{
            Assertions.assertEquals(STATUT_TICKET_EN_ATTENTE,statutNouveauTicket.getText().trim(), "le statut est pas bon");
        }

        Assertions.assertTrue(nouveauTicket.isDisplayed(), "Le nouveau ticket n'apparaît pas dans la liste !");

    }


    private void creeCategorie(boolean isTeacher) {

        String idBtnCreation = isTeacher ? "create-categorie-teacher" : "create-categorie-student";
        String idInputName = isTeacher ? "categorie-name-teacher" : "categorie-name-student";
        String idInputSubject = isTeacher ? "categorie-subject-teacher" : "categorie-subject-student";
        String idInputDesc = "categorie-description-student";

        WebElement btnCreeCategorie = wait.until(
                ExpectedConditions.elementToBeClickable(By.id(idBtnCreation))
        );
        Actions actions = new Actions(driver);
        actions.moveToElement(btnCreeCategorie).click().perform();

        WebElement inputCategorieName = wait.until(
                ExpectedConditions.visibilityOfElementLocated(By.id(idInputName))
        );

        WebElement inputCategorieSubject = driver.findElement(By.id(idInputSubject));
        inputCategorieSubject.sendKeys(SUBJECT_CATEGORIE);

        inputCategorieName.sendKeys(TITRE_CATEGORIE);

        if (!isTeacher) {
            WebElement inputCategorieDescription = driver.findElement(By.id(idInputDesc));
            inputCategorieDescription.sendKeys(DESCRIPTION);
        }

        WebElement btnValidate = wait.until(
                ExpectedConditions.elementToBeClickable(By.id("validate-categorie"))
        );
        btnValidate.click();

        WebElement nouvelleCategorie = wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        By.xpath("//*[contains(text(), '" + TITRE_CATEGORIE + "')]")
                )
        );
        Assertions.assertTrue(nouvelleCategorie.isDisplayed(), "La nouvelle catégorie n'apparaît pas dans la liste !");

        WebElement proprietaireLabel = wait.until(
                ExpectedConditions.visibilityOfElementLocated(By.id("category-card-proprio-" + TITRE_CATEGORIE.replace(" ", "-"))));
        Assertions.assertEquals(CATEGORIE_PROPRIETAIRE, proprietaireLabel.getText().trim(), "Le titre de propriétaire n'est pas affiché");
    }

    private void allerPageCategories() {
        WebElement btnCategories = wait.until(
                ExpectedConditions.elementToBeClickable(By.id("navbar-categories"))
        );

        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", btnCategories);

        wait.until(ExpectedConditions.urlToBe(frontUrl + "/categories"));

        Assertions.assertEquals(frontUrl + "/categories", driver.getCurrentUrl(), "l'url vers la page catégorie n'est pas bonne");

        WebElement titlePage = wait.until(
                ExpectedConditions.visibilityOfElementLocated(By.xpath("//*[@id=\"__nuxt\"]/div/div/main/div/div[1]/h1"))
        );

        Assertions.assertEquals("Gestion des Catégories", titlePage.getText().trim(), "Le titre de la page catégorie n'est pas bon");
    }

    private void allerDansMessage(){
        WebElement statutNouveauTicket = wait.until(
                ExpectedConditions.visibilityOfElementLocated(By.id("issue-card-statut-" + TITRE_TICKET))
        );

        statutNouveauTicket.click();

        WebElement title = wait.until(
                ExpectedConditions.visibilityOfElementLocated(By.id("issue-title"))
        );
        Assertions.assertEquals(TITRE_TICKET, title.getText().trim(), "tu es pas sur la page de message");
    }

    private void envoyerMessage(){
        WebElement inputMessageZone = driver.findElement(By.id("input-message"));
        WebElement btnSendMessage = driver.findElement(By.id("btn_send-message"));

        inputMessageZone.sendKeys(MESSAGE);
        btnSendMessage.click();

        wait.until(ExpectedConditions.invisibilityOfElementLocated(By.id("empty-issue-message")));

        WebElement messageSend = driver.findElement(By.id("issue-message-"+MESSAGE));

        Assertions.assertEquals(MESSAGE, messageSend.getText().trim(), "le message a pas été envoyé");
    }

    private void affecterManager(boolean self){
        int choix = self ? 1 : 2;

        WebElement selectorTeacher = wait.until(
                ExpectedConditions.elementToBeClickable(By.id("selector-assignation-teacher"))
        );
        selectorTeacher.click();

        List<WebElement> options = wait.until(
                ExpectedConditions.visibilityOfAllElementsLocatedBy(By.cssSelector("[role='option']"))
        );
        options.get(choix).click();

        WebElement btnAffecterManager = driver.findElement(By.id("btn-affecter-manager"));
        btnAffecterManager.click();

        WebElement btnConfirmAssignation = wait.until(
                ExpectedConditions.elementToBeClickable(By.id("btn-confirm-assignation"))
        );
        btnConfirmAssignation.click();

        wait.until(ExpectedConditions.invisibilityOf(btnAffecterManager));

        WebElement emptyAssignation = wait.until(
                ExpectedConditions.visibilityOfElementLocated(By.id("empty-assignation"))
        );

        Assertions.assertEquals(EMPTY_ASSIGNATION, emptyAssignation.getText().trim(), "il y a encore des assignations");
    }
}
