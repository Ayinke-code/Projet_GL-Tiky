package fr.horizon.backend_api.model.dto;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IssueDTO {

    private String titre;

    private String description;

    private Long categorieId;
}
