<template>
  <aside class="rounded-2xl border border-gray-200 bg-white p-5 shadow-sm">
    <h2 class="mb-4 text-lg font-semibold text-gray-900">Informations</h2>

    <dl class="space-y-3 text-sm">
      <div>
        <dt class="text-gray-500">Auteur</dt>
        <dd class="font-medium text-gray-900">{{ auteur || '—' }}</dd>
        <dd v-if="displayAuteurRoles" class="text-xs text-gray-400">{{ displayAuteurRoles }}</dd>
      </div>
      <div>
        <dt class="text-gray-500">Responsable</dt>
        <dd class="font-medium text-gray-900">{{ professeur || '—' }}</dd>
        <dd v-if="displayProfesseurRoles" class="text-xs text-gray-400">{{ displayProfesseurRoles }}</dd>
      </div>
      <div>
        <dt class="text-gray-500">Abonnés</dt>
        <dd class="font-medium text-gray-900">{{ abonnes?.length || 0 }}</dd>
      </div>
    </dl>
  </aside>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  auteur: { type: String, default: '' },
  auteurRole: { type: String, default: '' },
  auteurRoles: { type: Array, default: () => [] },
  professeur: { type: String, default: '' },
  professeurRole: { type: String, default: '' },
  professeurRoles: { type: Array, default: () => [] },
  abonnes: { type: Array, default: () => [] }
})

const formatRoles = (role) => {
  const roleToDisplay = role?.toUpperCase() || ''
  if (!roleToDisplay) return ''

  const roleMap = {
    'ADMIN': 'Administrateur',
    'TEACHER': 'Enseignant',
    'STUDENT': 'Étudiant',
    'USER': 'Utilisateur'
  }

  const normalizedRole = roleToDisplay.startsWith('ROLE_') ? roleToDisplay.substring(5) : roleToDisplay
  return roleMap[normalizedRole] || normalizedRole
}

const displayAuteurRoles = computed(() => formatRoles(props.auteurRole))
const displayProfesseurRoles = computed(() => formatRoles(props.professeurRole))
</script>

