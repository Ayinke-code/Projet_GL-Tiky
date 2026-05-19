export const useApi = () => {
  const config = useRuntimeConfig()

  const request = $fetch.create({
    baseURL: config.public.apiBase,
    credentials: 'include',

    onResponseError({ response }) {
      console.error('API Error:', response.status)
    }
  })

  /**
   * Normalise un message reçu du backend
   */
  const normalizeMessage = (msg) => {
    const rawRoles = Array.isArray(msg.user?.roles) ? msg.user.roles : [msg.user?.role || 'STUDENT']
    const role = getPriorityRole(rawRoles)
    return {
      id: msg.id,
      userId: msg.user?.id ?? null,
      auteur: msg.user?.name || msg.user?.email || 'Anonyme',
      role: role,
      roles: [role],
      date: msg.createdAt,
      contenu: msg.content ?? msg.contenu
    }
  }

  /**
   * Détermine le rôle prioritaire
   */
  const getPriorityRole = (roles) => {
    if (!roles) return 'STUDENT'
    const upperRoles = roles.map(r => String(r).toUpperCase())
    if (upperRoles.includes('ADMIN')) /*|| upperRoles.includes('ROLE_ADMIN'))*/  return 'ADMIN'
    if (upperRoles.includes('TEACHER')) /*|| upperRoles.includes('ROLE_TEACHER'))*/ return 'TEACHER'
    return 'STUDENT'
  }

  /**
   * Récupère les messages d'une issue
   */
  const getIssueMessages = async (issueId) => {
    try {
      const response = await request(`/conversations/issue/${issueId}`)
      if (!response?.conversation?.messages) {
        return []
      }
      return response.conversation.messages.map(normalizeMessage)
    } catch (error) {
      console.error('Error fetching messages:', error)
      return []
    }
  }

  /**
   * Envoie un message sur une issue
   */
  const sendIssueMessage = async (issueId, content) => {
    try {
      const response = await request(`/conversations/issue/${issueId}`)
      if (!response?.conversation?.id) {
        throw new Error('Conversation not found')
      }
      const conversationId = response.conversation.id
      const message = await request(`/conversations/${conversationId}/messages`, {
        method: 'POST',
        body: { content, issueId }
      })

      return normalizeMessage(message)
    } catch (error) {
      console.error('Error sending message:', error)
      throw error
    }
  }

  return Object.assign(request, {
    request,
    getIssueMessages,
    sendIssueMessage,
    normalizeMessage,
    getPriorityRole
  })
}
