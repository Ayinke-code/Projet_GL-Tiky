export const useCategories = () => {
  const api = useApi()

    /**
   * État global des catégories.
   */
  const categories = useState('categories', () => [])

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
  const fetchCategories = async () => {
    const data = await api.getCategories()
    categories.value = data
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
  const createCategory = async (payload) => {

    error.value = null

    if (!user?.value?.id) {
      await navigateTo('/login')
      return
    }

    const values = {
      name: payload.name,
      subject: payload.subject,
      teacher: user.value.email || user.value.name
    }

    try {
      const created = await api.createCategory(values)

      // refresh local state
      await fetchCategories()

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
  const deleteCategory = async (id) => {
    await api.deleteCategory(id)

    categories.value = categories.value.filter(c => c.id !== id)
  }

  return {
    categories,
    fetchCategories,
    createCategory,
    deleteCategory,
    error
  }
}
