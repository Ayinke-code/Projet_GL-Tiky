package fr.horizon.backend_api.model.dto;

import lombok.*;
import org.springframework.cglib.core.Local;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DemandeManagerDTO {
    String titreIssue;
    LocalDateTime dateCreation;
    LocalDateTime dateTraitement;
    String statut;
    String cible;

}
