package fr.horizon.backend_api.model.dto;

import fr.horizon.backend_api.model.enums.IssueStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class IssueDto {
    private Long id;
    private String title;
    private String description;
    private IssueStatus status;
    private boolean isPrivate;
    private String rejectType;
    private String rejectReason;
    private String rejectComment;
    // Catégorie
    private Long categoryId;
    private String categoryName;
    // Createur
    private Long createdId;
    private String createdName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
