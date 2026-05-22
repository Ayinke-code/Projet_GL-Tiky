import { ROLE, ROLE_PRIORITY } from '~/utils/roles'

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
    if (!roles) return ROLE.STUDENT

    const upperRoles = roles.map(r => String(r).toUpperCase())
    return ROLE_PRIORITY.find(role => upperRoles.includes(role) || upperRoles.includes(`ROLE_${role}`)) || ROLE.STUDENT
  }

  const normalizeUser = (user) => {
    if (!user) return null

    const rawRoles = Array.isArray(user.roles) ? user.roles : [user.role || ROLE.STUDENT]
    const role = getPriorityRole(rawRoles)

    return {
      id: user.id ?? null,
      name: user.name || user.email || 'Anonyme',
      email: user.email || '',
      role,
      roles: [role],
      displayRole: user.displayRole || role
    }
  }

  /**
   * Normalise un message reçu du backend.
   */
  const normalizeMessage = (msg) => {
    const user = normalizeUser(msg.user)
    const replyToMessage = msg.replyToMessage || msg.replyTo || null
    const replyToUser = normalizeUser(replyToMessage?.user)

    return {
      id: msg.id,
      userId: user?.id ?? null,
      auteur: user?.name || 'Anonyme',
      role: user?.role || ROLE.STUDENT,
      roles: user?.roles || [ROLE.STUDENT],
      date: msg.createdAt,
      contenu: msg.content ?? msg.contenu,
      replyToMessageId: msg.replyToMessageId ?? replyToMessage?.id ?? null,
      replyTo: replyToMessage
        ? {
            id: replyToMessage.id ?? null,
            userId: replyToUser?.id ?? null,
            auteur: replyToUser?.name || 'Anonyme',
            role: replyToUser?.role || ROLE.STUDENT,
            roles: replyToUser?.roles || [ROLE.STUDENT],
            date: replyToMessage.createdAt,
            contenu: replyToMessage.content ?? replyToMessage.contenu
          }
        : null
    }
  }

  /**
   * Récupère les messages d'une issue.
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
   * Initialise une conversation pour une issue (crée si nécessaire).
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
  const sendIssueMessage = async (issueId, content, replyToMessageId = null) => {
    try {
      // Initialiser/créer la conversation si elle n'existe pas
      const conversation = await initializeConversation(issueId)

      const conversationId = conversation.id
      const body = { content, issueId }

      if (replyToMessageId !== null && replyToMessageId !== undefined) {
        body.replyToMessageId = replyToMessageId
      }

      const message = await api(`/conversations/${conversationId}/messages`, {
        method: 'POST',
        body
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
