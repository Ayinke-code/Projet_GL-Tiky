export const useAuth = () => {
    const api = useApi()

    /**
     * État global de l'utilisateur connecté.
     * Null si aucun utilisateur n'est authentifié.
     */
    const user = useState('user', () => null)

    /**
     * Token WebSocket pour la connexion temps réel
     */
    const wsToken = useState('wsToken', () => null)

    /**
     * Indique si l'état d'authentification a été initialisé.
     * Devient true après l'appel à /auth/me.
     */
    const ready = useState('ready', () => false)

    const setSession = (data) => {
        user.value = data
        if (data?.wsToken) {
            wsToken.value = data.wsToken
            if (process.client) localStorage.setItem('wsToken', data.wsToken)
        }
        ready.value = true
    }

    const clearSession = () => {
        user.value = null
        wsToken.value = null
        if (process.client) localStorage.removeItem('wsToken')
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
            // Réinitialisation user en cas d'erreur
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
        try {
            const data = await api('/auth/me')

            setSession(data)
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

    return { user, wsToken, ready, login, fetchUser, logout }
}
