import { ref } from 'vue'

export const useCategory = (teacherId = 'teacher-default') => {
  const categories = ref([])
  const loading = ref(false)
  const error = ref(null)

  const API_BASE = 'http://localhost:8080/api/categories'

  // Helper pour les headers
  const getHeaders = () => ({
    'Content-Type': 'application/json',
    'X-Teacher-ID': teacherId
  })

  // Récupérer les catégories de l'enseignant courant
  const fetchCategories = async () => {
    loading.value = true
    error.value = null
    try {
      const response = await fetch(`${API_BASE}/teacher`, {
        headers: getHeaders()
      })
      if (!response.ok) throw new Error('Erreur lors de la récupération')
      categories.value = await response.json()
    } catch (err) {
      error.value = err.message
    } finally {
      loading.value = false
    }
  }

  // Créer une catégorie
  const createCategory = async (categoryData) => {
    loading.value = true
    error.value = null
    try {
      const response = await fetch(API_BASE, {
        method: 'POST',
        headers: getHeaders(),
        body: JSON.stringify(categoryData)
      })

      if (!response.ok) {
        const errData = await response.json()
        throw new Error(errData.message || 'Erreur lors de la création')
      }

      const newCategory = await response.json()
      categories.value.push(newCategory)
      return newCategory
    } catch (err) {
      error.value = err.message
      throw err
    } finally {
      loading.value = false
    }
  }

  const updateCategory = async (id, updates) => {
    loading.value = true
    error.value = null
    try {
      const response = await fetch(`${API_BASE}/${id}`, {
        method: 'PUT',
        headers: getHeaders(),
        body: JSON.stringify(updates)
      })

      if (!response.ok) throw new Error('Erreur lors de la mise à jour')

      return await response.json()
    } catch (err) {
      error.value = err.message
      throw err
    } finally {
      loading.value = false
    }
  }

  const archiveCategory = async (id) => {
    loading.value = true
    try {
      const response = await fetch(`${API_BASE}/${id}/archive`, {
        method: 'PATCH',
        headers: getHeaders()
      })
      if (!response.ok) throw new Error('Erreur lors de l\'archivage')
      return await response.json()
    } finally {
      loading.value = false
    }
  }

  const unarchiveCategory = async (id) => {
    loading.value = true
    try {
      const response = await fetch(`${API_BASE}/${id}/unarchive`, {
        method: 'PATCH',
        headers: getHeaders()
      })
      if (!response.ok) throw new Error('Erreur lors du désarchivage')
      return await response.json()
    } finally {
      loading.value = false
    }
  }

  const followCategory = async (id) => {
    try {
      await fetch(`${API_BASE}/${id}/follow`, {
        method: 'POST',
        headers: getHeaders()
      })
    } catch (err) {
      console.error('Follow error', err)
    }
  }

  const unfollowCategory = async (id) => {
    try {
      await fetch(`${API_BASE}/${id}/follow`, {
        method: 'DELETE',
        headers: getHeaders()
      })
    } catch (err) {
      console.error('Unfollow error', err)
    }
  }

  const getFollowedCategories = async () => {
    try {
      const response = await fetch(`${API_BASE}/followed`, {
        headers: getHeaders()
      })
      if (response.ok) return await response.json()
    } catch (err) {
      console.error('Fetch followed error', err)
    }
    return []
  }

  const deleteCategoryPermanent = async (id) => {
    loading.value = true
    try {
      const response = await fetch(`${API_BASE}/${id}`, {
        method: 'DELETE',
        headers: getHeaders()
      })
      if (!response.ok) throw new Error('Erreur lors de la suppression')
    } finally {
      loading.value = false
    }
  }

  const fetchFollowers = async (categoryId) => {
    try {
      const response = await fetch(`${API_BASE}/${categoryId}/followers`, {
        headers: getHeaders()
      })
      if (response.ok) return await response.json()
    } catch (err) {
      console.error('Fetch followers error', err)
    }
    return []
  }

  const addFollower = async (categoryId, userId) => {
    try {
      const response = await fetch(`${API_BASE}/${categoryId}/followers?userId=${userId}`, {
        method: 'POST',
        headers: getHeaders()
      })
      if (!response.ok) throw new Error('Erreur lors de l\'ajout du follower')
      return await response.json()
    } catch (err) {
      console.error('Add follower error', err)
      throw err
    }
  }

  const removeFollower = async (categoryId, userId) => {
    try {
      const response = await fetch(`${API_BASE}/${categoryId}/followers/${userId}`, {
        method: 'DELETE',
        headers: getHeaders()
      })
      if (!response.ok) throw new Error('Erreur lors de la suppression du follower')
      return await response.json()
    } catch (err) {
      console.error('Remove follower error', err)
      throw err
    }
  }

  const fetchAvailableTeachers = async (categoryId) => {
    try {
      const response = await fetch(`${API_BASE}/${categoryId}/available-teachers`, {
        headers: getHeaders()
      })
      if (response.ok) return await response.json()
    } catch (err) {
      console.error('Fetch available teachers error', err)
    }
    return []
  }

  const approveFollower = async (categoryId, teacherId) => {
    try {
      const response = await fetch(`${API_BASE}/${categoryId}/approve/${teacherId}`, {
        method: 'POST',
        headers: getHeaders()
      })
      if (!response.ok) throw new Error('Erreur lors de l\'approbation')
    } catch (err) {
      console.error('Approve follower error', err)
      throw err
    }
  }

  const rejectFollower = async (categoryId, teacherId) => {
    try {
      const response = await fetch(`${API_BASE}/${categoryId}/reject/${teacherId}`, {
        method: 'DELETE',
        headers: getHeaders()
      })
      if (!response.ok) throw new Error('Erreur lors du rejet')
    } catch (err) {
      console.error('Reject follower error', err)
      throw err
    }
  }

  const fetchPendingRequests = async (categoryId) => {
    try {
      const response = await fetch(`${API_BASE}/${categoryId}/pending`, {
        headers: getHeaders()
      })
      if (response.ok) return await response.json()
    } catch (err) {
      console.error('Fetch pending requests error', err)
    }
    return []
  }

  return {
    categories,
    loading,
    error,
    fetchCategories,
    createCategory,
    updateCategory,
    archiveCategory,
    unarchiveCategory,
    followCategory,
    unfollowCategory,
    getFollowedCategories,
    deleteCategoryPermanent,
    fetchFollowers,
    addFollower,
    removeFollower,
    fetchAvailableTeachers,
    approveFollower,
    rejectFollower,
    fetchPendingRequests
  }
}