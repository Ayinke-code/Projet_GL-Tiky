export const useApi = () => {
  const config = useRuntimeConfig()

  return $fetch.create({
    baseURL: config.public.apiBase,
    credentials: 'include',

    onResponseError({ response }) {
      console.error('API Error:', response.status)
    }
  })

}