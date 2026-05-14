import { ref } from 'vue'

export const useApi = () => {
  const token = useCookie('auth_token')
  const BASE_URL = 'http://localhost:8080/api'

  const loading = ref(false)
  const error = ref<string | null>(null)

  // Helper pour les headers
  const getHeaders = () => {
    const headers: Record<string, string> = {
      'Content-Type': 'application/json'
    }

    if (token.value) {
      headers['Authorization'] = `Bearer ${token.value}`
    }

    return headers
  }

  // Créer une issue
  const createIssue = async (issueData: any) => {
    loading.value = true
    error.value = null
    try {
      const response = await $fetch(`${BASE_URL}/issues`, {
        method: 'POST',
        headers: getHeaders(),
        body: issueData
      })
      return response
    } catch (err: any) {
      console.error('Create issue error:', err)
      error.value = err.data?.message || err.message || 'Erreur lors de la création'
      throw err
    } finally {
      loading.value = false
    }
  }

  // Récupérer toutes les issues
  const getIssues = async () => {
    loading.value = true
    error.value = null
    try {
      const response = await $fetch(`${BASE_URL}/issues`, {
        headers: getHeaders()
      })
      return response || []
    } catch (err: any) {
      console.error('Get issues error:', err)
      const errorMsg = err.data?.message || err.message || 'Erreur lors du chargement des issues'
      error.value = errorMsg
      throw err
    } finally {
      loading.value = false
    }
  }

  // Récupérer une issue par ID
  const getIssueById = async (id: number) => {
    loading.value = true
    error.value = null
    try {
      const response = await $fetch(`${BASE_URL}/issues/${id}`, {
        headers: getHeaders()
      })
      return response
    } catch (err: any) {
      console.error('Get issue by id error:', err)
      error.value = err.data?.message || err.message || 'Erreur lors du chargement'
      throw err
    } finally {
      loading.value = false
    }
  }

  // Supprimer une issue
  const deleteIssue = async (id: number) => {
    loading.value = true
    error.value = null
    try {
      await $fetch(`${BASE_URL}/issues/${id}`, {
        method: 'DELETE',
        headers: getHeaders()
      })
    } catch (err: any) {
      console.error('Delete issue error:', err)
      error.value = err.data?.message || err.message || 'Erreur lors de la suppression'
      throw err
    } finally {
      loading.value = false
    }
  }

  // Récupérer les catégories actives pour les formulaires de création
  const getCategories = async () => {
    loading.value = true
    error.value = null
    try {
      const response = await $fetch(`${BASE_URL}/categories/active`, {
        headers: getHeaders()
      })
      return response || []
    } catch (err: any) {
      console.error('Get categories error:', err)
      const errorMsg = err.data?.message || err.message || 'Impossible de charger les catégories'
      error.value = errorMsg
      // Ne pas thrower, retourner un array vide pour que le formulaire fonctionne
      return []
    } finally {
      loading.value = false
    }
  }

  // Mettre à jour le statut d'une issue
  const updateIssueStatus = async (id: number, statut: string) => {
    loading.value = true
    error.value = null
    try {
      const response = await $fetch(`${BASE_URL}/issues/${id}/status`, {
        method: 'PUT',
        headers: getHeaders(),
        body: { statut }
      })
      return response
    } catch (err: any) {
      console.error('Update issue status error:', err)
      error.value = err.data?.message || err.message || 'Erreur lors de la mise à jour du statut'
      throw err
    } finally {
      loading.value = false
    }
  }

  return {
    loading,
    error,
    createIssue,
    getIssues,
    getIssueById,
    deleteIssue,
    getCategories,
    updateIssueStatus
  }
}
