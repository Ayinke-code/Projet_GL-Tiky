package fr.horizon.backend_api.model.dto;

import fr.horizon.backend_api.model.enums.RelationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OfficialLinkRequestDto {
    private Long id;
    private Long sourceIssueId;
    private String sourceIssueTitle;
    private Long targetIssueId;
    private String targetIssueTitle;
    private RelationType type;
}