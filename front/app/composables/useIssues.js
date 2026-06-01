export const useIssues = () => {
    const api = useApi()

    /**
   * État global des issues.
   */
    const issues = useState('issues', () => [])

    /**
   * Utilisateur actuellement connecté.
   */
    const { user } = useAuth()

    /**
   * État global des erreurs liées aux issues.
   */
    const error = ref(null)

    /**
   * Statuts autorisés (aligné backend)
   */
    const VALID_STATUSES = ['OUVERT', 'EN_ATTENTE', 'INCOMPLET', 'TERMINE']

    /**
   * Récupère toutes les issues
   */
    const fetchIssues = async () => {
        try {
            const data = await api('/issues')
            issues.value = data
            return data
        } catch (e) {
            error.value = e?.data?.message || 'FETCH_ISSUES_ERROR'
            return []
        }
    }

     /**
    * Récupère une issue par ID
    */
    const getIssueById = async (id) => {
        return await api(`/issues/${id}`)
    }

     /**
    * Création d'une issue
    */
    const createIssue = async (payload) => {
        error.value = null

        if (!user?.value?.id) {
            await navigateTo('/login')
            return
        }
        if (!payload?.title || !payload?.description || !payload?.categoryId) {
            error.value = 'INVALID_PAYLOAD'
            return
        }

        try {
            const created = await api('/issues', {
                method: 'POST',
                body: {
                    title: payload.title,
                    description: payload.description,
                    categoryId: payload.categoryId,
                    pendingCase: payload.pendingCase
                }
            })

            // refresh local state
            await fetchIssues()

            return created
        } catch (e) {
            error.value = e?.data?.message || 'CREATE_ISSUE_ERROR'
            return null
        }
    }

     /**
    * Suppression d'une issue
    */
    const deleteIssue = async (id) => {
        await api(`/issues/${id}`, {
            method: 'DELETE'
        })

        issues.value = issues.value.filter(i => i.id !== id)
    }

     /**
    * Mise à jour statut issue
    */
    const updateIssueStatus = async (id, newValue) => {
        if (!VALID_STATUSES.includes(newValue)) {
            error.value = 'INVALID_STATUS'
            return
        }

        await api(`/issues/${id}/update-status`, {
            method: 'POST',
            body: newValue
        })

        return newValue
    }

     /**
    * Toggle visibilité issue (private/public)
    */
    const updateIssuePrivacy = async (id) => {
        await api(`/issues/${id}/update-private`, {
            method: 'POST'
        })

        return true
    }

  /**
   * Récupère uniquement les issues affichables pour l'utilisateur connecté :
   * - Créées par lui
   * - Ouvertes et publiques
   * - Suivies par lui
   */
  const getDisplayableIssues = async () => {
    try {
      // Appel à la nouvelle route backend
      // Note: Si votre backend attend l'email en paramètre plutôt que via le token (Principal),
      // vous pouvez faire : api('/issues/displayable', { query: { email: user.value.email } })
      const data = await api('/issues/displayable')

      // Mise à jour de l'état global avec les issues filtrées
      issues.value = data
      return data
    } catch (e) {
      error.value = e?.data?.message || 'FETCH_DISPLAYABLE_ISSUES_ERROR'
      return []
    }
  }

    return {
        issues,
        fetchIssues,
        getIssueById,
        createIssue,
        updateIssuePrivacy,
        updateIssueStatus,
        deleteIssue,
        getDisplayableIssues,
        error
    }
}
