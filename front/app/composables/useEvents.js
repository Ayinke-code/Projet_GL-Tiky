export const useEvents = () => {
  const api = useApi()

    /**
   * État global des catégories.
   */
  const events = useState('events', () => [])

    /**
   * Utilisateur actuellement connecté.
   */
  const { user } = useAuth()

    /**
   * État global des erreurs liées aux catégories.
   */
  const error = ref(null)

  /**
   * Récupère toutes les catégories depuis l'API.
   *
   * @return liste des catégories
   * @throws erreur API si le chargement échoue
   */
  const fetchEvents = async () => {
    const data = await api('/events')
    events.value = data
    return data
  }

  /**
   * Crée une nouvelle catégorie.
   *
   * @param payload données de création
   * @param payload.name nom de la catégorie
   * @param payload.subject sujet de la catégorie
   *
   * @return catégorie créée
   *
   * @throws redirection vers /login si utilisateur non connecté
   * @throws met à jour `error` si erreur API
   */
  const createEvents = async (payload) => {

    error.value = null

    try {
      const created = await api('/events', {
        method: 'POST',
        body: payload
      })

      // refresh local state
      await fetchEvents()

      return created
    } catch (e) {
      error.value = e?.data?.message || 'UNKNOWN_ERROR'
      return null
    }
  }

  /**
   * Supprime une catégorie.
   *
   * @param id identifiant de la catégorie
   */
  const deleteEvents = async (id) => {
    await api(`/events/${id}`, {
      method: 'DELETE'
    })

    events.value = events.value.filter(c => c.id !== id)
  }

  return {
    events,
    fetchEvents,
    createEvents,
    deleteEvents,
    error
  }
}