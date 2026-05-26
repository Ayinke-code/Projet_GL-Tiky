package fr.horizon.backend_api.model.dto;
import lombok.Data;

@Data
public class InvitationResponseRequestDto {
    private Boolean response;
    private String raison;
}