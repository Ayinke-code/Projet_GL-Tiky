import { Client } from '@stomp/stompjs'

let stompClient = null

export const useNotifications = () => {
  const notifications = useState('notifications', () => [])
  const api = useApi()


  const fetchHistory = async (userId) => {
    try {
      const data = await api(`/notifications/user/${userId}`)
      notifications.value = data
    } catch (error) {
      console.error('Erreur lors de la récupération des notifications', error)
    }
  }

  const connectToWebSocket = (userId, token) => {
    stompClient = new Client({
      brokerURL: 'ws://localhost:8080/api/ws/notifications',
      connectHeaders: {
        Authorization: `Bearer ${token}`
      },
      onConnect: () => {
        console.log('Connecté au WebSocket !')

        stompClient.subscribe(`/topic/notifications/user/${userId}`, (message) => {
          const newNotification = JSON.parse(message.body)
          notifications.value.unshift(newNotification)
        })
      },
      onStompError: (frame) => {
        console.error('Erreur STOMP : ', frame.headers['message'])
      }
    })

    stompClient.activate()
  }

  const disconnect = () => {
    if (stompClient && stompClient.active) {
      stompClient.deactivate()
      console.log('WebSocket déconnecté')
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
