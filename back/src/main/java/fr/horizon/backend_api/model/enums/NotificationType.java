package fr.horizon.backend_api.model.enums;

/**
 * Types d'actions pouvant déclencher une notification.
 */
public enum NotificationType {
    /** Nouveau message posté sur l'issue */
    NEW_MESSAGE,
    /** Statut de l'issue mis à jour */
    STATUS_UPDATED,
    /** Visibilité de l'issue modifiée */
    VISIBILITY_UPDATED
}