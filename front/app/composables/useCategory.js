import { ref } from 'vue'

const categories = ref([])
const loading = ref(false)
const error = ref(null)

export const useCategory = (teacherId = 'teacher-default') => {

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

  return {
    categories,
    loading,
    error,
    fetchCategories,
    createCategory,
    updateCategory
  }
}