package fr.horizon.backend_api.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IssueResponseDto {
    @JsonProperty("id")
    private Long idTick;

    private String titre;

    private String description;

    private String statut;

    private boolean estPrive;

    private LocalDateTime dateCreation;

    private LocalDateTime dateModification;

    @JsonProperty("categorie")
    private CategoryNameDto category;

    @JsonProperty("auteur")
    private UserNameDto createdBy;

    @JsonProperty("professeur")
    private UserNameDto manager;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryNameDto {
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
    public static class UserNameDto {
        private Long id;
        private String name;
        private String email;
        private String role;
        private java.util.Set<String> roles;
    }
}