export const useAuth = () => {
    const config = useRuntimeConfig()

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
            await $fetch(`${config.public.apiBase}/auth/login`, {
                method: 'POST',
                body: { email, password },
                credentials: 'include'
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
        try {
            const data = await $fetch(`${config.public.apiBase}/auth/me`, {
                credentials: 'include'
            })

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

    return { user, ready, login, fetchUser }
}