export const useCategories = () => {
  const api = useApi()

  /**
   * État global des catégories.
   */
  const categories = useState('categories', () => [])

  /**
   * Catégorie couramment sélectionnée / affichée.
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
   * @returns {Array}
   */
  const fetchCategories = async () => {
    const data = await api('/categories')
    categories.value = data
    return data
  }

  /**
   * Récupère les catégories filtrées par type.
   *
   * @param {string} type
   * @returns {Array}
   */
  const fetchCategoriesByType = async (type) => {
    const data = await api(`/categories/type/${type}`)
    categories.value = data
    return data
  }

  /**
   * Récupère une catégorie par son ID.
   */
  const fetchCategoryById = async (id) => {
    const data = await api(`/categories/${id}`)
    category.value = data
    return data
  }

  /**
   * Crée une catégorie.
   */
  const createCategory = async (payload) => {
    if (!user?.value?.id) return null
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
      return {
        error: `Catégorie existe déja pour le type Issue`
      }
    }
  }

  /**
   * Supprime une catégorie.
   */
  const deleteCategory = async (id) => {
    const deleted = await api(`/categories/${id}`, {
      method: 'DELETE'
    })
    await fetchCategories()
    return deleted
  }

  /**
   * Met à jour une catégorie.
   */
  const updateCategory = async (id, payload) => {
    const values = {
      name: payload.name,
      subject: payload.subject,
      ownerId: payload.ownerId,
      initialFollowerIds: payload.initialFollowerIds
    }
    const updated = await api(`/categories/${id}`, {
      method: 'PUT',
      body: values
    })
    await fetchCategories()
    await fetchCategoryById(id)
    return updated
  }

  /**
   * Archive une catégorie.
   */
  const archiveCategory = async (id) => {
    const archived = await api(`/categories/${id}/archive`, {
      method: 'POST'
    })
    await fetchCategories()
    return archived
  }

  /**
   * Désarchive une catégorie.
   */
  const unarchiveCategory = async (id) => {
    const unarchived = await api(`/categories/${id}/unarchive`, {
      method: 'POST'
    })
    await fetchCategories()
    return unarchived
  }

  /**
   * Suivre une catégorie en tant qu'utilisateur courant.
   */
  const followCategory = async (categoryId) => {
    if (!user?.value?.id || !categoryId) return null
    const res = await api(`/categories/followers/${categoryId}`, {
      method: 'POST'
    })
    await fetchCategories()
    return res
  }

  /**
   * Ne plus suivre une catégorie en tant qu'utilisateur courant.
   */
  const unfollowCategory = async (categoryId) => {
    if (!user?.value?.id || !categoryId) return null
    const res = await api(`/categories/followers/${categoryId}`, {
      method: 'DELETE'
    })
    await fetchCategories()
    return res
  }

  /**
   * Récupère les catégories suivies par l'utilisateur courant.
   */
  const getFollowedCategories = async () => {
    try {
      return await api('/categories/followed')
    } catch (e) {
      const currentUserId = user?.value?.id
      if (!currentUserId) return []
      return categories.value.filter(cat => {
        return cat?.followers?.some(f => f.id === currentUserId) || cat?.followerIds?.includes(currentUserId)
      })
    }
  }

  /**
   * Récupère les followers d'une catégorie.
   */
  const fetchFollowers = async (categoryId) => {
    return await api(`/categories/${categoryId}/followers`)
  }

  /**
   * Ajoute un follower à une catégorie.
   */
  const addFollower = async (categoryId, userId) => {
    const res = await api(`/categories/followers/${categoryId}`, {
      method: 'POST'
    })
    await fetchCategoryById(categoryId)
    return res
  }

  /**
   * Supprime un follower d'une catégorie.
   */
  const removeFollower = async (categoryId, userId) => {
    await api(`/categories/followers/${categoryId}`, {
      method: 'DELETE'
    })
    await fetchCategoryById(categoryId)
  }

  /**
   * Récupère les demandes en attente pour une catégorie.
   */
  const fetchPendingRequests = async (categoryId) => {
    return await api(`/categories/${categoryId}/followers/requests`)
  }

  /**
   * Approuve une demande d'abonnement.
   */
  const approveFollower = async (categoryId, teacherId) => {
    const res = await api(`/categories/${categoryId}/followers/${teacherId}/approve`, {
      method: 'POST'
    })
    await fetchCategoryById(categoryId)
    return res
  }

  /**
   * Rejette une demande d'abonnement.
   */
  const rejectFollower = async (categoryId, teacherId) => {
    const res = await api(`/categories/${categoryId}/followers/${teacherId}/reject`, {
      method: 'POST'
    })
    await fetchCategoryById(categoryId)
    return res
  }

  /**
   * Supprime un utilisateur des followers d'une catégorie.
   */
  const deleteTeacherFollower = async (idUser, idCategory) => {
    if (!idUser || !idCategory) return
    await api(`/categories/followers/${idCategory}`, {
      method: 'DELETE'
    })
    if (category.value?.followers) {
      category.value.followers = category.value.followers.filter(u => u.id !== idUser)
    }
  }

  /**
   * Ajoute un utilisateur aux followers d'une catégorie.
   */
  const addTeacherFollower = async (idUser, idCategory) => {
    if (!idUser || !idCategory) return
    const res = await api(`/categories/followers/${idCategory}`, {
      method: 'POST'
    })
    category.value = res
    return res
  }

  /**
 * Envoie une demande de retrait d'une catégorie au propriétaire.
 *
 * @param categoryId identifiant de la catégorie
 */
  const requestCategoryRemoval = async (categoryId) => {
    await api(`/categories/${categoryId}/removal-request`, {
      method: 'POST'
    })
  }

  return {
    categories,
    category,
    fetchCategories,
    fetchCategoriesByType,
    fetchCategoryById,
    createCategory,
    updateCategory,
    archiveCategory,
    unarchiveCategory,
    followCategory,
    unfollowCategory,
    getFollowedCategories,
    fetchFollowers,
    addFollower,
    removeFollower,
    fetchPendingRequests,
    approveFollower,
    rejectFollower,
    deleteCategory,
    deleteTeacherFollower,
    addTeacherFollower,
    requestCategoryRemoval,
    error
  }
}
