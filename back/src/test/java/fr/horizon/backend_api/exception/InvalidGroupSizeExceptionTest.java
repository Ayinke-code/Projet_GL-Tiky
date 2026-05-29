package fr.horizon.backend_api.exception;

import fr.horizon.backend_api.model.enums.GroupType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class InvalidGroupSizeExceptionTest {

    @Test
    void shouldBuildCorrectMessageForOptionType() {
        InvalidGroupSizeException ex = new InvalidGroupSizeException(GroupType.OPTION, 24, 10);

        assertEquals(
                "Un groupe de type OPTION doit contenir au minimum 24 étudiant(s). Vous en avez sélectionné 10.",
                ex.getMessage()
        );
    }

    @Test
    void shouldBuildCorrectMessageForProjectType() {
        InvalidGroupSizeException ex = new InvalidGroupSizeException(GroupType.PROJECT, 2, 1);

        assertEquals(
                "Un groupe de type PROJECT doit contenir au minimum 2 étudiant(s). Vous en avez sélectionné 1.",
                ex.getMessage()
        );
    }

    @Test
    void shouldExtendRuntimeException() {
        InvalidGroupSizeException ex = new InvalidGroupSizeException(GroupType.OPTION, 24, 5);

        assertInstanceOf(RuntimeException.class, ex);
    }
}
