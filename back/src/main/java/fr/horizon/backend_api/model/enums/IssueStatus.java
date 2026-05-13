package fr.horizon.backend_api.model.enums;

public enum IssueStatus {
    OPEN,
    COMPLETED,
    INCOMPLETE,
    FINISHED;

    public static IssueStatus fromValue(String value) {
        if (value == null) {
            return OPEN;
        }
        try {
            return IssueStatus.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return OPEN; // Default value
        }
    }
}
