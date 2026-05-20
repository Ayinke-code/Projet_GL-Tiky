/**
 * Composable gérant le suivi des issues par un utilisateur.
 */
export const useIssueFollowers = () => {

  const error = ref(null)

  /**
   * Abonne l'utilisateur courant à une issue avec une raison.
   * @param {number} issueId - identifiant de l'issue
   * @param {string} raison - INTERESTED | ABLE_TO_HELP | IMPACTED
   * @returns l'abonnement créé
   */
  const followIssue = async (issueId, raison) => {
    const api = useApi()
    error.value = null
    try {
      return await api(`/issues/${issueId}/follow?raison=${raison}`, {
        method: 'POST'
      })
    } catch (e) {
      error.value = e?.data?.message || 'FOLLOW_ISSUE_ERROR'
      throw e
    }
  }

  return {
    followIssue,
    error
  }
}
