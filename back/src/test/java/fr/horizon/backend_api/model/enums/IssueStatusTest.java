package fr.horizon.backend_api.model.enums;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class IssueStatusTest {

    @Test
    void shouldReturnEnumWhenValidValue() {

        assertEquals(IssueStatus.OUVERT, IssueStatus.fromValue("OUVERT"));
        assertEquals(IssueStatus.COMPLET, IssueStatus.fromValue("COMPLET"));
        assertEquals(IssueStatus.INCOMPLET, IssueStatus.fromValue("INCOMPLET"));
        assertEquals(IssueStatus.TERMINE, IssueStatus.fromValue("TERMINE"));
    }

    @Test
    void shouldBeCaseInsensitive() {

        assertEquals(IssueStatus.TERMINE, IssueStatus.fromValue("termine"));
        assertEquals(IssueStatus.COMPLET, IssueStatus.fromValue("complet"));
    }

    @Test
    void shouldReturnDefaultWhenNull() {

        assertEquals(IssueStatus.OUVERT, IssueStatus.fromValue(null));
    }

    @Test
    void shouldReturnDefaultWhenInvalidValue() {

        assertEquals(IssueStatus.OUVERT, IssueStatus.fromValue("INVALID"));
    }
}
