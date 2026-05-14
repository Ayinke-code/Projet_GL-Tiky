package fr.horizon.backend_api.model.dto;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequestManagerDto {
    String issueTitle;
    LocalDateTime creationDate;
    LocalDateTime processingDate;
    String status;
    String target;

}
