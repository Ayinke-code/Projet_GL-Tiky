import { computed } from 'vue'

export const useAuth = () => {
    const api = useApi()

    /**
     * État global de l'utilisateur connecté.
     * Null si aucun utilisateur n'est authentifié.
     */
    const user = useState('user', () => null)

    /**
     * Indique si l'état d'authentification a été initialisé.
     * Devient true après l'appel à /auth/me.
     */
    const ready = useState('ready', () => false)

    /**
     * Rôles métier temporaires utilisés pour les badges de la sidebar.
     * <p>
     * Permet par exemple d'afficher qu'un utilisateur a créé une issue
     * après la soumission du formulaire.
     */
    const issueRoles = useState('issueRoles', () => [])

    const normalizeRoles = (data) => {
        const rawRoles = data?.roles

        if (Array.isArray(rawRoles)) {
            return rawRoles.filter(Boolean).map(role => String(role).toUpperCase())
        }

        if (rawRoles instanceof Set) {
            return Array.from(rawRoles).filter(Boolean).map(role => String(role).toUpperCase())
        }

        if (rawRoles && typeof rawRoles === 'object') {
            return Object.values(rawRoles).filter(Boolean).map(role => String(role).toUpperCase())
        }

        if (data?.role) {
            return [String(data.role).toUpperCase()]
        }

        return []
    }

    const normalizeUser = (data) => {
        if (!data) {
            return null
        }

        // Le backend renvoie déjà le rôle filtré par priorité (ADMIN > TEACHER > STUDENT)
        // Mais par sécurité, on recalcule la priorité si une liste de rôles est présente
        const rawRoles = Array.isArray(data.roles) ? data.roles : (data.role ? [data.role] : [])
        const upperRoles = rawRoles.map(r => String(r).toUpperCase())

        let primaryRole = String(data.role || 'STUDENT').toUpperCase()

        if (upperRoles.includes('ADMIN') || upperRoles.includes('ROLE_ADMIN')) {
            primaryRole = 'ADMIN'
        } else if (upperRoles.includes('TEACHER') || upperRoles.includes('ROLE_TEACHER')) {
            primaryRole = 'TEACHER'
        } else if (upperRoles.includes('STUDENT') || upperRoles.includes('ROLE_STUDENT')) {
            primaryRole = 'STUDENT'
        }

        return {
            ...data,
            role: primaryRole,
            roles: [primaryRole],
        }
    }

    const setSession = (data, { resetIssueRoles = false } = {}) => {
        user.value = normalizeUser(data)
        ready.value = true

        if (resetIssueRoles) {
            issueRoles.value = []
        }
    }

    const currentRoles = computed(() => normalizeRoles(user.value))

    const hasRole = (...expectedRoles) =>
        expectedRoles.some(role => currentRoles.value.includes(String(role).toUpperCase()))

    const isAuthenticated = computed(() => !!user.value)

    const isIssueCreator = computed(() =>
        issueRoles.value.includes('CREATOR') || hasRole('ADMIN', 'TEACHER')
    )

    const isIssueFollower = computed(() =>
        issueRoles.value.includes('FOLLOWER') || hasRole('STUDENT')
    )

    const isParticipant = computed(() =>
        issueRoles.value.includes('PARTICIPANT') || isAuthenticated.value
    )

    const canAccessCategories = computed(() => isAuthenticated.value)
    const canAccessIssues = computed(() => isAuthenticated.value)
    const canCreateIssue = computed(() => isAuthenticated.value)

    const syncSession = (data) => {
        setSession(data, { resetIssueRoles: true })
    }

    const addIssueRole = (role) => {
        const normalizedRole = String(role || '').toUpperCase()

        if (!normalizedRole) {
            return
        }

        if (!issueRoles.value.includes(normalizedRole)) {
            issueRoles.value = [...issueRoles.value, normalizedRole]
        }
    }

    const clearSession = () => {
        user.value = null
        issueRoles.value = []
        ready.value = true
    }

    /**
     * Authentifie un utilisateur via email et mot de passe.
     * <p>
     * Appelle l'API /auth/login qui crée un cookie HttpOnly.
     * Puis récupère les informations utilisateur via /auth/me.
     *
     * @param email email de l'utilisateur
     * @param password mot de passe utilisateur
     * @return true si authentification réussie
     * @throws Error INVALID_CREDENTIALS si les identifiants sont incorrects (401)
     * @throws Error UNKNOWN_ERROR pour toute autre erreur API
     */
    const login = async (email, password) => {
        try {
            // Appel API pour créer la session (cookie HttpOnly)
            await api('/auth/login', {
                method: 'POST',
                body: { email, password },
            })

            // Récupération des informations utilisateur après login
            await fetchUser()
            return true
        } catch (e) {
            // Réinitialisation user en cas d’erreur
            clearSession()

            // Erreur métier : identifiants invalides
            if (e?.response?.status === 401) {
                throw new Error('INVALID_CREDENTIALS')
            }

            // Autres erreurs
            throw new Error('UNKNOWN_ERROR')
        }

    }

    /**
     * Récupère l'utilisateur courant à partir du cookie de session.
     * <p>
     * Utilise l'endpoint /auth/me pour restaurer l'état utilisateur.
     * Met à jour l'état global `user` et `ready`.
     *
     * @throws erreur API si le token est invalide ou expiré
     */
    const fetchUser = async () => {
        console.log("fetching")
        try {
            const data = await api('/auth/me')
            console.log("data", data)
            setSession(data, { resetIssueRoles: true })
        } catch (e) {
            // Si token invalide ou expiré
            clearSession()
            throw e
        }
    }

        /**
     * Déconnecte l'utilisateur courant.
     * <p>
     * Supprime le cookie de session côté serveur
     * puis réinitialise l'état utilisateur côté client.
     * <p>
     * Redirige ensuite vers la page de connexion.
     *
     * @throws erreur API si la déconnexion échoue
     */
    const logout = async () => {
        try {
            // Suppression de la session serveur
            await api('/auth/logout', { method: 'POST' })
            // Réinitialisation utilisateur local
            clearSession()

            // Redirection vers login
            await navigateTo('/login')
        } catch (e) {
            throw e
        }
    }

    return {
        user,
        ready,
        isAuthenticated,
        isIssueCreator,
        isIssueFollower,
        isParticipant,
        canAccessCategories,
        canAccessIssues,
        canCreateIssue,
        login,
        fetchUser,
        logout,
        syncSession,
        addIssueRole,
    }
}
