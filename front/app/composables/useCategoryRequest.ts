import { ref } from 'vue'
import type { Ref } from 'vue'
import { useCookie } from '#app'

export interface CategoryRequest {
  id: number
  name: string
  subject: string
  status: string
  requesterId: number
  requesterName: string
  description?: string
}

export const useCategoryRequest = () => {
  const categoryRequests: Ref<CategoryRequest[]> = ref([])
  const loading: Ref<boolean> = ref(false)
  const error: Ref<string | null> = ref(null)
  const token = useCookie<string | null>('auth_token')

  const API_BASE = 'http://localhost:8080/api/category-requests'

  const getHeaders = (): Record<string, string> => {
    const headers: Record<string, string> = { 'Content-Type': 'application/json' }
    if (token.value) headers['Authorization'] = `Bearer ${token.value}`
    return headers
  }

  const fetchCategoryRequests = async (): Promise<void> => {
    loading.value = true
    error.value = null
    try {
      const response = await fetch(API_BASE, { headers: getHeaders() })
      if (!response.ok) throw new Error('Erreur lors de la récupération')
      categoryRequests.value = await response.json()
    } catch (err: unknown) {
      error.value = err instanceof Error ? err.message : 'Erreur lors de la récupération'
    } finally {
      loading.value = false
    }
  }

  const createCategoryRequest = async (data: Partial<CategoryRequest>): Promise<CategoryRequest | null> => {
    loading.value = true
    error.value = null
    try {
      const response = await fetch(API_BASE, {
        method: 'POST',
        headers: getHeaders(),
        body: JSON.stringify(data)
      })
      if (!response.ok) {
        const errData = await response.json().catch(() => ({}))
        throw new Error((errData as any).message || 'Erreur lors de la création')
      }
      const created: CategoryRequest = await response.json()
      categoryRequests.value.push(created)
      return created
    } catch (err: unknown) {
      error.value = err instanceof Error ? err.message : 'Erreur lors de la création'
      return null
    } finally {
      loading.value = false
    }
  }

  const acceptCategoryRequest = async (id: number): Promise<boolean> => {
    loading.value = true
    error.value = null
    try {
      const response = await fetch(`${API_BASE}/${id}/accept`, {
        method: 'POST',
        headers: getHeaders()
      })
      if (!response.ok) throw new Error('Erreur lors de l\'acceptation')
      categoryRequests.value = categoryRequests.value.filter(r => r.id !== id)
      return true
    } catch (err: unknown) {
      error.value = err instanceof Error ? err.message : 'Erreur lors de l\'acceptation'
      return false
    } finally {
      loading.value = false
    }
  }

  const deleteCategoryRequest = async (id: number): Promise<void> => {
    loading.value = true
    error.value = null
    try {
      const response = await fetch(`${API_BASE}/${id}`, {
        method: 'DELETE',
        headers: getHeaders()
      })
      if (!response.ok) throw new Error('Erreur lors de la suppression')
      categoryRequests.value = categoryRequests.value.filter(r => r.id !== id)
    } catch (err: unknown) {
      error.value = err instanceof Error ? err.message : 'Erreur lors de la suppression'
    } finally {
      loading.value = false
    }
  }

  return {
    categoryRequests,
    loading,
    error,
    fetchCategoryRequests,
    createCategoryRequest,
    acceptCategoryRequest,
    deleteCategoryRequest
  }
}
