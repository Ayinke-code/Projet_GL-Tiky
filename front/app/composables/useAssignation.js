export const useAssignation = () => {
  const api = useApi()

  return {
    // Récupère les tickets en attente d'une première assignation (Alertes)
    getOwnerAlerts: (userId) => api(`issues/demandes-manager/owner-alerts`, { params: { userId } }),

    // Récupère les assignations qu'un professeur doit valider
    getTeacherAssignments: (userId) => api(`issues/demandes-manager/teacher`, { params: { userId } }),

    // Récupère l'historique complet pour un Category Owner (Acceptés, Refusés, En attente avec cible)
    getHistory: (userId) => api(`issues/demandes-manager/history`, { params: { userId } }),

    // Liste tous les professeurs disponibles pour l'assignation
    getTeachersList: (categoryId) => api(`users/teachers/${categoryId}`),

    // Envoie une demande d'assignation à un professeur spécifique
    assignTeacher: (issueId, teacherId) =>
      api(`issues/assign-manager/issue/${issueId}/target-user/${teacherId}`, {
        method: 'POST'
      }),

    // Permet à un professeur de répondre (Accept/Deny) à une demande
    respondToRequest: (demandeId, accept, userId) =>
      api(`issues/demandes-manager/${demandeId}/respond`, {
        method: 'PUT',
        params: { accept, userId }
      }),

    // Récupère la liste des issues incomplètes pour un utilisateur (pour renvoi)
    // A utiliser au chargement pour l'user afin de récupérer ses demandes qu'il doit completer
    getIncompleteIssues: (userId) =>
      api(`issues/incomplete/${userId}`, {
        method: 'GET'
      }),

    getIssueDtoById: (issueId)=>
      api(`issues/dto/${issueId}`, {
        method: 'GET'
      }),

    // Marque une issue comme incomplète et refuse la demande associée
    markIssueAsIncomplete: (issueId, commentaire, raison) =>
      api(`issues/${issueId}/mark-incomplete`, {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({
          commentaire: commentaire,
          raison: raison
        })
      }),

    // Mise a jour d'une issue suite a un sigalement comme incomplet
    updateIssueWhenIncomplete: (id, issueData) =>
      api(`issues/update/incomplete/${id}`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        // On transforme l'objet contenant les nouvelles données en chaîne JSON
        body: JSON.stringify(issueData)
      })


  }
}
