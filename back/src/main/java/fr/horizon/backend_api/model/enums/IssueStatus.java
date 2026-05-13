package fr.horizon.backend_api.model.enums;

public enum IssueStatus {
    OUVERT,
    COMPLET,
    INCOMPLET,
    TERMINE;

    public static IssueStatus fromValue(String value) {
        if (value == null) {
            return OUVERT;
        }
        try {
            return IssueStatus.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return OUVERT; // Default value
        }
    }
}

