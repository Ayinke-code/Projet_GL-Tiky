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
      auteur: issue?.auteur ?? issue?.createdBy?.name ?? issue?.createdBy?.email ?? '',
      professeur: issue?.professeur ?? issue?.manager?.name ?? issue?.manager?.email ?? '',
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

  const getIssueMessages = async (issueId) => {
    try {
      const response = await request(`/conversations/issue/${issueId}`)
      if (!response?.conversation?.messages) {
        return []
      }
      return response.conversation.messages.map(msg => ({
        id: msg.id,
        auteur: msg.user?.name || msg.user?.email || 'Anonyme',
        role: 'user',
        date: msg.createdAt,
        contenu: msg.content
      }))
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
      await request(`/conversations/${conversationId}/messages`, {
        method: 'POST',
        body: { content, issueId }
      })
      return true
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
    normalizeIssue
  })
}
