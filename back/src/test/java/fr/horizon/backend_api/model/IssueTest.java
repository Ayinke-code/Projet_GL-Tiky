package fr.horizon.backend_api.model;

import fr.horizon.backend_api.model.enums.IssueStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class IssueTest {

    @Test
    void shouldCreateIssueWithBuilder() {

        Category category = Category.builder()
                .name("IT")
                .subject("Réseau")
                .build();

        User user = new User();
        user.setEmail("test@test.com");

        Issue issue = Issue.builder()
                .title("Panne réseau")
                .description("Internet down")
                .category(category)
                .createdBy(user)
                .build();

        assertEquals("Panne réseau", issue.getTitle());
        assertEquals("Internet down", issue.getDescription());

        // valeurs par défaut Lombok
        assertEquals(IssueStatus.EN_ATTENTE, issue.getStatus());
        assertFalse(issue.isPrivate());

        // champs optionnels
        assertNull(issue.getRejectType());
        assertNull(issue.getRejectReason());
        assertNull(issue.getRejectComment());

        // relations
        assertNotNull(issue.getCategory());
        assertNotNull(issue.getCreatedBy());
    }
}