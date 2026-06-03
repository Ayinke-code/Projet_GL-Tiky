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
    } catch (error) {}
  }

  const connectToWebSocket = (userId, token, onNewNotification = null) => {
    if (!process.client) return

    // Déconnecter toute connexion existante avant d'en créer une nouvelle
    if (stompClient?.active) {
      stompClient.deactivate()
    }

    const apiBase = config.public.apiBase
    const wsBaseUrl = apiBase.replace(/^http/, 'ws')
    const brokerURL = `${wsBaseUrl}/ws/notifications`

    // Référence locale pour éviter le bug de closure sur stompClient module-level
    const client = new Client({
      brokerURL: brokerURL,
      connectHeaders: {
        Authorization: `Bearer ${token}`
      },
      onConnect: () => {
        client.subscribe(`/topic/notifications/user/${userId}`, (message) => {
          const newNotification = JSON.parse(message.body)
          notifications.value = [newNotification, ...notifications.value]

          onNewNotification?.(newNotification)
        })
      },
      onStompError: (frame) => {}
    })

    stompClient = client
    client.activate()
  }

  const disconnect = () => {
    if (stompClient && stompClient.active) {
      stompClient.deactivate()
    }
    notifications.value = []
  }

  const unreadCount = computed(() => notifications.value.length)

  const handleMarkAsRead = async (id) => {
    try {
      await api(`/notifications/${id}/read`, { method: 'PUT' })
      notifications.value = notifications.value.filter(n => n.id !== id)
    } catch (error) {
      console.error('Erreur lors de la mise à jour de la notification')
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
