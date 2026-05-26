/**
 * Composable de gestion des événements.
 *
 * Fournit :
 * - fetchCategoriesByType : récupère les catégories filtrées par type (AUDIT/DEFENSE)
 *   → utilisé à l'étape 2 du formulaire de création
 * - createEvent : POST /events — restreint TEACHER/ADMIN côté backend
 * - fetchEventsByType : GET /events/type/{type} — alimente la liste selon l'onglet actif
 */
export const useEvents = () => {
  const api = useApi()
  const { user } = useAuth()

  // État global partagé entre toutes les instances du composable
  const events = useState('events', () => [])
  const myPendingInvitations = useState('myPendingInvitations', () => [])
  const error = ref(null)

  /**
   * Récupère les catégories filtrées par type d'événement.
   * Appelé quand l'utilisateur choisit AUDIT ou DEFENSE dans EventForms.
   *
   * @param type - 'AUDIT' | 'DEFENSE'
   * @returns liste de CategoryDto avec eventType correspondant
   */
  const fetchCategoriesByType = async (type) => {
    return await api(`/categories/type/${type}`)
  }

  /**
   * Crée un nouvel événement.
   * Le backend dérive le eventType depuis la catégorie choisie.
   * Lève une AccessDeniedException (403) si l'user n'est pas TEACHER/ADMIN.
   *
   * @param payload - { name, description, categoryId }
   */
  const createEvent = async (payload) => {
    error.value = null
    if (!user?.value?.id) {
      await navigateTo('/login')
      return
    }
    try {
      return await api('/events', { method: 'POST', body: payload })
    } catch (e) {
      error.value = e?.data?.message || 'CREATE_EVENT_ERROR'
      throw e
    }
  }

  /**
   * Récupère les événements par type et met à jour l'état global.
   * Appelé au montage de la page et lors du changement d'onglet.
   *
   * @param type - 'AUDIT' | 'DEFENSE'
   */
  const fetchEventsByType = async (type) => {
    try {
      const data = await api(`/events/type/${type}`)
      events.value = data
      return data
    } catch (e) {
      error.value = e?.data?.message || 'FETCH_EVENTS_ERROR'
      return []
    }
  }

  const deleteEvent = async (id) => {
    await api(`/events/${id}`, { method: 'DELETE' })
    events.value = events.value.filter(e => e.id !== id)
  }

/**
* Invite un utilisateur spécifique à un événement.
  *
  * @param eventId - L'ID de l'événement
  * @param userId - L'ID de l'utilisateur à inviter
  */
  const inviteUserToEvent = async (eventId, userId) => {
    error.value = null
    try {
      return await api(`/events/${eventId}/invitations`, {
        method: 'POST',
        body: { userId }
      })
    } catch (e) {
      error.value = e?.data?.message || 'INVITE_USER_ERROR'
      throw e
    }
  }

  /**
   * Invite un groupe d'utilisateurs à un événement.
   *
   * @param eventId - L'ID de l'événement
   * @param userIds - Un tableau d'IDs d'utilisateurs [1, 2, 3]
   */
  const inviteUsersToEvent = async (eventId, userIds) => {
    error.value = null
    try {
      return await api(`/events/${eventId}/invitations/bulk`, {
        method: 'POST',
        body: { userIds }
      })
    } catch (e) {
      error.value = e?.data?.message || 'INVITE_USERS_BULK_ERROR'
      throw e
    }
  }

  /**
   * Permet à l'utilisateur connecté de répondre à une invitation.
   *
   * @param eventId - L'ID de l'événement
   * @param response - true (accepte) ou false (refuse)
   * @param raison - Chaîne de caractères optionnelle si refus
   */
  const respondToInvitation = async (eventId, response, raison = null) => {
    error.value = null
    try {
      const result = await api(`/events/${eventId}/invitations/respond`, {
        method: 'PUT',
        body: { response, raison }
      })
      // Rafraîchir la liste des invitations après avoir répondu
      await fetchMyInvitations()
      return result
    } catch (e) {
      error.value = e?.data?.message || 'RESPOND_INVITATION_ERROR'
      throw e
    }
  }

  /**
   * Récupère le statut de toutes les invitations d'un événement (réservé au créateur).
   *
   * @param eventId - L'ID de l'événement
   */
  const fetchEventInvitationStatus = async (eventId) => {
    error.value = null
    try {
      return await api(`/events/${eventId}/invitations/status`)
    } catch (e) {
      error.value = e?.data?.message || 'FETCH_INVITATION_STATUS_ERROR'
      throw e
    }
  }

  /**
   * Récupère la liste des invitations en attente de l'utilisateur connecté
   * et met à jour l'état global myPendingInvitations.
   */
  const fetchMyInvitations = async () => {
    error.value = null
    try {
      const data = await api(`/events/invitations/me`)
      myPendingInvitations.value = data
      return data
    } catch (e) {
      error.value = e?.data?.message || 'FETCH_MY_INVITATIONS_ERROR'
      return []
    }
  }

  return {
    events,
    myPendingInvitations,
    error,
    fetchCategoriesByType,
    createEvent,
    fetchEventsByType,
    deleteEvent,
    inviteUserToEvent,
    inviteUsersToEvent,
    respondToInvitation,
    fetchEventInvitationStatus,
    fetchMyInvitations
  }
}
