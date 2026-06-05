package fr.horizon.backend_api.model.dto;

import fr.horizon.backend_api.model.enums.TypeNotification;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDto {

    private Long id;
    private TypeNotification type;
    private boolean isRead;
    private String content;
    private Long issueId;
    private Long eventId;
    private String eventCategory;
    private LocalDateTime sendingHour;
    private Long requestManagerId;
}