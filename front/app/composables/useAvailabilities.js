export const useAvailabilities = () => {
  const api = useApi()
  const availabilities = useState('availabilities', () => [])
  const error = ref(null)

  const fetchAvailabilities = async () => {
    error.value = null
    try {
      const data = await api('/availabilities')
      availabilities.value = Array.isArray(data) ? data : []
      return availabilities.value
    } catch (e) {
      error.value = e?.data?.message || 'FETCH_AVAILABILITIES_ERROR'
      availabilities.value = []
      return []
    }
  }

  const createAvailability = async (payload) => {
    error.value = null
    try {
      const created = await api('/availabilities', {
        method: 'POST',
        body: payload
      })
      await fetchAvailabilities()
      return created
    } catch (e) {
      error.value = e?.data?.message || 'CREATE_AVAILABILITY_ERROR'
      throw e
    }
  }

  const deleteAvailability = async (id) => {
    error.value = null
    try {
      await api(`/availabilities/${id}`, {
        method: 'DELETE'
      })
      availabilities.value = availabilities.value.filter(item => String(item.id) !== String(id))
      return true
    } catch (e) {
      error.value = e?.data?.message || 'DELETE_AVAILABILITY_ERROR'
      throw e
    }
  }

  return {
    availabilities,
    fetchAvailabilities,
    createAvailability,
    deleteAvailability,
    error
  }
}
