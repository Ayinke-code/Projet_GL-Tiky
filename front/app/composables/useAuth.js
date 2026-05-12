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
            user.value = null

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
            user.value = data
            ready.value = true
        } catch (e) {
            // Si token invalide ou expiré
            user.value = null
            // Marque la fin de l’initialisation (succès ou échec)
            ready.value = true
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
            user.value = null

            // Redirection vers login
            await navigateTo('/login')
        } catch (e) {
            throw e
        }
    }

    return { user, ready, login, fetchUser, logout }
}
