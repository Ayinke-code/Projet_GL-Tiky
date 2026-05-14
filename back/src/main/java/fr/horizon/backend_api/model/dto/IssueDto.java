package fr.horizon.backend_api.model.dto;

import lombok.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IssueDto {

    private String titre;

    private String description;

    private Long categorieId;

    private List<MultipartFile> documents;
}