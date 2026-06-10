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
    NEW_UNFOLLOW,
    // demande de retrait d'une catégorie par un professeur
    CATEGORY_REMOVAL_REQUEST,
    // ajout d'un professeur à une catégorie
    ADDED_TO_CATEGORY
    NEW_UNFOLLOW,
    // utilsaiteur atteigne 5 liens => demande officiel
    SUBSCRIBERS_NEW_LINK,
    // Manager demade de lien a un autre manager
    MANAGER_LINK_REQUEST,
    // notifier les followers de l'issue d'un lien officiel
    FOLLOWERS_LINK,
    // notifier le manager lorsque la demande est accepté
    MANAGER_RESPONSE_LINK_REQUEST,
    // notifie le manager lorsqu'un autre manager veut fusionner
    MERGE_REQUEST_MANAGER,
    //notifie le manager d'un refus de fusion
    MERGE_REQUEST_DENIED,
    //notifie le manager d'une acceptation de fusion
    MERGE_REQUEST_ACCEPTED,
    // notifie les followers
    MERGE_REQUEST_FOLLOWERS,
    // notifier un teacher qu'un manager l'a rajoute dans sa categorie
    MANAGER_ADDING_TEACHER_CATEGORY,
    // notifier qu'un teacher veuille partir de la category
    TEACHER_QUIT_CATEGORY,
    // notifier qu'un category owner a accepté le départ
    ACCEPTED_TEACHER_QUIT_CATEGORY,
    // notifier qu'un category owner a refusé le départ
    DENIED_TEACHER_QUIT_CATEGORY,
    // catégorie supprimé
    DELETING_CATEGORY

}
