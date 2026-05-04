export default defineNuxtRouteMiddleware(async (to) => {

    // Ne s’exécute que côté client
    if (import.meta.server) return

    const { user, ready } = useAuth()

    // Routes accessibles sans authentification
    const publicPages = ['/login', '/register', '/forgot-password']
    if (publicPages.includes(to.path)) return

    // Attend que l’état d’authentification soit initialisé (/me terminé)
    if (!ready.value) {
        await new Promise((resolve) => {
            const stop = watch(ready, (val) => {
                if (val) { stop(); resolve() }
            })
        })
    }

    // Si aucun utilisateur après initialisation → redirection vers login
    if (!user.value) {
        window.location.href = '/login'
        return abortNavigation()
    }
})