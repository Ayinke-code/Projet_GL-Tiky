package fr.horizon.backend_api.model.dto;

import fr.horizon.backend_api.model.enums.MergeRequestStatus;
import lombok.Data;

@Data
public class MergeRequestDto {
    private Long id;
    private Long sourceIssueId;
    private String sourceIssueTitle;
    private Long targetIssueId;
    private String targetIssueTitle;
    private Long requesterId;
    private MergeRequestStatus status;
    private String rejectionReason;
    private Boolean sourceManagerApproved;
    private Boolean targetManagerApproved;
    private Long sourceManagerId;
    private Long targetManagerId;
}