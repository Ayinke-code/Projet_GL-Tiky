/**
 * Composable gérant les notifications de l'utilisateur courant.
 */
export const useNotifications = () => {
  const error = ref(null)
  const notifications = ref([])
  const unreadCount = computed(() =>
    notifications.value.filter(n => !n.read).length
  )

  /**
   * Récupère toutes les notifications de l'utilisateur courant.
   */
  const fetchNotifications = async () => {
    const api = useApi()
    error.value = null
    try {
      notifications.value = await api('/notifications')
    } catch (e) {
      error.value = e?.data?.message || 'FETCH_NOTIFICATIONS_ERROR'
      throw e
    }
  }

  /**
   * Marque une notification comme lue.
   * @param {number} id - identifiant de la notification
   */
  const markAsRead = async (id) => {
    const api = useApi()
    error.value = null
    try {
      await api(`/notifications/${id}/read`, { method: 'PATCH' })
      const notif = notifications.value.find(n => n.id === id)
      if (notif) notif.read = true
    } catch (e) {
      error.value = e?.data?.message || 'MARK_AS_READ_ERROR'
      throw e
    }
  }

  /**
   * Marque toutes les notifications comme lues.
   */
  const markAllAsRead = async () => {
    const api = useApi()
    error.value = null
    try {
      await api('/notifications/read-all', { method: 'PATCH' })
      notifications.value.forEach(n => n.read = true)
    } catch (e) {
      error.value = e?.data?.message || 'MARK_ALL_AS_READ_ERROR'
      throw e
    }
  }

  return {
    notifications,
    unreadCount,
    fetchNotifications,
    markAsRead,
    markAllAsRead,
    error
  }
}
