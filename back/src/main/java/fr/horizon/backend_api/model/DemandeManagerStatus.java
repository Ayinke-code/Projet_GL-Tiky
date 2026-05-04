package fr.horizon.backend_api.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum DemandeManagerStatus {
    DENIED("Refusé"),
    PENDING("En attente"),
    ACCEPTED("Validé");

    private final String label;

    @Override
    public String toString() {
        return label;
    }
}
