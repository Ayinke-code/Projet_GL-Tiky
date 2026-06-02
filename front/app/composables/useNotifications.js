import { Client } from '@stomp/stompjs'

let stompClient = null

export const useNotifications = () => {
  const notifications = useState('notifications', () => [])
  const api = useApi()

  const config = useRuntimeConfig()



  const fetchHistory = async (userId) => {
    try {
      const data = await api(`/notifications/user/${userId}`)
      notifications.value = data
    } catch (error) {
    }
  }

  const connectToWebSocket = (userId, token) => {
    const apiBase = config.public.apiBase
    const wsBaseUrl = apiBase.replace(/^http/, 'ws')

    const brokerURL = `${wsBaseUrl}/ws/notifications`
    stompClient = new Client({


      brokerURL: brokerURL,
      connectHeaders: {
        Authorization: `Bearer ${token}`
      },
      onConnect: () => {

        stompClient.subscribe(`/topic/notifications/user/${userId}`, (message) => {
          const newNotification = JSON.parse(message.body)
          notifications.value.unshift(newNotification)
        })
      },
      onStompError: (frame) => {
      }
    })

    stompClient.activate()
  }

  const disconnect = () => {
    if (stompClient && stompClient.active) {
      stompClient.deactivate()
    }
    notifications.value = []
  }

  const unreadCount = computed(() => notifications.value.length)

  // La méthode pour marquer comme lu une notification
  const handleMarkAsRead = async (id) => {
    try {
      await api(`/notifications/${id}/read`, { method: 'PUT' })

      notifications.value = notifications.value.filter(n => n.id !== id)

    } catch (error) {
      console.error('Erreur lors de la mise à jour de la notification')
      // Si erreur, il faudrait idéalement refetch l'historique
    }
  }

  return {
    notifications,
    unreadCount,
    fetchHistory,
    connectToWebSocket,
    disconnect,
    handleMarkAsRead,
  }
}
