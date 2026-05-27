<template>
  <Modal v-model:open="openModel" :title="isTeacher ? 'Gestion des participants' : 'Réponse de l\'enseignant'">
    <div class="p-2 max-h-[60vh] overflow-y-auto flex flex-col gap-4">

      <div v-if="isTeacher" class="bg-gray-50 p-3 rounded-xl border border-gray-200">
        <div v-if="possibleUsers.length > 0" class="flex items-end gap-3">
          <div class="flex-1 flex flex-col gap-1">
            <label class="text-[10px] font-bold text-gray-600 uppercase tracking-wider">Inviter un abonné</label>
            <select
              :value="newParticipantId"
              @change="$emit('update:newParticipantId', $event.target.value)"
              class="w-full border border-gray-300 rounded-lg p-2 text-xs outline-none focus:ring-2 focus:ring-indigo-500 bg-white cursor-pointer"
            >
              <option value="" disabled>-- Sélectionner un utilisateur --</option>
              <option v-for="userOpt in possibleUsers" :key="userOpt.id" :value="userOpt.id">
                {{ userOpt.name }} ({{ formatRole(userOpt.role) }})
              </option>
            </select>
          </div>
          <Button
            @click="$emit('add-participant')"
            :disabled="!newParticipantId"
            label="Inviter"
            color="primary"
            variant="solid"
            size="sm"
            rounded
          />
        </div>

        <div v-else class="text-xs text-gray-500 italic text-center py-2">
          Tous les abonnés du ticket sont déjà invités à cette réunion.
        </div>
      </div>

      <div v-if="isLoadingStatuses" class="text-center flex flex-col items-center justify-center text-gray-500 py-8">
        <span class="i-lucide-loader-2 w-8 h-8 animate-spin mb-2"></span>
        Chargement...
      </div>

      <div v-else-if="eventStatuses.length > 0" class="flex flex-col gap-3">
        <div v-for="(status, index) in eventStatuses" :key="index" class="flex justify-between items-center p-4 bg-gray-50 rounded-lg border border-gray-200">
          <div class="flex-1">
            <p class="font-bold text-sm text-gray-800">
              {{ status.participantName }}
              <span class="text-xs font-normal text-gray-500 ml-1">({{ formatRole(status.participantRole) }})</span>
            </p>
            <p v-if="status.isPresent === false && status.reason" class="text-xs text-red-600 italic mt-1 bg-red-50 p-2 rounded inline-block">
              Motif : "{{ status.reason }}"
            </p>
          </div>

          <div class="flex items-center gap-3 ml-4">
            <span v-if="status.isPresent === true" class="px-3 py-1 bg-green-100 text-green-700 rounded-full text-xs font-bold whitespace-nowrap flex items-center gap-1">
              <span class="i-lucide-check w-3 h-3"></span> Accepté
            </span>
            <span v-else-if="status.isPresent === false" class="px-3 py-1 bg-red-100 text-red-700 rounded-full text-xs font-bold whitespace-nowrap flex items-center gap-1">
              <span class="i-lucide-x w-3 h-3"></span> Refusé
            </span>
            <span v-else class="px-3 py-1 bg-orange-100 text-orange-700 rounded-full text-xs font-bold whitespace-nowrap flex items-center gap-1">
              <span class="i-lucide-clock w-3 h-3"></span> En attente
            </span>

            <Button
              v-if="isTeacher && status.participantId"
              @click="$emit('remove-participant', status.participantId)"
              icon="i-lucide-trash-2"
              label="Retirer"
              color="error"
              variant="subtle"
              size="xs"
              rounded
              title="Retirer ce participant"
            />
          </div>
        </div>
      </div>

      <div v-else class="text-gray-500 italic text-center py-8">
        Aucune invitation n'a été envoyée pour ce créneau.
      </div>
    </div>
  </Modal>
</template>

<script setup>
import { computed } from 'vue'
import Button from '~/components/ui/Button.vue'
import Modal from '~/components/ui/Modal.vue'

const props = defineProps({
  open: {
    type: Boolean,
    required: true
  },
  isTeacher: {
    type: Boolean,
    default: false
  },
  possibleUsers: {
    type: Array,
    default: () => []
  },
  isLoadingStatuses: {
    type: Boolean,
    default: false
  },
  eventStatuses: {
    type: Array,
    default: () => []
  },
  newParticipantId: {
    type: String,
    default: ''
  }
})

const emit = defineEmits(['update:open', 'update:newParticipantId', 'add-participant', 'remove-participant'])

const openModel = computed({
  get: () => props.open,
  set: (val) => emit('update:open', val)
})

const formatRole = (role) => {
  if (!role) return ''
  const upper = role.toUpperCase()
  if (upper.includes('TEACHER')) return 'Professeur'
  if (upper.includes('STUDENT')) return 'étudiant'
  return role
}
</script>
