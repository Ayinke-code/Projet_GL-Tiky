package fr.horizon.backend_api.model.dto;

import lombok.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IssueDTO {

    private String title;

    private String description;

    private Long categoryId;

    private List<MultipartFile> documents;
}
