package fr.horizon.backend_api.model.enums;

public enum IssueStatus {
    OUVERT,
    EN_ATTENTE,
    INCOMPLET,
    TERMINE,
    DEMANDE_REOUVERTURE;

    public static IssueStatus fromValue(String value) {
        if (value == null) {
            return EN_ATTENTE;
        }
        try {
            return IssueStatus.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return EN_ATTENTE; // Default value
        }
    }
}

