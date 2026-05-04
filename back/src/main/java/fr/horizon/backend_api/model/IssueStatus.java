package fr.horizon.backend_api.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum IssueStatus {
    REFUSE("Refusé"),
    INCOMPLETE("Incomplet"),
    VALIDE("Validé");

    private final String label;

    @Override
    public String toString() {
        return label;
    }
}
