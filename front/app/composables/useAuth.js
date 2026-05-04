export const useAuth = () => {
    const config = useRuntimeConfig()
    // État global utilisateur (partagé dans toute l’app)
    const user = useState('user', () => null)
    // Indique si la vérification d’authentification est terminée (/me appelé)
    const ready = useState('ready', () => false)

    // Authentification utilisateur
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

    // Récupération de l’utilisateur courant via le cookie
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