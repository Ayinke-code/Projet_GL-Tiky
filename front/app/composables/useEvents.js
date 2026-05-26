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

  return { events, error, fetchCategoriesByType, createEvent, fetchEventsByType, deleteEvent }
}