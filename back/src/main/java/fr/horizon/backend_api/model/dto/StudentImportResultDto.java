package fr.horizon.backend_api.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class StudentImportResultDto {
    private int created;
    private int alreadyExisted;
    private int skipped;
}
