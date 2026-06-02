import { ref, computed, onMounted } from 'vue'

// Cache pour les marqueurs en dehors du composable pour partager l'état entre les instances
const followedIssuesUpdateMarkers = ref({})
const followedIssuesSeenUpdatedAt = ref({})
let conversationNewMessageListenerRegistered = false

/**
 * Composable gérant le state global des issues suivies.
 * Permet à plusieurs pages/composants de rester synchronisés.
 */
export const useFollowedIssuesState = () => {
  const { user } = useAuth()
  const followedIssues = useState('followedIssues', () => [])
  const api = useApi()
  const error = ref(null)

  // Initialisation du localStorage (client-side only)
  const initStorage = () => {
    if (!import.meta.client) return
    const storedSeen = localStorage.getItem('followedIssuesSeenUpdatedAt')
    const storedMarkers = localStorage.getItem('followedIssuesUpdateMarkers')

    if (storedSeen) {
      try {
        followedIssuesSeenUpdatedAt.value = JSON.parse(storedSeen)
      } catch (e) {
        console.error('[useFollowedIssuesState] Erreur parsing seen timestamps', e)
      }
    }

    if (storedMarkers) {
      try {
        followedIssuesUpdateMarkers.value = JSON.parse(storedMarkers)
      } catch (e) {
        console.error('[useFollowedIssuesState] Erreur parsing update markers', e)
      }
    }
  }

  const persistStoredMaps = () => {
    if (!import.meta.client) return
    localStorage.setItem('followedIssuesSeenUpdatedAt', JSON.stringify(followedIssuesSeenUpdatedAt.value))
    localStorage.setItem('followedIssuesUpdateMarkers', JSON.stringify(followedIssuesUpdateMarkers.value))
  }

  const normalizeTimestamp = (ts) => {
    if (!ts) return 0
    return new Date(ts).getTime()
  }

  /**
   * Marque une issue comme lue/prise en compte.
   */
  const acknowledgeFollowedIssueUpdate = (issueOrId) => {
    const issueId = typeof issueOrId === 'object' ? issueOrId?.id : issueOrId
    if (!issueId) return

    const issue = typeof issueOrId === 'object' ? issueOrId : followedIssues.value.find(item => item.id === issueId)
    const issueKey = String(issueId)

    if (issue) {
      followedIssuesSeenUpdatedAt.value[issueKey] = normalizeTimestamp(issue.updatedAt)
    }
    followedIssuesUpdateMarkers.value[issueKey] = false
    persistStoredMaps()
  }

  /**
   * Indique si une issue suivie a reçu une mise à jour.
   */
  const isFollowedIssueUpdated = (issueId) => {
    return Boolean(followedIssuesUpdateMarkers.value[String(issueId)])
  }

  /**
   * Force le marqueur de mise à jour pour une issue.
   */
  const markFollowedIssueAsUpdated = (issueId) => {
    if (!issueId) return
    followedIssuesUpdateMarkers.value[String(issueId)] = true
    persistStoredMaps()
  }

  /**
   * Synchronise les marqueurs en fonction de la date de mise à jour.
   */
  const syncFollowedIssueUpdateMarkers = (issues) => {
    if (!issues || !Array.isArray(issues)) return

    issues.forEach(issue => {
      const issueId = String(issue.id)
      const currentUpdatedAt = normalizeTimestamp(issue.updatedAt)
      const seenUpdatedAt = followedIssuesSeenUpdatedAt.value[issueId] || 0

      // Si l'issue a été mise à jour depuis la dernière fois qu'on l'a vue
      if (currentUpdatedAt > seenUpdatedAt) {
        followedIssuesUpdateMarkers.value[issueId] = true
      }
    })
    persistStoredMaps()
  }

  /**
   * Gère la réception d'un nouveau message via un événement global.
   */
  const handleConversationNewMessage = (event) => {
    const message = event?.detail
    if (!message) return

    const issueId = message.issueId ?? message.conversation?.issueId ?? null
    if (!issueId) return

    const authorId = message.userId ?? message.authorId ?? message.createdBy ?? message.user?.id ?? null
    const currentUserId = user?.value?.id ?? null

    // Si c'est l'utilisateur courant qui a envoyé le message, on ne marque pas comme non lu
    if (authorId !== null && currentUserId !== null && Number(authorId) === Number(currentUserId)) {
      return
    }

    markFollowedIssueAsUpdated(issueId)
  }

  // Enregistrement de l'écouteur d'événements
  if (import.meta.client && !conversationNewMessageListenerRegistered) {
    initStorage()
    window.addEventListener('conversation:new_message', handleConversationNewMessage)
    conversationNewMessageListenerRegistered = true
  }

  const setFollowedIssues = (issues) => {
    followedIssues.value = issues || []
    syncFollowedIssueUpdateMarkers(followedIssues.value)
  }

  const removeFollowedIssue = (issueId) => {
    followedIssues.value = followedIssues.value.filter(issue => issue.id !== issueId)
    delete followedIssuesUpdateMarkers.value[String(issueId)]
    delete followedIssuesSeenUpdatedAt.value[String(issueId)]
    persistStoredMaps()
  }

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
    acknowledgeFollowedIssueUpdate,
    isFollowedIssueUpdated,
    markFollowedIssueAsUpdated,
    syncFollowedIssueUpdateMarkers,
    error
  }
}
