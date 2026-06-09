package fr.horizon.backend_api.model.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DeletionCategoryRequestDto {
        private Long id;
        private Long requesterId;
        private Long validatorId;
        private Long categoryId;
        private String status;
        private String rejectionReason;
}
