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
      return await api('/events/school', { method: 'POST', body: payload })
    } catch (e) {
      error.value = e?.data?.message || 'CREATE_EVENT_ERROR'
      throw e
    }
  }

  /**
   * Crée un nouvel événement académique (réunion) lié à un ticket.
   * Fait appel au endpoint POST /meeting/{issueId}.
   *
   * @param payload - { name, description, categoryId }
   * @param issueId - L'ID du ticket/issue associé
   */
  const createMeeting = async (payload, issueId) => {
    error.value = null
    if (!user?.value?.id) {
      await navigateTo('/login')
      return
    }
    try {
      return await api(`/events/meeting/${issueId}`, {
        method: 'POST',
        body: payload
      })
    } catch (e) {
      error.value = e?.data?.message || 'CREATE_MEETING_ERROR'
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
        body:  userIds
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
      myPendingInvitations.value = Array.isArray(data) ? data : []
      return myPendingInvitations.value
    } catch (e) {
      error.value = e?.data?.message || 'FETCH_MY_INVITATIONS_ERROR'
      myPendingInvitations.value = []
      return []
    }
  }

  /**
   * Vérifie si un ticket possède déjà une réunion à venir.
   *
   * @param {number} issueId - L'ID du ticket associé
   * @returns {Promise<boolean>} true si une réunion est prévue, false sinon
   */
  const checkUpcomingMeeting = async (issueId) => {
    error.value = null
    try {
      return await api(`/events/meeting/${issueId}/upcoming`)
    } catch (e) {
      error.value = e?.data?.message || 'CHECK_MEETING_ERROR'
      return false
    }
  }

  /**
   * Récupère la liste des événements créés par l'utilisateur connecté.
   */
  const fetchMyCreatedEvents = async () => {
    error.value = null
    try {
      const data = await api(`/events/me/created`)
      // Tu peux soit retourner la donnée, soit la stocker dans un state si besoin
      events.value = data
      return data
    } catch (e) {
      error.value = e?.data?.message || 'FETCH_MY_CREATED_EVENTS_ERROR'
      return []
    }
  }

  /**
   * Reprogramme une réunion suite à un refus et met à jour les invitations.
   *
   * @param {number} eventId - L'ID de l'événement
   * @param {boolean} addAll - true pour réinviter tout le monde, false pour composer sans les absents
   * @param {Object} datesPayload - { startDate: 'yyyymmddTHHMM', endDate: 'yyyymmddTHHMM' }
   */
  const updateMeetingDueToChange = async (eventId, addAll, datesPayload) => {
    error.value = null
    try {
      const result = await api(`/events/${eventId}/reschedule`, {
        method: 'PUT',
        params: { addAll },
        body: datesPayload
      })

      // Optionnel : Rafraîchir la liste locale des événements après modification
      await fetchMyCreatedEvents()

      return result
    } catch (e) {
      error.value = e?.data?.message || 'RESCHEDULE_MEETING_ERROR'
      throw e
    }
  }

  /**
   * Annule un événement et met à jour la liste locale.
   *
   * @param {number} eventId - L'ID de l'événement à annuler
   */
  const cancelEvent = async (eventId) => {
    error.value = null
    try {
      await api(`/events/${eventId}/cancel`, {
        method: 'DELETE'
      })

      // Mise à jour de l'état local pour retirer l'événement de l'affichage
      events.value = events.value.filter(e => e.id !== eventId)
    } catch (e) {
      error.value = e?.data?.message || 'CANCEL_EVENT_ERROR'
      throw e
    }
  }

  /**
   * Crée une demande d'événement (réunion) lié à un ticket (soumis à validation).
   * Fait appel au endpoint POST /events/meeting/ask/{issueId}.
   *
   * @param {Object} payload - { name, description, categoryId, startDate, endDate }
   * @param {number} issueId - L'ID du ticket associé
   */
  const askForMeeting = async (payload, issueId) => {
    error.value = null
    if (!user?.value?.id) {
      await navigateTo('/login')
      return
    }
    try {
      return await api(`/events/meeting/ask/${issueId}`, {
        method: 'POST',
        body: payload
      })
    } catch (e) {
      error.value = e?.data?.message || 'ASK_MEETING_ERROR'
      throw e
    }
  }

  /**
   * Récupère la liste et le statut des demandes de réunions initiées par l'utilisateur connecté.
   * Fait appel au endpoint GET /events/me/demands.
   *
   * @returns {Promise<Array>} Liste de MeetingDemandeDto [{ issueId, status }]
   */
  const fetchMyDemandsStatus = async () => {
    error.value = null
    try {
      return await api('/events/me/demands')
    } catch (e) {
      error.value = e?.data?.message || 'FETCH_DEMANDS_STATUS_ERROR'
      return []
    }
  }

  /**
   * Retire un participant spécifique d'une réunion.
   * Fait appel au endpoint DELETE /events/{eventId}/participants/{userId}.
   *
   * @param {number} eventId - L'ID de l'événement
   * @param {number} userId - L'ID de l'utilisateur à retirer
   * @returns {Promise<Object>} Le DTO de l'événement mis à jour
   */
  const removeParticipantFromMeeting = async (eventId, userId) => {
    error.value = null
    try {
      const result = await api(`/events/${eventId}/participants/${userId}`, {
        method: 'DELETE'
      })

      // Optionnel : Si tu affiches la liste détaillée des statuts juste après,
      // tu peux re-fetcher les statuts ici
      // await fetchEventInvitationStatus(eventId)

      return result
    } catch (e) {
      error.value = e?.data?.message || 'REMOVE_PARTICIPANT_ERROR'
      throw e
    }
  }

  /**
   * Récupère la liste des abonnés du ticket rattaché qui ne sont pas encore invités.
   * Fait appel au endpoint GET /events/{eventId}/possible-participants.
   *
   * @param {number} eventId - L'ID de l'événement
   * @returns {Promise<Array>} Liste des FollowersInfoDto
   */
  const fetchPossibleUsersInterested = async (eventId) => {
    error.value = null
    try {
      const data = await api(`/events/${eventId}/possible-participants`)
      return Array.isArray(data) ? data : []
    } catch (e) {
      error.value = e?.data?.message || 'FETCH_POSSIBLE_USERS_ERROR'
      return []
    }
  }


  /**
   * Récupère tous les événements consolidés pour le calendrier de l'utilisateur
   * (Créateur, Invité, ou Membre d'un groupe assigné).
   * Fait appel au endpoint GET /events/calendar.
   *
   * @returns {Promise<Array>} Liste des EventResponseDto
   */
  const fetchCalendarEvents = async () => {
    error.value = null
    try {
      const data = await api('/events/calendar')

      // On met à jour l'état global "events" pour que le calendrier réagisse
      events.value = Array.isArray(data) ? data : []

      return events.value
    } catch (e) {
      error.value = e?.data?.message || 'FETCH_CALENDAR_EVENTS_ERROR'
      events.value = []
      return []
    }
  }

  /**
   * Récupère les événements auxquels l'utilisateur connecté participe (invitations acceptées).
   * Exclut les événements créés par l'utilisateur lui-même.
   */
  const fetchMyParticipations = async () => {
    error.value = null
    try {
      const data = await api('/events/my-participations')
      return Array.isArray(data) ? data : []
    } catch (e) {
      error.value = e?.data?.message || 'FETCH_MY_PARTICIPATIONS_ERROR'
      return []
    }
  }

  /**
   * Déclare une plage de disponibilité pour l'utilisateur connecté.
   * POST /events/availability
   *
   * @param {{ startDate: string, endDate: string, description?: string }} payload
   */
  const createAvailability = async (payload) => {
    error.value = null
    try {
      const result = await api('/events/availability', {
        method: 'POST',
        body: payload
      })
      await fetchCalendarEvents()
      return result
    } catch (e) {
      const status = e?.response?.status ?? e?.statusCode
      error.value = e?.data?.error || e?.data?.message || `Erreur serveur (${status ?? 'réseau'})`
      console.error('[createAvailability]', status, e?.data)
      throw e
    }
  }

  /**
   * Récupère toutes les disponibilités déclarées par l'utilisateur connecté.
   * GET /events/availability/me
   */
  const fetchMyAvailabilities = async () => {
    error.value = null
    try {
      const data = await api('/events/availability/me')
      return Array.isArray(data) ? data : []
    } catch (e) {
      error.value = e?.data?.message || 'FETCH_AVAILABILITIES_ERROR'
      return []
    }
  }

  /**
   * Récupère les disponibilités déclarées par un utilisateur spécifique.
   * Utilisé pour vérifier la disponibilité avant d'envoyer une invitation.
   * GET /events/users/{userId}/availabilities
   *
   * @param {number} userId - L'ID de l'utilisateur cible
   * @returns {Promise<Array>} Liste des EventResponseDto de type AVAILABILITY
   */
  const fetchUserAvailabilities = async (userId) => {
    try {
      const data = await api(`/events/users/${userId}/availabilities`)
      return Array.isArray(data) ? data : []
    } catch (e) {
      console.error('[fetchUserAvailabilities]', e)
      return []
    }
  }

  /**
   * Supprime une disponibilité appartenant à l'utilisateur connecté.
   * DELETE /events/availability/{id}
   *
   * @param {number} id - L'ID de l'event AVAILABILITY
   */
  const deleteAvailability = async (id) => {
    error.value = null
    try {
      await api(`/events/availability/${id}`, { method: 'DELETE' })
      await fetchCalendarEvents()
    } catch (e) {
      const status = e?.response?.status ?? e?.statusCode
      error.value = e?.data?.error || e?.data?.message || `Erreur (${status ?? 'réseau'})`
      console.error('[deleteAvailability]', status, e?.data)
      throw e
    }
  }

  return {
    fetchEventsByType,
    deleteEvent,
    fetchCalendarEvents,
    events,
    myPendingInvitations,
    error,
    fetchCategoriesByType,
    createEvent,
    createMeeting,
    inviteUserToEvent,
    inviteUsersToEvent,
    respondToInvitation,
    fetchEventInvitationStatus,
    fetchMyInvitations,
    checkUpcomingMeeting,
    fetchMyCreatedEvents,
    updateMeetingDueToChange,
    cancelEvent,
    fetchMyDemandsStatus,
    askForMeeting,
    removeParticipantFromMeeting,
    fetchPossibleUsersInterested,
    fetchMyParticipations,
    createAvailability,
    fetchMyAvailabilities,
    fetchUserAvailabilities,
    deleteAvailability
  }
}
