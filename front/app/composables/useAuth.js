export const useAuth = () => {
    const config = useRuntimeConfig()
    const user = useState('user', () => null)

    const login = async (email, password) => {
        await $fetch(`${config.public.apiBase}/auth/login`, {
            method: 'POST',
            body: { email, password },
            credentials: 'include'
        })

        await fetchUser()
    }

    const fetchUser = async () => {
        try {
            const data = await $fetch(`${config.public.apiBase}/auth/me`, {
                credentials: 'include'
            })

            user.value = data
        } catch (e) {
            user.value = null
            throw e
        }
    }

    return { user, login, fetchUser }
}