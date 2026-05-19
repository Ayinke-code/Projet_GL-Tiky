export const useCategories = () => {
  const api = useApi()

  /**
 * État global des catégories.
 */
  const categories = useState('categories', () => [])

  /**
 * Catégorie couramment sélectionnée / affichée
 */
  const category = useState('category', () => null)

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
    const data = await api('/categories')
    categories.value = data
    return data
  }

  /**
 * Récupère une catégorie par son ID
 */
  const fetchCategoryById = async (id) => {
    const data = await api(`/categories/${id}`)
    category.value = data
    return data
  }

  /**
 * Supprime un utilisateur des followers d'une catégorie
 * et met à jour le state local
 */
  const deleteTeacherFollower = async (idUser, idCategory) => {
    if (!idUser || !idCategory) return

    await api(`/categories/followers/${idUser}/${idCategory}`, {
      method: 'DELETE'
    })

    category.value.followers = category.value.followers.filter(u => u.id !== idUser)
  }

  /**
 * Ajoute un utilisateur aux followers d'une catégorie
 * et remplace la catégorie locale par la version mise à jour
 */
  const addTeacherFollower = async (idUser, idCategory) => {
    if (!idUser || !idCategory) return

    const res = await api(`/categories/followers/${idUser}/${idCategory}`, {
      method: 'POST'
    })
    category.value = res
    return res
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
      ownerId: user.value.id
    }

    try {
      const created = await api('/categories', {
        method: 'POST',
        body: values
      })

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
    await api(`/categories/${id}`, {
      method: 'DELETE'
    })

    categories.value = categories.value.filter(c => c.id !== id)
  }

  return {
    categories,
    category,
    fetchCategories,
    fetchCategoryById,
    createCategory,
    deleteCategory,
    deleteTeacherFollower,
    addTeacherFollower,
    error
  }
}