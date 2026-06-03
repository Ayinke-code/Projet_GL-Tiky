export const useIssueRelations = () => {
  const relations = useState('issueRelations', () => [])
  const error = ref(null)
  const loading = ref(false)

  /**
   * Récupère toutes les relations liées à une issue (Source ou Cible)
   */
  const fetchRelationsByIssue = async (issueId) => {
    const api = useApi() // Instance déplacée à l'intérieur de la fonction
    loading.value = true
    try {
      const data = await api(`/issues/${issueId}/relations`)
      relations.value = data
      return data
    } catch (e) {
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
    const api = useApi() // Instance déplacée à l'intérieur de la fonction
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
    const api = useApi() // Instance déplacée à l'intérieur de la fonction
    error.value = null
    try {
      await api(`/issues/relations/${relationId}`, { method: 'DELETE' })
      relations.value = relations.value.filter(r => r.id !== relationId)
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
