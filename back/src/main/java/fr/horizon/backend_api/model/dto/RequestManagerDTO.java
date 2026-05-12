package fr.horizon.backend_api.model.dto;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RequestManagerDTO {
    Long id;
    String issueTitle;
    LocalDateTime creationDate;
    LocalDateTime processingDate;
    String status;
    String target;
    String creator;

}
