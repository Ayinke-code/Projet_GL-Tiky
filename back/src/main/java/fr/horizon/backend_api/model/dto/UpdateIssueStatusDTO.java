package fr.horizon.backend_api.model.dto;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateIssueStatusDTO {
    private String status;
}
