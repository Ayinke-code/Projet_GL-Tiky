import { ref } from 'vue'

export const useMergeIssues = () => {
  const pendingRequests = useState('mergeRequests', () => [])

  const loading = ref(false)
  const error = ref(null)

  const resetError = () => {
    error.value = null
  }


  /**
   * Initie une nouvelle demande de fusion.
   */
  const requestMerge = async (sourceId, targetId) => {
    const api = useApi()
    loading.value = true
    resetError()

    try {
      const result = await api(`/merging/${sourceId}/merge?targetId=${targetId}`, {
        method: 'POST'
      })
      return result
    } catch (e) {
      error.value = e?.data?.message || 'Erreur lors de la demande de fusion.'
      throw e
    } finally {
      loading.value = false
    }
  }

  /**
   * Permet à un manager de répondre (accepter/refuser) à une demande de fusion.
   */
  const respondToMergeRequest = async (requestId, accept, reason = '') => {
    const api = useApi()
    loading.value = true
    resetError()

    try {
      const url = new URLSearchParams()
      url.append('accept', accept)
      if (!accept && reason) {
        url.append('reason', reason)
      }

      const result = await api(`/merging/${requestId}/respond?${url.toString()}`, {
        method: 'POST'
      })

      await fetchPendingRequests()
      return result
    } catch (e) {
      error.value = e?.data?.message || 'Erreur lors du traitement de la demande.'
      throw e
    } finally {
      loading.value = false
    }
  }

  /**
   * Récupère la liste des demandes de fusion en attente pour le manager courant.
   */
  const fetchPendingRequests = async () => {
    const api = useApi()
    loading.value = true
    resetError()

    try {
      const data = await api('/merging/requests')
      pendingRequests.value = data
      return data
    } catch (e) {
      error.value = e?.data?.message || 'Erreur lors de la récupération des demandes.'
      pendingRequests.value = []
      return []
    } finally {
      loading.value = false
    }
  }

  return {
    pendingRequests,
    loading,
    error,
    requestMerge,
    respondToMergeRequest,
    fetchPendingRequests
  }
}
