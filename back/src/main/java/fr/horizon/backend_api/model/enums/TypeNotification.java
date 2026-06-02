package fr.horizon.backend_api.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum TypeNotification {
    // mise a jour d'une issue suivi
    UPDATING_ISSUE,
    // ajout d'un event pour les participant
    NEW_EVENT,
    // ajout d'une issue dans une catégorie pour le categoryOwner
    NEW_ISSUE,
    // action de fusion pour le manager
    MERGING,
    // mise a jour d'un event (date) pour les participants
    UPDATING_EVENT
}
