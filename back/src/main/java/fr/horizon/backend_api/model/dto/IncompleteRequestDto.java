package fr.horizon.backend_api.model.dto;


import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class IncompleteRequestDto {
    private String raison;
    private String commentaire;
}