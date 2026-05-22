package fr.horizon.backend_api.model;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;

@Data
@Entity
@Table(name = "events")

public class Event {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String title;
    @JsonProperty("start") // Sera envoyé comme "start" au front
    private LocalDateTime start_date;
    @JsonProperty("end") // Sera envoyé comme "end" au front
    private LocalDateTime end_date;
    private String description;
    private String statut;
    private String color; // Pour le style du calendrier

}
