<template>
  <div class="relative" ref="bellRef">
    <button
      @click="toggleDropdown"
      class="relative p-2 text-gray-500 hover:text-gray-700 focus:outline-none"
    >
      <i class="fa-solid fa-bell text-xl"></i>
      <span
        v-if="unreadCount > 0"
        class="absolute -top-1 -right-1 h-5 w-5 rounded-full bg-red-500 text-white text-xs flex items-center justify-center font-bold"
      >
        {{ unreadCount > 9 ? '9+' : unreadCount }}
      </span>
    </button>

    <div
      v-if="isOpen"
      class="absolute right-0 mt-2 w-80 bg-white rounded-xl shadow-xl border border-gray-100 z-50 overflow-hidden"
    >
      <div class="flex items-center justify-between px-4 py-3 border-b border-gray-100">
        <h3 class="font-bold text-gray-800 text-sm">Notifications</h3>
        <button
          v-if="unreadCount > 0"
          @click="handleMarkAllAsRead"
          class="text-xs text-indigo-600 hover:text-indigo-800 font-medium"
        >
          Tout marquer comme lu
        </button>
      </div>

      <div class="max-h-96 overflow-y-auto">
        <div v-if="loading" class="p-4 space-y-3">
          <USkeleton class="h-12 w-full" />
          <USkeleton class="h-12 w-full" />
        </div>

        <div v-else-if="notifications.length === 0" class="p-6 text-center text-gray-400 text-sm">
          Aucune notification
        </div>

        <div v-else>
          <div
            v-for="notif in notifications"
            :key="notif.id"
            @click="handleNotifClick(notif)"
            class="px-4 py-3 border-b border-gray-50 cursor-pointer transition-colors"
            :class="notif.read ? 'bg-white hover:bg-gray-50' : 'bg-indigo-50 hover:bg-indigo-100'"
          >
            <div class="flex items-start gap-3">
              <div class="mt-0.5 shrink-0">
                <i
                  class="fa-solid text-sm"
                  :class="{
                    'fa-message text-indigo-500': notif.type === 'NEW_MESSAGE',
                    'fa-circle-check text-green-500': notif.type === 'STATUS_UPDATED',
                    'fa-lock text-orange-500': notif.type === 'VISIBILITY_UPDATED'
                  }"
                ></i>
              </div>
              <div class="flex-1 min-w-0">
                <p class="text-sm text-gray-800 leading-snug">{{ notif.content }}</p>
                <p class="text-xs text-gray-400 mt-1">{{ formatDate(notif.createdAt) }}</p>
              </div>
              <div v-if="!notif.read" class="h-2 w-2 rounded-full bg-indigo-500 mt-1 shrink-0"></div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="js">
const { notifications, unreadCount, fetchNotifications, markAsRead, markAllAsRead } = useNotifications()

const isOpen = ref(false)
const loading = ref(false)
const bellRef = ref(null)

const toggleDropdown = async () => {
  isOpen.value = !isOpen.value
  if (isOpen.value && notifications.value.length === 0) {
    loading.value = true
    try {
      await fetchNotifications()
    } finally {
      loading.value = false
    }
  }
}

const handleNotifClick = async (notif) => {
  if (!notif.read) await markAsRead(notif.id)
  isOpen.value = false
  await navigateTo(`/issues/${notif.issue?.id}`)
}

const handleMarkAllAsRead = async () => {
  await markAllAsRead()
}

const formatDate = (date) => {
  return new Date(date).toLocaleDateString('fr-FR', {
    day: 'numeric',
    month: 'short',
    hour: '2-digit',
    minute: '2-digit'
  })
}

// Fermer le dropdown en cliquant ailleurs
onMounted(() => {
  document.addEventListener('click', (e) => {
    if (bellRef.value && !bellRef.value.contains(e.target)) {
      isOpen.value = false
    }
  })
})
</script>
