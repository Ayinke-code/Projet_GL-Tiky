package fr.horizon.backend_api.model.dto;

import java.time.LocalDateTime;

public record EventResponseDto(
    Long id,
    String title,
    LocalDateTime start,
    LocalDateTime end,
    String description,
    String statut,
    String color
) {}