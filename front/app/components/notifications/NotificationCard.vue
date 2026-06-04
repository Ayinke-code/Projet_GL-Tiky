<template>
  <div class="p-4 mb-2 rounded-lg border border-gray-100 bg-white shadow-sm hover:shadow-md transition-all duration-200 flex items-start gap-3 relative">

    <div class="flex-shrink-0 mt-0.5 z-10">
      <label class="relative flex items-center justify-center cursor-pointer">
        <input
          type="checkbox"
          class="peer sr-only"
          @change="$emit('mark-as-read', notification.id)"
        >
        <div class="w-5 h-5 rounded-full border-2 border-gray-300 peer-checked:border-[#00DC82] peer-checked:bg-[#00DC82] transition-all duration-200 flex items-center justify-center shadow-sm">
          <svg class="w-3.5 h-3.5 text-white opacity-0 peer-checked:opacity-100 transition-opacity duration-200" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="3">
            <path stroke-linecap="round" stroke-linejoin="round" d="M4.5 12.75l6 6 9-13.5" />
          </svg>
        </div>
      </label>
    </div>

    <div class="flex-1 min-w-0">

      <NuxtLink
        v-if="targetUrl"
        :to="targetUrl"
        class="block group"
        @click="$emit('mark-as-read', notification.id)"
      >
        <div class="flex justify-between items-start gap-2 mb-1">
          <h4 :class="`text-sm font-semibold flex items-center gap-1.5 group-hover:underline ${uiConfig.textColor}`">
            <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" class="w-4 h-4">
              <path stroke-linecap="round" stroke-linejoin="round" :d="uiConfig.iconPath" />
            </svg>
            {{ uiConfig.title }}
          </h4>

          <!-- Affichage de l'heure -->
          <span v-if="formattedTime" class="text-xs text-gray-400 whitespace-nowrap mt-0.5 group-hover:text-gray-500 transition-colors">
            {{ formattedTime }}
          </span>
        </div>

        <p class="text-sm text-gray-600 break-words leading-snug group-hover:text-gray-900 transition-colors">
          {{ notification.content }}
        </p>
      </NuxtLink>

      <!-- Si la notification n'a pas de lien cliquable -->
      <div v-else class="block">
        <div class="flex justify-between items-start gap-2 mb-1">
          <h4 :class="`text-sm font-semibold flex items-center gap-1.5 ${uiConfig.textColor}`">
            <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" class="w-4 h-4">
              <path stroke-linecap="round" stroke-linejoin="round" :d="uiConfig.iconPath" />
            </svg>
            {{ uiConfig.title }}
          </h4>

          <!-- Affichage de l'heure -->
          <span v-if="formattedTime" class="text-xs text-gray-400 whitespace-nowrap mt-0.5">
            {{ formattedTime }}
          </span>
        </div>

        <p class="text-sm text-gray-600 break-words leading-snug">
          {{ notification.content }}
        </p>
      </div>

    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  notification: {
    type: Object,
    required: true
  }
})

defineEmits(['mark-as-read'])

const targetUrl = computed(() => {
  if (props.notification.type === 'NEW_EVENT_INVITATION' && props.notification.eventId && props.notification.issueId) {
    return `/invitations`
  }

  if (props.notification.type === 'NEW_EVENT_INVITATION' && props.notification.eventId) {
    return '/calendar'
  }

  if (props.notification.issueId) {
    return `/issues/${props.notification.issueId}`
  }

  if (props.notification.eventId) {
    return '/calendar'
  }

  return null
})

const formattedTime = computed(() => {
  if (!props.notification.sendingHour) return null

  const date = new Date(props.notification.sendingHour)

  return new Intl.DateTimeFormat('fr-FR', {
    day: '2-digit',
    month: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  }).format(date).replace(',', ' à')
})

