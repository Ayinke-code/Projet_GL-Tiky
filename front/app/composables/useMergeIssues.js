import { ref } from 'vue'

export const useMergeIssues = () => {
  // Clés distinctes pour éviter les conflits d'état
  const pendingRequests = useState('pendingMergeRequests', () => [])
  const deniedRequests = useState('deniedMergeRequests', () => [])

  const loading = ref(false)
  const error = ref(null)

  const resetError = () => {
    error.value = null
  }

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
      await fetchDeniedRequests() // On rafraîchit l'historique si on vient de refuser
      return result
    } catch (e) {
      error.value = e?.data?.message || 'Erreur lors du traitement de la demande.'
      throw e
    } finally {
      loading.value = false
    }
  }

  const fetchPendingRequests = async () => {
    const api = useApi()
    loading.value = true
    resetError()

    try {
      const data = await api('/merging/requests/pending')
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

  const fetchDeniedRequests = async () => {
    const api = useApi()
    loading.value = true
    resetError()

    try {
      // Endpoint supposé pour l'historique des refus (les 30 derniers jours)
      const data = await api('/merging/requests/denied')
      deniedRequests.value = data
      return data
    } catch (e) {
      error.value = e?.data?.message || 'Erreur lors de la récupération de l\'historique.'
      deniedRequests.value = []
      return []
    } finally {
      loading.value = false
    }
  }

  return {
    pendingRequests,
    deniedRequests,
    loading,
    error,
    requestMerge,
    respondToMergeRequest,
    fetchPendingRequests,
    fetchDeniedRequests
  }
}
