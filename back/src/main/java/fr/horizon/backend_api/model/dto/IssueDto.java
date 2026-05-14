package fr.horizon.backend_api.model.dto;

import java.time.LocalDateTime;
import fr.horizon.backend_api.model.enums.IssueStatus;
import lombok.Builder;
import lombok.Data;
import lombok.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

@Getter
@Setter
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
    // Catégorie
    private Long categoryId;

    private List<MultipartFile> documents;
    private String categoryName;
    private Long categoryOwnerId;
    private String categoryOwnerName;
    // Createur
    private Long createdId;
    private String createdName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
