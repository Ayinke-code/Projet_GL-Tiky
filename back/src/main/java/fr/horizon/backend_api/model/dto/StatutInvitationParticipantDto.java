package fr.horizon.backend_api.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatutInvitationParticipantDto {
    private String participantName;
    private String participantRole;
    private Boolean isPresent;
    private String reason;
}
