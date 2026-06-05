package fr.horizon.backend_api.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum TypeNotification {
    // Validation d'un ticket
    VALIDE_ISSUE,
    // Création d'un ticket dans une catégorie
    NEW_ISSUE_IN_CATEGORIE,
    // Réponse d'une demande de manager
    RESPONSE_TO_MANAGER_ISSUE,
    // MIS EN INCOMPLET D'un ticket
    INCOMPLETE_ISSUE,
    // RECEPTION demande de manager
    NEW_DEMANDE_MANAGER,
    // ajout d'un event pour les participant
    NEW_EVENT_INVITATION,
    // modification de l'heure de rdv pour un event
    UPDATE_EVENT,
    // réponse d'un invité à un évènement
    EVENT_RESPONSE,
    // Annulation d'un évènement
    EVENT_CANCEL,
    // action de fusion pour le manager
    MERGING,
    // demande de reouverture pour le manager
    NEW_OPEN_ISSUE,
    // réouverture par le manager
    OPEN_ISSUE,
    // acceptation de la réouverture du ticket pour le créateur
    ACCEPTED_OPEN_ISSUE,
    // ticket cloturé
    CLOSING_ISSUE,
    // nouveaux follower
    NEW_FOLLOWER,
    // desabonnement follower
    NEW_UNFOLLOW
}
