package fr.horizon.backend_api.exception;

import fr.horizon.backend_api.model.enums.GroupType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    void handleInvalidGroupSize_returns422() {
        InvalidGroupSizeException ex = new InvalidGroupSizeException(GroupType.OPTION, 24, 3);
        ResponseEntity<Map<String, String>> resp = handler.handleInvalidGroupSize(ex);
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, resp.getStatusCode());
        assertNotNull(resp.getBody());
        assertTrue(resp.getBody().containsKey("error"));
    }

    @Test
    void handleStudentAlreadyInGroup_returns409() {
        StudentAlreadyInGroupException ex = new StudentAlreadyInGroupException("Alice", GroupType.OPTION);
        ResponseEntity<Map<String, String>> resp = handler.handleStudentAlreadyInGroup(ex);
        assertEquals(HttpStatus.CONFLICT, resp.getStatusCode());
        assertNotNull(resp.getBody());
        assertTrue(resp.getBody().containsKey("error"));
    }

    @Test
    void handleUserNotFound_returns404() {
        UserNotFoundException ex = new UserNotFoundException("unknown@test.fr");
        ResponseEntity<Map<String, String>> resp = handler.handleUserNotFound(ex);
        assertEquals(HttpStatus.NOT_FOUND, resp.getStatusCode());
        assertNotNull(resp.getBody());
        assertTrue(resp.getBody().containsKey("error"));
    }

    @Test
    void handleEventNotFound_returns404() {
        EventNotFoundException ex = new EventNotFoundException(99L);
        ResponseEntity<Map<String, String>> resp = handler.handleEventNotFound(ex);
        assertEquals(HttpStatus.NOT_FOUND, resp.getStatusCode());
        assertNotNull(resp.getBody());
        assertTrue(resp.getBody().containsKey("error"));
    }

    @Test
    void handleStudentGroupNotFound_returns404() {
        StudentGroupNotFoundException ex = new StudentGroupNotFoundException(42L);
        ResponseEntity<Map<String, String>> resp = handler.handleStudentGroupNotFound(ex);
        assertEquals(HttpStatus.NOT_FOUND, resp.getStatusCode());
        assertNotNull(resp.getBody());
        assertTrue(resp.getBody().containsKey("error"));
    }

    @Test
    void handleIllegalArgument_returns400() {
        IllegalArgumentException ex = new IllegalArgumentException("date invalide");
        ResponseEntity<Map<String, String>> resp = handler.handleIllegalArgument(ex);
        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
        assertNotNull(resp.getBody());
        assertEquals("date invalide", resp.getBody().get("error"));
    }

    @Test
    void handleEventConflict_returns409() {
        EventConflictException ex = new EventConflictException("Conflit détecté pour : Alice");
        ResponseEntity<Map<String, String>> resp = handler.handleEventConflict(ex);
        assertEquals(HttpStatus.CONFLICT, resp.getStatusCode());
        assertNotNull(resp.getBody());
        assertEquals("Conflit détecté pour : Alice", resp.getBody().get("error"));
    }

    @Test
    void handleAccessDenied_returns403() {
        AccessDeniedException ex = new AccessDeniedException("Accès refusé");
        ResponseEntity<Map<String, String>> resp = handler.handleAccessDenied(ex);
        assertEquals(HttpStatus.FORBIDDEN, resp.getStatusCode());
        assertNotNull(resp.getBody());
        assertEquals("Accès refusé", resp.getBody().get("error"));
    }
}
