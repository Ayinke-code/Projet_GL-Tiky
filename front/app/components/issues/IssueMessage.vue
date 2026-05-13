<template>
  <article class="rounded-2xl border border-gray-200 bg-white p-4 shadow-sm">
    <div class="mb-2 flex items-start justify-between gap-4">
      <div>
        <p class="text-sm font-semibold text-gray-900">{{ auteur }}</p>
        <p class="text-xs text-gray-500">{{ displayRole }}</p>
      </div>
      <time class="text-xs text-gray-400">{{ formattedDate }}</time>
    </div>
    <p class="whitespace-pre-line text-sm leading-6 text-gray-700">{{ contenu }}</p>
  </article>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  auteur: { type: String, default: 'Anonyme' },
  role: { type: String, default: '' },
  roles: { type: Array, default: () => [] },
  date: { type: [String, Date], default: '' },
  contenu: { type: String, default: '' }
})

const displayRole = computed(() => {
  const roleToDisplay = props.role?.toUpperCase() || ''
  if (!roleToDisplay) return ''

  const roleMap = {
    'ADMIN': 'Administrateur',
    'TEACHER': 'Enseignant',
    'STUDENT': 'Étudiant',
    'USER': 'Utilisateur'
  }

  const normalizedRole = roleToDisplay.startsWith('ROLE_') ? roleToDisplay.substring(5) : roleToDisplay
  return roleMap[normalizedRole] || normalizedRole
})

const formattedDate = computed(() => {
  if (!props.date) return ''
  const value = new Date(props.date)
  return Number.isNaN(value.getTime()) ? String(props.date) : value.toLocaleString('fr-FR')
})
</script>

