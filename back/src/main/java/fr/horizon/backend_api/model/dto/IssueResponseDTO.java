package fr.horizon.backend_api.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IssueResponseDTO {
    @JsonProperty("id")
    private Long idTick;

    private String titre;

    private String description;

    private String statut;

    private boolean estPrive;

    private LocalDateTime dateCreation;

    private LocalDateTime dateModification;

    @JsonProperty("categorie")
    private CategoryNameDTO category;

    @JsonProperty("auteur")
    private UserNameDTO createdBy;

    @JsonProperty("professeur")
    private UserNameDTO manager;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryNameDTO {
        private Long id;
        private String name;
        private String nom;
        private String subject;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserNameDTO {
        private Long id;
        private String name;
        private String email;
    }
}

