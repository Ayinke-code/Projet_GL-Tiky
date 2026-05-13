package fr.horizon.backend_api.model.dto;

import lombok.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IssueDTO {

    private String titre;

    private String description;

    private Long categoryId;

    private String category;

    private List<MultipartFile> documents;
    private String creator;
}
