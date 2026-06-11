package fr.horizon.backend_api.model.enums;

/**
 * Enumération des raisons pour lesquelles un utilisateur peut suivre une issue.
 * La raison choisie détermine les droits d'interaction de l'utilisateur sur l'issue.
 */
public enum FollowedReason {
    /** Consultation uniquement - l'utilisateur est intéressé par l'issue */
    INTERESTED,

    /** Interaction autorisée - l'utilisateur peut aider à résoudre l'issue */
    ABLE_TO_HELP,

    /** Interaction autorisée - l'utilisateur est également impacté par l'issue */
    IMPACTED,

    IS_MANAGER
}