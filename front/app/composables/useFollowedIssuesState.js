/**
 * Composable gérant le state global des issues suivies.
 * Permet à plusieurs pages/composants de rester synchronisés.
 *
 * Utilisation :
 * const { followedIssues, setFollowedIssues, removeFollowedIssue, refreshFollowedIssues } = useFollowedIssuesState()
 */
export const useFollowedIssuesState = () => {
  /**
   * State global : liste des issues suivies
   */
  const followedIssues = useState('followedIssues', () => [])

  const api = useApi()
  const error = ref(null)

  /**
   * Met à jour le state global avec une nouvelle liste.
   * @param {Array} issues - liste des issues suivies
   */
  const setFollowedIssues = (issues) => {
    followedIssues.value = issues || []
  }

  /**
   * Retire une issue du state global (ex: quand elle devient privée).
   * @param {number} issueId - identifiant de l'issue à retirer
   */
  const removeFollowedIssue = (issueId) => {
    followedIssues.value = followedIssues.value.filter(issue => issue.id !== issueId)
  }

  /**
   * Recharge la liste des issues suivies depuis le backend.
   * Appelé au montage des pages qui affichent les issues suivies.
   * @returns {Promise<Array>} liste mises à jour
   */
  const refreshFollowedIssues = async () => {
    error.value = null
    try {
      const apiUseIssueFollowers = useIssueFollowers()
      const fresh = await apiUseIssueFollowers.getFollowedIssues()
      setFollowedIssues(fresh)
      return fresh
    } catch (err) {
      error.value = err?.data?.message || 'Erreur lors du chargement des issues suivies'
      console.error('[useFollowedIssuesState] Erreur:', error.value)
      throw err
    }
  }

  return {
    followedIssues,
    setFollowedIssues,
    removeFollowedIssue,
    refreshFollowedIssues,
    error
  }
}

