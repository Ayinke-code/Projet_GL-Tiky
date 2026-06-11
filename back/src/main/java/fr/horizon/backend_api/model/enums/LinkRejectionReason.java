package fr.horizon.backend_api.model.enums;

public enum LinkRejectionReason {
    DUPLICATE_RELATION,      // Un lien similaire existe déjà ou la relation est redondante
    IRRELEVANT,              // Les tickets n'ont pas de lien réel
    WRONG_RELATION_TYPE,     // Le type suggéré est incorrect
    OTHER                    // Autre raison
}
