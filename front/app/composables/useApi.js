export const useApi = () => {
  const config = useRuntimeConfig()

  const request = $fetch.create({
    baseURL: config.public.apiBase,
    credentials: 'include',
    onResponseError({ response }) {
      console.error('API Error:', response.status)
    }
  })

  const normalizeCategory = (category) => ({
    ...category,
    ownerName: category?.ownerName || category?.owner || null,
    status: String(category?.status || '').toUpperCase() === 'ACTIVE' ? 'ACTIVE' : 'ARCHIVED'
  })

  const normalizeIssue = (issue) => {
    const category = issue?.category || issue?.categorie || {}

    // Le backend renvoie déjà le rôle filtré par priorité (ADMIN > TEACHER > STUDENT)
    // On s'assure que si ADMIN est présent dans les rôles, il est prioritaire
    const rawAuteurRoles = Array.isArray(issue?.roles) ? issue.roles : (issue?.createdBy?.roles || issue?.auteur?.roles || [issue?.auteur?.role || issue?.createdBy?.role || 'STUDENT'])
    const rawProfesseurRoles = Array.isArray(issue?.manager?.roles) ? issue.manager.roles : (issue?.professeur?.roles || issue?.manager?.roles || (issue?.professeur || issue?.manager ? [issue?.professeur?.role || issue?.manager?.role || 'TEACHER'] : []))

    const getPriorityRole = (roles) => {
      const upperRoles = roles.map(r => String(r).toUpperCase())
      if (upperRoles.includes('ADMIN') || upperRoles.includes('ROLE_ADMIN')) return 'ADMIN'
      if (upperRoles.includes('TEACHER') || upperRoles.includes('ROLE_TEACHER')) return 'TEACHER'
      return 'STUDENT'
    }

    const auteurRole = getPriorityRole(rawAuteurRoles)
    const professeurRole = (issue?.professeur || issue?.manager) ? getPriorityRole(rawProfesseurRoles) : null

    return {
      ...issue,
      idTick: issue?.idTick ?? issue?.id,
      titre: issue?.titre ?? issue?.title ?? '',
      statut: issue?.statut ?? issue?.status ?? 'OUVERT',
      categorie: {
        ...category,
        name: category?.name ?? category?.nom ?? '',
        nom: category?.nom ?? category?.name ?? ''
      },
      dateCreation: issue?.dateCreation ?? issue?.createdAt ?? null,
      estPrive: issue?.estPrive ?? issue?.isPrivate ?? false,
      visibilite: issue?.visibilite ?? (issue?.isPrivate ? 'Privé' : 'Public'),
      auteur: (issue?.auteur?.name || issue?.auteur) ?? issue?.createdBy?.name ?? issue?.createdBy?.email ?? '',
      auteurRole: auteurRole,
      auteurRoles: [auteurRole],
      professeur: (issue?.professeur?.name || issue?.professeur) ?? issue?.manager?.name ?? issue?.manager?.email ?? '',
      professeurRole: professeurRole,
      professeurRoles: professeurRole ? [professeurRole] : [],
      abonnes: issue?.abonnes ?? []
    }
  }

  const api = async (url, options = {}) => request(url, options)

  const getCategories = async () => {
    const data = await request('/categories')
    return Array.isArray(data) ? data.map(normalizeCategory) : []
  }

  const createCategory = async (payload) => {
    const created = await request('/categories', {
      method: 'POST',
      body: payload
    })
    return normalizeCategory(created)
  }

  const deleteCategory = async (id) => {
    await request(`/categories/${id}`, { method: 'DELETE' })
  }

  const getIssues = async () => {
    const data = await request('/issues')
    return Array.isArray(data) ? data.map(normalizeIssue) : []
  }

  const getIssueById = async (id) => normalizeIssue(await request(`/issues/${id}`))

  const createIssue = async (payload) => {
    const created = await request('/issues', {
      method: 'POST',
      body: payload
    })
    return normalizeIssue(created)
  }

  const deleteIssue = async (id) => {
    await request(`/issues/${id}`, { method: 'DELETE' })
  }

  const updateIssueStatus = async (id, statut) => {
    const updated = await request(`/issues/${id}/status`, {
      method: 'PUT',
      body: { statut }
    })
    return normalizeIssue(updated)
  }

  const getPriorityRole = (roles) => {
    if (!roles) return 'STUDENT'
    const upperRoles = roles.map(r => String(r).toUpperCase())
    if (upperRoles.includes('ADMIN') || upperRoles.includes('ROLE_ADMIN')) return 'ADMIN'
    if (upperRoles.includes('TEACHER') || upperRoles.includes('ROLE_TEACHER')) return 'TEACHER'
    return 'STUDENT'
  }

  const normalizeMessage = (msg) => {
    const rawRoles = Array.isArray(msg.user?.roles) ? msg.user.roles : [msg.user?.role || 'STUDENT']
    const role = getPriorityRole(rawRoles)
    return {
      id: msg.id,
      auteur: msg.user?.name || msg.user?.email || 'Anonyme',
      role: role,
      roles: [role],
      date: msg.createdAt,
      contenu: msg.content ?? msg.contenu
    }
  }

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

  return Object.assign(api, {
    request,
    getCategories,
    createCategory,
    deleteCategory,
    getIssues,
    getIssueById,
    createIssue,
    deleteIssue,
    updateIssueStatus,
    getIssueMessages,
    sendIssueMessage,
    normalizeCategory,
    normalizeIssue,
    normalizeMessage,
    getPriorityRole
  })
}
