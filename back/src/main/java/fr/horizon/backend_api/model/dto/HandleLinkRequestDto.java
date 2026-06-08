package fr.horizon.backend_api.model.dto;

import fr.horizon.backend_api.model.enums.LinkRejectionReason;
import lombok.Data;

@Data
public class HandleLinkRequestDto {
    private boolean approve;
    private LinkRejectionReason rejectionReason;
}