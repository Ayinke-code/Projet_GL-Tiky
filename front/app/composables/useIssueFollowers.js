/**
 * Composable gérant le suivi des issues par un utilisateur.
 */
export const useIssueFollowers = () => {

  const error = ref(null)

  /**
   * Abonne l'utilisateur courant à une issue avec une raison.
   * @param {number} issueId - identifiant de l'issue
   * @param {string} reason - INTERESTED | ABLE_TO_HELP | IMPACTED
   * @returns l'abonnement créé
   */
  const followIssue = async (issueId, reason) => {
    const api = useApi()
    error.value = null
    try {
      return await api(`/issues/${issueId}/follow?reason=${reason}`, {
        method: 'POST'
      })
    } catch (e) {
      error.value = e?.data?.message || 'FOLLOW_ISSUE_ERROR'
      throw e
    }
  }

  /**
   * Désabonne l'utilisateur courant d'une issue.
   * @param {number} issueId - identifiant de l'issue
   */
  const unfollowIssue = async (issueId) => {
    const api = useApi()
    error.value = null
    try {
      return await api(`/issues/${issueId}/follow`, {
        method: 'DELETE'
      })
    } catch (e) {
      error.value = e?.data?.message || 'UNFOLLOW_ISSUE_ERROR'
      throw e
    }
  }

  return {
    followIssue,
    unfollowIssue,
    error
  }
}
