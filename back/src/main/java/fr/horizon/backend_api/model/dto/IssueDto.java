package fr.horizon.backend_api.model.dto;

import fr.horizon.backend_api.model.enums.IssueStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IssueDto {
    private Long id;
    private String title;
    private String description;
    private IssueStatus status;
    private boolean isPrivate;
    private String rejectType;
    private String rejectReason;
    private String rejectComment;
    // Catégorie owner
    private Long categoryId;
    private String categoryName;
    private Long categoryOwnerId;
    private String categoryOwnerName;
    // Catégorie manager
    private Long categoryManagerId;
    private String categoryManagerName;
    // Createur
    private Long createdId;
    private String createdName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    //teacher manager
    private String teacherManagerName;
    //follower
    private int followersCount;
}
