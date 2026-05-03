export default defineNuxtPlugin(async () => {
    const { fetchUser } = useAuth()

    try {
        await fetchUser()
    } catch {
        // pas connecté ou token expiré
    }
})