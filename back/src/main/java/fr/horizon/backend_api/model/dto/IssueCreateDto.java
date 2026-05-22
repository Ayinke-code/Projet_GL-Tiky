package fr.horizon.backend_api.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IssueCreateDto {
    private String title;
    private String description;
    private Long categoryId;
    private boolean pendingCase;
}
