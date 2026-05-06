/**
 * Plugin Nuxt exécuté au démarrage de l'application.
 * <p>
 * Initialise l'état d'authentification en tentant de récupérer
 * l'utilisateur courant via le cookie de session.
 * <p>
 * Permet de restaurer la session utilisateur automatiquement
 * lors du rechargement de la page.
 */
export default defineNuxtPlugin(async () => {
    const { fetchUser } = useAuth()

    try {
        await fetchUser()
    } catch {
        // pas connecté ou token expiré
    }
})