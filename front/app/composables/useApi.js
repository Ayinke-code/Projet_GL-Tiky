export const useApi = () => {
  const config = useRuntimeConfig()

  const request = $fetch.create({
    baseURL: config.public.apiBase,
    credentials: 'include',

    onResponseError({ response }) {
      console.error('API Error:', response.status)
    }
  })

  return Object.assign(request, {
    request,
  })
}
