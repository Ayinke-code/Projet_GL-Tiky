/**
 * Plugin Nuxt exécuté au démarrage de l'application.
 * Initialise l'état d'authentification depuis le cookie de session.
 */
export default defineNuxtPlugin(() => {
    useAuth()
})