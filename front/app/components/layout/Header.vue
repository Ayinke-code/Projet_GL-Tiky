<template>
  <header class="h-16 bg-white shadow-sm flex items-center justify-between px-4 md:px-8" :aria-label="$t('header_label')">
    <div class="flex items-center">
      <button
        @click="$emit('toggle-sidebar')"
        class="md:hidden mr-4 text-gray-500 hover:text-gray-700 focus:outline-none"
        :aria-label="$t('sidebar.toggle')"
      >
        <i class="fa-solid fa-bars text-xl"></i>
      </button>
      <h1 class="text-xl md:text-2xl font-bold text-gray-700">
        Tiky
      </h1>
    </div>

    <div class="flex items-center space-x-6">
      <div
        class="relative"
        ref="dropdownContainer"
      >
        <button
          @click.stop="toggleNotifications"
          class="relative text-gray-500 hover:text-blue-600 transition-colors focus:outline-none mt-1 p-1"
        >
          <svg
            xmlns="http://www.w3.org/2000/svg"
            fill="none"
            viewBox="0 0 24 24"
            stroke-width="1.5"
            stroke="currentColor"
            class="w-7 h-7"
          >
            <path
              stroke-linecap="round"
              stroke-linejoin="round"
              d="M21.75 6.75v10.5a2.25 2.25 0 01-2.25 2.25h-15a2.25 2.25 0 01-2.25-2.25V6.75m19.5 0A2.25 2.25 0 0019.5 4.5h-15a2.25 2.25 0 00-2.25 2.25m19.5 0v.243a2.25 2.25 0 01-1.07 1.916l-7.5 4.615a2.25 2.25 0 01-2.36 0L3.32 8.91a2.25 2.25 0 01-1.07-1.916V6.75"
            />
          </svg>

          <span
            v-if="unreadCount > 0"
            class="absolute -top-1 -right-1 bg-red-500 text-white text-[10px] font-bold rounded-full h-4 w-4 flex items-center justify-center"
          >
            {{ unreadCount > 99 ? '99+' : unreadCount }}
          </span>
        </button>

        <div
          v-if="isDropdownOpen"
          @click.stop
          class="absolute right-0 mt-3 w-80 sm:w-96 bg-white rounded-lg shadow-xl border border-gray-100 z-50 overflow-hidden origin-top-right"
        >
          <div class="p-3 border-b border-gray-100 bg-white flex justify-between items-center">
            <h3 class="font-semibold text-gray-800">
              {{ $t('notifications') }}
            </h3>
          </div>

          <div class="max-h-[400px] overflow-y-auto p-2 scrollbar-thin scrollbar-thumb-gray-200">
            <div
              v-if="notifications.length === 0"
              class="flex flex-col items-center justify-center py-10 px-4 text-center bg-white"
            >
              <svg
                xmlns="http://www.w3.org/2000/svg"
                class="h-12 w-12 text-gray-300 mb-3"
                fill="none"
                viewBox="0 0 24 24"
                stroke="currentColor"
              >
                <path
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  stroke-width="2"
                  d="M20 13V6a2 2 0 00-2-2H6a2 2 0 00-2 2v7m16 0v5a2 2 0 01-2 2H6a2 2 0 01-2-2v-5m16 0h-2.586a1 1 0 00-.707.293l-2.414 2.414a1 1 0 01-.707.293h-3.172a1 1 0 01-.707-.293l-2.414-2.414A1 1 0 006.586 13H4"
                />
              </svg>
              <p class="text-gray-600 text-sm font-medium">
                {{ $t('no_notifications') }}
              </p>
              <p class="text-gray-400 text-xs mt-1">
                {{ $t('up_to_date') }}
              </p>
            </div>

            <NotificationCard
              v-else
              v-for="notif in notifications"
              :key="notif.id"
              :notification="notif"
              @mark-as-read="handleMarkAsRead"
            />
          </div>
        </div>
      </div>

      <LanguageSelector />

      <ClientOnly>
        <div class="h-9 w-9 rounded-full bg-blue-100 flex items-center justify-center font-bold text-blue-600">
          {{ userInitial }}
        </div>
        <template #fallback>
          <div class="h-9 w-9 rounded-full bg-slate-200 animate-pulse" />
        </template>
      </ClientOnly>
    </div>
  </header>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useNotifications } from '~/composables/useNotifications.js'
import NotificationCard from '~/components/notifications/NotificationCard.vue'
import LanguageSelector from '~/components/layout/LanguageSelector.vue'

defineEmits(['toggle-sidebar'])

const { user, wsToken, ready, fetchUser } = useAuth()
const { connectToWebSocket, notifications, unreadCount, fetchHistory, handleMarkAsRead } = useNotifications()

const isDropdownOpen = ref(false)
const dropdownContainer = ref(null)

const userInitial = computed(() => {
  if (ready.value && user.value?.name) return user.value.name.charAt(0).toUpperCase()
  return '?'
})

const toggleNotifications = () => {
  isDropdownOpen.value = !isDropdownOpen.value
}

const handleClickOutside = (event) => {
  if (isDropdownOpen.value && dropdownContainer.value && !dropdownContainer.value.contains(event.target)) {
    isDropdownOpen.value = false
  }
}

onMounted(async () => {
  if (!ready.value) {
    await fetchUser().catch(() => { })
  }
  document.addEventListener('click', handleClickOutside)
})

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
})

const nuxtApp = useNuxtApp()

watch(ready, async (isReady) => {
  if (!process.client) return
  if (isReady && user.value?.id && wsToken.value) {
    await fetchHistory(user.value.id)
    connectToWebSocket(user.value.id, wsToken.value, (notif) => {
      window.dispatchEvent(new CustomEvent('notification:received', { detail: notif }))
    })
  }
}, { immediate: true })
</script>
