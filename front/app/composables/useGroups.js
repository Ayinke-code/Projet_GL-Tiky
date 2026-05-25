export const useGroups = () => {
    const api = useApi()

    const groups = useState('groups', () => [])
    const loading = ref(false)
    const error = ref(null)

    /**
     * Récupère tous les groupes, ou filtrés par type si fourni.
     *
     * @param {string|null} type - 'PROMOTION' | 'OPTION' | 'PROJECT' | null
     */
    const fetchGroups = async (type = null) => {
        loading.value = true
        error.value = null
        try {
            const url = type ? `/groups/type/${type}` : '/groups'
            groups.value = await api(url)
        } catch (err) {
            error.value = err?.data?.error ?? 'Erreur lors du chargement des groupes'
            throw err
        } finally {
            loading.value = false
        }
    }

    /**
     * Crée un nouveau groupe d'étudiants.
     *
     * @param {{ name: string, type: string, memberIds: number[] }} payload
     * @returns {Promise<Object>} groupe créé
     */
    const createGroup = async (payload) => {
        loading.value = true
        error.value = null
        try {
            const created = await api('/groups', {
                method: 'POST',
                body: payload,
            })
            groups.value = [...groups.value, created]
            return created
        } catch (err) {
            error.value = err?.data?.error ?? 'Erreur lors de la création du groupe'
            throw err
        } finally {
            loading.value = false
        }
    }

    const deleteGroup = async (id) => {
        await api(`/groups/${id}`, { method: 'DELETE' })
        groups.value = groups.value.filter(g => g.id !== id)
    }

    return { groups, loading, error, fetchGroups, createGroup, deleteGroup }
}
