export const useAssignation = () => {
  const api = useApi()

  return {
    // Récupère les tickets en attente d'une première assignation (Alertes)
    getOwnerAlerts: (userId) => api(`issues/demandes-manager/owner-alerts`, {params: {userId}}),

    // Récupère les assignations qu'un professeur doit valider
    getTeacherAssignments: (userId) => api(`issues/demandes-manager/teacher`, {params: {userId}}),

    // Récupère l'historique complet pour un Category Owner (Acceptés, Refusés, En attente avec cible)
    getHistory: (userId) => api(`issues/demandes-manager/history`, {params: {userId}}),

    // Liste tous les professeurs disponibles pour l'assignation
    getTeachersList: () => api(`/users/teachers`),

    // Envoie une demande d'assignation à un professeur spécifique
    assignTeacher: (issueId, teacherId) =>
      api(`issues/assign-manager/issue/${issueId}/target-user/${teacherId}`, {
        method: 'POST'
      }),

    // Permet à un professeur de répondre (Accept/Deny) à une demande
    respondToRequest: (demandeId, accept, userId) =>
      api(`issues/demandes-manager/${demandeId}/respond`, {
        method: 'PUT',
        params: {accept, userId}
      }),

    // Marque une issue comme incomplète et refuse la demande associée
    markIssueAsIncomplete: (issueId) =>
      api(`issues/${issueId}/mark-incomplete`, {
        method: 'PUT'
      }),


    // Récupère la liste des issues incomplètes pour un utilisateur (pour renvoi)
    // A utiliser au chargement pour l'user afin de récupérer ses demandes qu'il doit completer
    getIncompleteIssues: (userId) =>
      api(`issues/incomplete/${userId}`, {
        method: 'GET'
      }),

    getIssueById: (issueId)=>
      api(`issues/dto/${issueId}`, {
        method: 'GET'
      })
  }

}