const uiConfig = computed(() => {
  switch (props.notification.type) {
    // --- TICKETS ET AFFECTATIONS ---
    case 'NEW_ISSUE_IN_CATEGORIE':
      return {
        title: 'Nouveau Ticket',
        textColor: 'text-purple-600',
        iconPath: 'M12 9v6m3-3H9m12 0a9 9 0 11-18 0 9 9 0 0118 0z'
      }
    case 'RESPONSE_TO_MANAGER_ISSUE':
      return {
        title: 'Demande de manager',
        textColor: 'text-orange-600',
        iconPath: 'M15.75 6a3.75 3.75 0 11-7.5 0 3.75 3.75 0 017.5 0zM4.501 20.118a7.5 7.5 0 0114.998 0A17.933 17.933 0 0112 21.75c-2.676 0-5.216-.584-7.499-1.632z'
      }
    case 'VALIDE_ISSUE':
      return {
        title: 'Mise à jour du Ticket',
        textColor: 'text-blue-600',
        iconPath: 'M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z'
      }
    case 'INCOMPLETE_ISSUE':
      return {
        title: 'Ticket Incomplet',
        textColor: 'text-red-600',
        iconPath: 'M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z'
      }
    case 'NEW_DEMANDE_MANAGER':
      return {
        title: 'Nouvelle Affectation',
        textColor: 'text-teal-600',
        iconPath: 'M20 13V6a2 2 0 00-2-2H6a2 2 0 00-2 2v7m16 0v5a2 2 0 01-2 2H6a2 2 0 01-2-2v-5m16 0h-2.586a1 1 0 00-.707.293l-2.414 2.414a1 1 0 01-.707.293h-3.172a1 1 0 01-.707-.293l-2.414-2.414A1 1 0 006.586 13H4'
      }

    // --- ÉVÉNEMENTS ---
    case 'NEW_EVENT_INVITATION':
      return {
        title: 'Nouvelle Invitation',
        textColor: 'text-indigo-600',
        iconPath: 'M6.75 3v2.25M17.25 3v2.25M3 18.75V7.5a2.25 2.25 0 012.25-2.25h13.5A2.25 2.25 0 0121 7.5v11.25m-18 0A2.25 2.25 0 005.25 21h13.5A2.25 2.25 0 0021 18.75m-18 0v-7.5A2.25 2.25 0 015.25 9h13.5A2.25 2.25 0 0121 11.25v7.5'
      }
    case 'UPDATE_EVENT':
      return {
        title: 'Événement Modifié',
        textColor: 'text-amber-600',
        iconPath: 'M12 6v6h4.5m4.5 0a9 9 0 11-18 0 9 9 0 0118 0z'
      }
    case 'EVENT_RESPONSE':
      return {
        title: 'Réponse à l\'événement',
        textColor: 'text-emerald-600',
        iconPath: 'M9 12.75L11.25 15 15 9.75M21 12a9 9 0 11-18 0 9 9 0 0118 0z'
      }
    case 'EVENT_CANCEL':
      return {
        title: 'Événement Annulé',
        textColor: 'text-rose-600',
        iconPath: 'M9.75 9.75l4.5 4.5m0-4.5l-4.5 4.5M21 12a9 9 0 11-18 0 9 9 0 0118 0z'
      }


    case 'NEW_OPEN_ISSUE':
      return {
        title: 'Demande de réouverture',
        textColor: 'text-cyan-600',
        iconPath: 'M16.023 9.348h4.992v-.001M2.985 19.644v-4.992m0 0h4.992m-4.993 0l3.181 3.183a8.25 8.25 0 0013.803-3.7M4.031 9.865a8.25 8.25 0 0113.803-3.7l3.181 3.182m0-4.991v4.99'
      }
    case 'ACCEPTED_OPEN_ISSUE':
      return {
        title: 'Réouverture Acceptée',
        textColor: 'text-emerald-600',
        iconPath: 'M13.5 10.5V6.75a4.5 4.5 0 119 0v3.75M3.75 21.75h10.5a2.25 2.25 0 002.25-2.25v-6.75a2.25 2.25 0 00-2.25-2.25H3.75a2.25 2.25 0 00-2.25 2.25v6.75a2.25 2.25 0 002.25 2.25z'
      }

    case 'OPEN_ISSUE':
      return {
        title: 'Ticket Réouvert',
        textColor: 'text-sky-600',
        iconPath: 'M13.5 10.5V6.75a4.5 4.5 0 119 0v3.75M3.75 21.75h10.5a2.25 2.25 0 002.25-2.25v-6.75a2.25 2.25 0 00-2.25-2.25H3.75a2.25 2.25 0 00-2.25 2.25v6.75a2.25 2.25 0 002.25 2.25z'
      }

    case 'CLOSING_ISSUE':
      return {
        title: 'Ticket Clôturé',
        textColor: 'text-slate-600',
        iconPath: 'M16.5 10.5V6.75a4.5 4.5 0 10-9 0v3.75m-.75 11.25h10.5a2.25 2.25 0 002.25-2.25v-6.75a2.25 2.25 0 00-2.25-2.25H6.75a2.25 2.25 0 00-2.25 2.25v6.75a2.25 2.25 0 002.25 2.25z'
      }

    default:
      return {
        title: 'Notification',
        textColor: 'text-gray-800',
        iconPath: 'M14.857 17.082a23.848 23.848 0 005.454-1.31A8.967 8.967 0 0118 9.75v-.7V9A6 6 0 006 9v.75a8.967 8.967 0 01-2.312 6.022c1.733.64 3.56 1.085 5.455 1.31m5.714 0a24.255 24.255 0 01-5.714 0m5.714 0a3 3 0 11-5.714 0'
      }
  }
})
</script>
