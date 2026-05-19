export const useApi = () => {
  const config = useRuntimeConfig()

  return $fetch.create({
    baseURL: config.public.apiBase,
    credentials: 'include',

    async onRequest({ request, options }) {
      const token = useCookie('token').value
      if (token) {
        options.headers = options.headers || {}
        options.headers.Authorization = `Bearer ${token}`
      }
    },

    onResponseError({ response }) {
      console.error('API Error:', response.status)
    }
  })

}
