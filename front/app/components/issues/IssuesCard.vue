<template>
  <div
    class="w-full bg-white hover:bg-gray-50 transition-colors rounded-xl px-5 py-3 flex flex-col sm:flex-row sm:items-center justify-between gap-4 shadow-sm border border-gray-200 cursor-pointer"
    @click="navigateTo(`/issues/${issue.id}`)"
  >

    <div class="flex flex-wrap items-center gap-4 min-w-0">
      <h3 class="font-semibold text-sm text-gray-900 truncate max-w-[250px] sm:max-w-[400px]">
        {{ issue.title }}
      </h3>

      <div class="flex items-center gap-2 flex-wrap">
        <span class="bg-blue-50 text-blue-600 border border-blue-100 text-[11px] px-2.5 py-0.5 rounded font-medium">
          {{ issue.categoryName || 'Sans catégorie' }}
        </span>

        <span
          :class="getStatusBadgeClass(issue.status)"
          class="px-2.5 py-0.5 rounded-full text-[11px] font-bold"
        >
          {{ getStatusLabel(issue.status) }}
        </span>

        <span v-if="issue.private" class="text-xs" title="Ticket privé">🔒</span>
      </div>
    </div>

    <div class="flex items-center justify-between sm:justify-end gap-6 ml-auto w-full sm:w-auto border-t border-gray-100 pt-2 sm:pt-0 sm:border-t-0">
      <div class="text-[12px] text-gray-500 whitespace-nowrap">
        {{ formatDateTime(issue.createdAt) }}
      </div>

      <!-- Le bouton de suppression s'affiche uniquement pour les profs et admins -->
      <UButton
        v-if="isTeacher"
        @click.stop="emit('delete', issue)"
        variant="ghost"
        size="sm"
        color="error"
        icon="i-lucide-trash-2"
        class="cursor-pointer text-red-500 hover:text-red-600 hover:bg-red-50 rounded-lg"
      />
    </div>

  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  issue: Object,
  updatingStatusId: Number
})

const emit = defineEmits(['delete'])

// Récupération de l'utilisateur connecté pour la gestion des droits d'affichage
const { user } = useAuth()

/**
 * Vérifie si l'utilisateur connecté dispose des droits d'enseignant ou d'administrateur
 */
const isTeacher = computed(() => {
  return user.value?.roles?.includes('TEACHER') || user.value?.roles?.includes('ADMIN') || false
})

/**
 * Formate une chaîne de date en format lisible (JJ/MM/AAAA HH:MM)
 */
const formatDateTime = (dateStr) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return date.toLocaleDateString('fr-FR', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  })
}

/**
 * Traduction des libellés de statuts
 */
const getStatusLabel = (status) => {
  const labels = {
    'OUVERT': 'Ouvert',
    'EN_ATTENTE': 'En attente',
    'INCOMPLET': 'Incomplet',
    'TERMINE': 'Terminé'
  }
  return labels[status] || status
}

/**
 * Couleurs des badges de statuts adaptées au thème clair
 */
const getStatusBadgeClass = (status) => {
  const classes = {
    'OUVERT': 'bg-yellow-100 text-yellow-800 border border-yellow-200',
    'EN_ATTENTE': 'bg-gray-100 text-gray-700 border border-gray-200',
    'INCOMPLET': 'bg-orange-100 text-orange-800 border border-orange-200',
    'TERMINE': 'bg-green-100 text-green-800 border border-green-200'
  }
  return classes[status] || 'bg-gray-100 text-gray-700'
}
</script>
