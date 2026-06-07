export const useIssueRelations = () => {
  // États globaux (via Nuxt useState) ou locaux
  const relations = useState('issueRelations', () => [])
  const pendingRequests = useState('pendingRelationRequests', () => [])

  const error = ref(null)
  const loading = ref(false)

  /**
   * Récupère toutes les relations liées à un ticket (Source ou Cible)
   */
  const fetchRelationsByIssue = async (issueId) => {
    const api = useApi()
    loading.value = true
    error.value = null
    try {
      const data = await api(`/issues/${issueId}/relations`)
      relations.value = data
      return data
    } catch (e) {
      const status = e?.response?.status || e?.status
      if (status === 403) {
        relations.value = []
        return []
      }
      error.value = e?.data?.message || 'Erreur lors du chargement des relations'
      return []
    } finally {
      loading.value = false
    }
  }

  /**
   * Crée un nouveau lien entre le ticket courant et une cible
   * NOTE : Peut retourner 'null' si l'API renvoie 202 ACCEPTED (requête envoyée à un autre manager)
   */
  const createRelation = async (sourceId, targetIssueId, type) => {
    const api = useApi()
    error.value = null
    loading.value = true
    try {
      const created = await api(`/issues/${sourceId}/relations`, {
        method: 'POST',
        body: { targetIssueId, type }
      })

      if (created) {
        await fetchRelationsByIssue(sourceId)
      }

      return created
    } catch (e) {
      error.value = e?.data?.message || 'Impossible de créer la relation'
      throw e
    } finally {
      loading.value = false
    }
  }

  /**
   * Supprime une relation par son identifiant unique
   */
  const deleteRelation = async (relationId, currentIssueId) => {
    const api = useApi()
    error.value = null
    try {
      await api(`/issues/relations/${relationId}`, { method: 'DELETE' })
      await fetchRelationsByIssue(currentIssueId)
    } catch (e) {
      error.value = e?.data?.message || 'Impossible de supprimer la relation'
      throw e
    }
  }


  /**
   * Récupère la liste des demandes de liens en attente pour le manager connecté
   */
  const fetchPendingRequests = async () => {
    const api = useApi()
    loading.value = true
    error.value = null
    try {
      const data = await api('/issues/relations/requests')
      pendingRequests.value = data
      return data
    } catch (e) {
      error.value = e?.data?.message || 'Impossible de charger les demandes en attente'
      return []
    } finally {
      loading.value = false
    }
  }

  /**
   * Traite une demande de lien officiel (Approuver ou Refuser)
   */
  const handleLinkRequest = async (requestId, approve, rejectionReason = null) => {
    const api = useApi()
    error.value = null
    loading.value = true
    try {
      const result = await api(`/issues/relations/requests/${requestId}/handle`, {
        method: 'POST',
        body: {
          approve,
          rejectionReason
        }
      })

      await fetchPendingRequests()

      return result
    } catch (e) {
      error.value = e?.data?.message || 'Erreur lors du traitement de la demande'
      throw e
    } finally {
      loading.value = false
    }
  }

  return {
    relations,
    pendingRequests,
    error,
    loading,
    fetchRelationsByIssue,
    createRelation,
    deleteRelation,
    fetchPendingRequests,
    handleLinkRequest
  }
}
