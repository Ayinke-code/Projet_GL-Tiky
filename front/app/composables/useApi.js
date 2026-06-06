export const useApi = () => {
  const config = useRuntimeConfig()

  return $fetch.create({
    baseURL: config.public.apiBase,
    credentials: 'include',

    onResponseError({ response, request }) {
      console.error('API Error:', response.status)
      const url = typeof request === 'string' ? request : request?.url ?? ''
      const isAuthEndpoint = url.includes('/auth/login') || url.includes('/auth/me')
      if ((response.status === 401 || response.status === 403) && !isAuthEndpoint) {
        if (import.meta.client) {
          navigateTo('/login')
        }
      }
    }
  })

}
