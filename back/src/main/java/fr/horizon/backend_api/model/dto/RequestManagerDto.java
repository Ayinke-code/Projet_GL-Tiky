package fr.horizon.backend_api.model.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RequestManagerDto {
    private Long id;
    private Long issueId;
    private String issueTitle;
    private LocalDateTime creationDate;
    private LocalDateTime processingDate;
    private String status;
    private String target;
    private String creator;

}
