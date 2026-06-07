package fr.horizon.backend_api.model.enums;

/**
 * Statut d'un lien entre deux issues.
 * PENDING   : en attente de validation du second manager
 * OFFICIAL  : validé par les deux managers (ou même manager pour les deux issues)
 * REJECTED  : refusé par le second manager
 */
public enum RelationStatus {
    PENDING,
    OFFICIAL,
    REJECTED
}