package fr.horizon.backend_api.model;

import java.time.LocalDate;

import fr.horizon.backend_api.model.enums.CategoryStatus;
import jakarta.persistence.*;
import lombok.*;

@Data
@Entity
@Table(name = "event")

public class Event {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id_evt;
    private String title;
    private LocalDate start_date;
    private LocalDate end_date;
    private String description;
    private String statut;
    private String color; // Pour le style du calendrier

}
