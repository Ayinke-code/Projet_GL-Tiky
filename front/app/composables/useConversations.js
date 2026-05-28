export const useConversations = () => {
  const api = useApi()

  /**
   * État des erreurs liées aux conversations.
   */
  const error = ref(null)

  /**
   * Détermine le rôle prioritaire.
   */
  const getPriorityRole = (roles) => {
    if (!roles) return 'STUDENT'

    const upperRoles = roles.map(r => String(r).toUpperCase())
    if (upperRoles.includes('ADMIN')) return 'ADMIN'
    if (upperRoles.includes('TEACHER')) return 'TEACHER'
    return 'STUDENT'
  }

  /**
   * Normalise un message reçu du backend.
   */
  const normalizeMessage = (msg) => {
    const rawRoles = Array.isArray(msg.user?.roles) ? msg.user.roles : [msg.user?.role || 'STUDENT']
    const role = getPriorityRole(rawRoles)

    return {
      id: msg.id,
      userId: msg.user?.id ?? null,
      auteur: msg.user?.name || msg.user?.email || 'Anonyme',
      role,
      roles: [role],
      date: msg.createdAt,
      contenu: msg.content ?? msg.contenu
    }
  }

  /**
   * Récupère les messages d'un ticket.
   */
  const getIssueMessages = async (issueId) => {
    try {
      const response = await api(`/conversations/issue/${issueId}`)
      if (!response?.conversation?.messages) {
        return []
      }

      return response.conversation.messages.map(normalizeMessage)
    } catch (e) {
      console.error('Error fetching messages:', e)
      error.value = e?.data?.message || 'UNKNOWN_ERROR'
      return []
    }
  }

  /**
   * Initialise une conversation pour un ticket (crée si nécessaire).
   */
  const initializeConversation = async (issueId) => {
    try {
      const response = await api(`/conversations/issue/${issueId}/initialize`, {
        method: 'POST'
      })

      if (!response?.conversation?.id) {
        throw new Error('Failed to initialize conversation')
      }

      return response.conversation
    } catch (e) {
      console.error('Error initializing conversation:', e)
      error.value = e?.data?.message || e?.message || 'UNKNOWN_ERROR'
      throw e
    }
  }

  /**
   * Envoie un message sur une issue.
   */
  const sendIssueMessage = async (issueId, content) => {
    try {
      // Initialiser/créer la conversation si elle n'existe pas
      const conversation = await initializeConversation(issueId)

      const conversationId = conversation.id
      const message = await api(`/conversations/${conversationId}/messages`, {
        method: 'POST',
        body: { content, issueId }
      })

      return normalizeMessage(message)
    } catch (e) {
      console.error('Error sending message:', e)
      error.value = e?.data?.message || e?.message || 'UNKNOWN_ERROR'
      throw e
    }
  }

  return Object.assign(api, {
    request: api,
    getIssueMessages,
    sendIssueMessage,
    initializeConversation,
    normalizeMessage,
    getPriorityRole,
    error
  })
}

