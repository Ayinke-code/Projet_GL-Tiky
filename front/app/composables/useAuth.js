export const useAuth = () => {
    const config = useRuntimeConfig()
    const user = useState('user', () => null)

    const login = async (email, password) => {
        try {
            await $fetch(`${config.public.apiBase}/auth/login`, {
                method: 'POST',
                body: { email, password },
                credentials: 'include'
            })

            await fetchUser()
            return true
        } catch (e) {
            user.value = null

            if (e?.response?.status === 401) {
                throw new Error('INVALID_CREDENTIALS')
            }

            throw new Error('UNKNOWN_ERROR')
        }

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