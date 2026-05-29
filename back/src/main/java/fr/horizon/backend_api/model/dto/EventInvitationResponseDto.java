
package fr.horizon.backend_api.model.dto;

import fr.horizon.backend_api.model.enums.CategoryType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventInvitationResponseDto {
    private Long id;
    private String name;
    private String description;
    private CategoryType categoryType;
    private String categoryName;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String participantName;
    private Long participantId;
    private String reason;
}
