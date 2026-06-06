export const useIssueRelations = () => {
  const relations = useState('issueRelations', () => [])
  const error = ref(null)
  const loading = ref(false)

  /**
   * Récupère toutes les relations liées à une issue (Source ou Cible)
   */
  const fetchRelationsByIssue = async (issueId) => {
      const api = useApi()
      loading.value = true
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
   * Crée un nouveau lien entre l'issue courante et une cible
   */
  const createRelation = async (sourceId, targetIssueId, type) => {
    const api = useApi()
    error.value = null
    try {
      const created = await api(`/issues/${sourceId}/relations`, {
        method: 'POST',
        body: { targetIssueId, type }
      })
      await fetchRelationsByIssue(sourceId)
      return created
    } catch (e) {
      error.value = e?.data?.message || 'Impossible de créer la relation'
      throw e
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

  return {
    relations,
    error,
    loading,
    fetchRelationsByIssue,
    createRelation,
    deleteRelation
  }
}
