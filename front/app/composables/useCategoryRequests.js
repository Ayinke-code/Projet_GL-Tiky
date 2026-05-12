export const useCategoryRequest = () => {
  const api = useApi()

  /**
   * État global des demandes de catégories.
   */
  const categoryRequests = useState('categoryRequests', () => [])

  /**
   * Utilisateur connecté.
   */
  const { user } = useAuth()

  /**
   * Gestion des erreurs.
   */
  const error = ref(null)

  /**
   * Récupère toutes les demandes de catégories depuis l'API
   * et met à jour l'état global `categoryRequests`.
   *
   * @returns {Array} liste des demandes de catégories
   */
  const fetchCategoryRequests = async () => {
    const data = await api('/category-requests')
    categoryRequests.value = data
    return data
  }

  /**
   * Crée une nouvelle demande de catégorie.
   *
   * Nettoie les données (suppression du champ description si vide),
   * envoie la requête au backend puis recharge la liste des demandes.
   *
   * @param {Object} payload - données du formulaire
   * @returns {Object|null} demande créée ou null en cas d'erreur
   */
  const createCategoryRequest = async (payload) => {
    error.value = null

    if (!user?.value?.id) {
      await navigateTo('/login')
      return
    }

    const values = {
      name: payload.name,
      subject: payload.subject,
      description: payload.description,
      requesterId: user.value.id
    }

    if (!values.description) {
      delete values.description
    }

    try {
      const created = await api('/category-requests', {
        method: 'POST',
        body: values
      })

      await fetchCategoryRequests()

      return created
    } catch (e) {
      error.value = e?.data?.message || 'UNKNOWN_ERROR'
      return null
    }
  }

  /**
   * Supprime une demande de catégorie.
   *
   * Met à jour immédiatement le state local après suppression API.
   *
   * @param {number} id - identifiant de la demande à supprimer
   */
  const deleteCategoryRequest = async (id) => {
    await api(`/category-requests/${id}`, {
      method: 'DELETE'
    })

    categoryRequests.value =
      categoryRequests.value.filter(r => r.id !== id)
  }

    /**
   * Valide une demande de catégorie (acceptation par un teacher).
   *
   * Envoie la validation au backend.
   * Le backend se charge de créer la catégorie associée.
   *
   * @param {number} id - identifiant de la demande
   * @returns {boolean|null} true si succès, null si erreur
   */
  const acceptCategoryRequest = async (id) => {
    if (!user?.value?.id) {
      await navigateTo('/login')
      return null
    }

    if (!id) {
      return null
    }

    try {
      await api(`/category-requests/${id}/accept`, {
        method: 'POST'
      })

      return true
    } catch (e) {
      error.value = e?.data?.message || 'UNKNOWN_ERROR'
      return null
    }
  }

  return {
    categoryRequests,
    fetchCategoryRequests,
    createCategoryRequest,
    deleteCategoryRequest,
    acceptCategoryRequest,
    error
  }
}