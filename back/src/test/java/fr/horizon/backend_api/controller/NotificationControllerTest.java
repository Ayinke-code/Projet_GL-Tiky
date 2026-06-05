package fr.horizon.backend_api.controller;

import fr.horizon.backend_api.service.JwtToken;
import fr.horizon.backend_api.model.dto.NotificationDto;
import fr.horizon.backend_api.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(NotificationController.class)
@AutoConfigureMockMvc(addFilters = false)
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NotificationService notificationService;

    @MockitoBean
    private JwtToken jwtToken;

    @Test
    void shouldMarkNotificationAsRead() throws Exception {
        doNothing().when(notificationService).markAsRead(1L);

        mockMvc.perform(put("/notifications/1/read"))
                .andExpect(status().isOk());

        verify(notificationService).markAsRead(1L);
    }

    @Test
    void shouldReturnUserNotifications() throws Exception {
        NotificationDto dto = new NotificationDto();
        when(notificationService.getUserNewNotification(2L)).thenReturn(List.of(dto));

        mockMvc.perform(get("/notifications/user/2"))
                .andExpect(status().isOk());

        verify(notificationService).getUserNewNotification(2L);
    }
}
