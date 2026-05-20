package fr.horizon.backend_api.model.enums;

public enum RoleEnum {
    ADMIN,
    TEACHER,
    STUDENT;

    /**
     * Retourne la représentation String du rôle.
     *
     * Utile pour :
     * - compatibilité avec Spring Security (hasAuthority)
     * - stockage / comparaison en base de données
     *
     * @return nom du rôle sous forme de String
     */
    public String value() {
        return name();
    }
}
