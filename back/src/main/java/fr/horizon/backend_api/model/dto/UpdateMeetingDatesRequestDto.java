package fr.horizon.backend_api.model.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UpdateMeetingDatesRequestDto {
    private LocalDateTime startDate;
    private LocalDateTime endDate;
}
