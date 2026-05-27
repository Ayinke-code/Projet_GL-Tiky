package fr.horizon.backend_api.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventMockTest {

    private Event event;

    @Mock
    private User mockUser;

    @Mock
    private EventParticipant mockParticipant;

    @BeforeEach
    void setUp() {
        event = Event.builder()
                .id(1L)
                .name("Réunion de cadrage")
                .participants(new HashSet<>())
                .build();
    }

    @Test
    @DisplayName("Ajout d'un participant")
    void shouldAddParticipant_usingMockedUser() {
        // Precondition
        event.addParticipant(mockUser, true);

        // Action
        assertThat(event.getParticipants()).hasSize(1);

        // RetroAction
        verifyNoInteractions(mockUser);
    }

    @DisplayName("Suppression d'un participant qui est dans la liste")
    @Test
    void shouldDeleteParticipant_whenUserMatchesMockedId() {
        // Mock
        when(mockUser.getId()).thenReturn(42L);
        when(mockParticipant.getUser()).thenReturn(mockUser);

        event.getParticipants().add(mockParticipant);

        // Action
        event.deleteParticipant(mockUser);

        // Assertion
        assertThat(event.getParticipants()).isEmpty();

        // RetroAction

        verify(mockParticipant, times(1)).getUser();

        verify(mockParticipant, times(1)).setEvent(null);

        verify(mockUser, times(2)).getId();
    }

    @DisplayName("Suppression d'un user qui n'est pas dans la liste des participants")
    @Test
    void shouldNotDeleteParticipant_whenUserDoesNotMatchMockedId() {
        // Mock
        User anotherUser = mock(User.class);
        when(anotherUser.getId()).thenReturn(99L);

        when(mockUser.getId()).thenReturn(42L);
        when(mockParticipant.getUser()).thenReturn(mockUser);

        event.getParticipants().add(mockParticipant);

        // Action
        event.deleteParticipant(anotherUser);

        // Assertion
        assertThat(event.getParticipants()).hasSize(1);

        // RetroAction

        verify(mockParticipant, times(1)).getUser();

        verify(mockUser, times(1)).getId();
        verify(anotherUser, times(1)).getId();

        verify(mockParticipant, never()).setEvent(null);
    }
}