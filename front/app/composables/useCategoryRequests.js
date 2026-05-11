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
   * Récupère toutes les demandes.
   */
  const fetchCategoryRequests = async () => {
    const data = await api('/category-requests')
    categoryRequests.value = data
    return data
  }

  /**
   * Crée une demande de catégorie.
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
   * Supprime une demande.
   */
  const deleteCategoryRequest = async (id) => {
    await api(`/category-requests/${id}`, {
      method: 'DELETE'
    })

    categoryRequests.value =
      categoryRequests.value.filter(r => r.id !== id)
  }

  return {
    categoryRequests,
    fetchCategoryRequests,
    createCategoryRequest,
    deleteCategoryRequest,
    error
  }
}