package fr.horizon.backend_api.exception;

import fr.horizon.backend_api.model.enums.GroupType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StudentAlreadyInGroupExceptionTest {

    @Test
    void shouldBuildCorrectMessageForOptionType() {
        StudentAlreadyInGroupException ex = new StudentAlreadyInGroupException("Alice Dupont", GroupType.OPTION);

        assertEquals(
                "L'étudiant \"Alice Dupont\" est déjà dans un groupe de type OPTION",
                ex.getMessage()
        );
    }

    @Test
    void shouldBuildCorrectMessageForProjectType() {
        StudentAlreadyInGroupException ex = new StudentAlreadyInGroupException("Bob Martin", GroupType.PROJECT);

        assertEquals(
                "L'étudiant \"Bob Martin\" est déjà dans un groupe de type PROJECT",
                ex.getMessage()
        );
    }

    @Test
    void shouldExtendRuntimeException() {
        StudentAlreadyInGroupException ex = new StudentAlreadyInGroupException("Test", GroupType.OPTION);

        assertInstanceOf(RuntimeException.class, ex);
    }
}
