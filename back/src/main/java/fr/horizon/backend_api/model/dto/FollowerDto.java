package fr.horizon.backend_api.model.dto;

import fr.horizon.backend_api.model.enums.FollowedReason;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FollowerDto {
    private Long id;
    private FollowedReason reason;
    private Long issueId;
    private Long userId;
    private String userName;
}
