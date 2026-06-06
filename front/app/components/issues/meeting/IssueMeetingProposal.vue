<script setup lang="js">
import { ref, computed, onMounted } from 'vue'
import AvailabilityConflictModal from '~/components/invitations/AvailabilityConflictModal.vue'

const props = defineProps({
  issue: {
    type: Object,
    required: true
  }
})

const { createMeeting, inviteUsersToEvent, fetchUserAvailabilities } = useEvents()
const { getIssueFollowers } = useIssueFollowers()
const { user } = useAuth()

const meetingName = ref(`Point sur le ticket : ${props.issue.title}`)
const startDate = ref('')
const durationHours = ref(1)
const durationMinutes = ref(0)

const followers = ref([])
const selectedFollowerIds = ref([])
const loadingFollowers = ref(true)

const submitting = ref(false)
const error = ref(null)
const success = ref(false)

// --- Contrôle de disponibilité ---
const isAvailabilityConflictOpen = ref(false)
const availabilityConflictUsers = ref([])
const skipAvailabilityCheck = ref(false)

const pad = (n) => n.toString().padStart(2, '0')

const formatDateTimeLocal = (date) => {
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`
}

const formatRole = (role) => {
  if (!role) return 'Inconnu'
  const upper = role.toUpperCase()
  return upper.includes('TEACHER') || upper.includes('ADMIN') ? 'Professeur' : 'Étudiant'
}

const totalDurationMinutes = computed(() =>
  (durationHours.value * 60) + durationMinutes.value
)

const isAllSelected = computed(() =>
  followers.value.length > 0 && selectedFollowerIds.value.length === followers.value.length
)

const toggleAllFollowers = () => {
  selectedFollowerIds.value = isAllSelected.value ? [] : followers.value.map(f => f.id)
}

onMounted(async () => {
  startDate.value = formatDateTimeLocal(new Date())

  try {
    const data = await getIssueFollowers(props.issue.id)
    const myId = user?.value?.id
    followers.value = (data ?? []).filter(f => f.id !== myId)
    selectedFollowerIds.value = followers.value.map(f => f.id)
  } catch (err) {
    console.error('Erreur lors de l\'initialisation du composant', err)
  } finally {
    loadingFollowers.value = false
  }
})

const isAvailableForSlot = (availabilities, eventStart, eventEnd) => {
  if (!availabilities || availabilities.length === 0) return true
  const eStart = new Date(String(eventStart).replace(' ', 'T'))
  const eEnd = new Date(String(eventEnd).replace(' ', 'T'))
  return availabilities.some(avail => {
    const aStart = new Date(String(avail.startDate).replace(' ', 'T'))
    const aEnd = new Date(String(avail.endDate).replace(' ', 'T'))
    return aStart <= eStart && aEnd >= eEnd
  })
}

const handleSubmit = async () => {
  error.value = null

  if (!meetingName.value.trim() || !startDate.value) {
    error.value = 'Veuillez remplir tous les champs obligatoires.'
    return
  }

  const startObj = new Date(startDate.value)

  if (startObj < new Date()) {
    error.value = "La date de début ne peut pas être dans le passé."
    return
  }

  if (totalDurationMinutes.value < 15) {
    error.value = "La durée doit être d'au moins 15 minutes."
    return
  }

  submitting.value = true
  success.value = false

  try {
    const endObj = new Date(startObj.getTime() + totalDurationMinutes.value * 60_000)

    // Vérification de disponibilité pour les abonnés sélectionnés
    if (!skipAvailabilityCheck.value && selectedFollowerIds.value.length > 0) {
      const conflictList = []
      for (const fId of selectedFollowerIds.value) {
        const avails = await fetchUserAvailabilities(fId)
        if (!isAvailableForSlot(avails, startObj.toISOString(), endObj.toISOString())) {
          const follower = followers.value.find(f => f.id === fId)
          conflictList.push({ id: fId, name: follower?.name || 'Inconnu', availabilities: avails })
        }
      }
      if (conflictList.length > 0) {
        availabilityConflictUsers.value = conflictList
        isAvailabilityConflictOpen.value = true
        submitting.value = false
        return
      }
    }

    const createdEvent = await createMeeting({
      name: meetingName.value,
      description: `Réunion de suivi pour le ticket #${props.issue.id}`,
      categoryId: props.issue.categoryId,
      startDate: startDate.value,
      endDate: formatDateTimeLocal(endObj)
    }, props.issue.id)

    if (createdEvent?.id && selectedFollowerIds.value.length > 0) {
      await inviteUsersToEvent(createdEvent.id, selectedFollowerIds.value)
    }

    success.value = true
    skipAvailabilityCheck.value = false
    setTimeout(() => { success.value = false }, 5000)

  } catch (err) {
    const status = err?.response?.status || err?.statusCode || err?.status
    console.error('[IssueMeetingProposal] handleSubmit error, status:', status, err)

    if (status === 400) {
      error.value = err?.data?.message || "Impossible de créer la réunion : vérifiez que la date n'est pas dans le passé."
    } else if (status === 403) {
      error.value = err?.data?.message || "Vous n'avez pas l'autorisation d'organiser cette réunion."
    } else if (status === 409) {
      error.value = err?.data?.message || "Conflit d'agenda détecté pour ce créneau."
    } else {
      error.value = err?.data?.message || "Une erreur inattendue est survenue lors de la création de la réunion."
    }
  } finally {
    submitting.value = false
  }
}

const proceedDespiteConflicts = async () => {
  isAvailabilityConflictOpen.value = false
  skipAvailabilityCheck.value = true
  await handleSubmit()
}

const cancelAvailabilityCheck = () => {
  isAvailabilityConflictOpen.value = false
  skipAvailabilityCheck.value = false
}

const handleSelectSlot = async ({ startDate: slotStart, endDate: slotEnd }) => {
  isAvailabilityConflictOpen.value = false

  const start = new Date(slotStart)
  const end = new Date(slotEnd)
  const diffMin = Math.round((end.getTime() - start.getTime()) / 60000)

  // Mettre à jour le formulaire avec le créneau choisi
  startDate.value = formatDateTimeLocal(start)
  durationHours.value = Math.floor(diffMin / 60)
  durationMinutes.value = diffMin % 60

  // Attendre que Vue applique les mises à jour réactives avant de soumettre
  await nextTick()

  // Soumettre directement avec ce créneau (skip la vérification de dispo)
  skipAvailabilityCheck.value = true
  await handleSubmit()
}
</script>

<template>
  <div class="bg-white rounded-xl border border-gray-200 overflow-hidden mt-4">

    <div class="px-5 py-3 border-b border-gray-100 bg-gray-50 flex items-center justify-center gap-2">
      <UIcon name="i-heroicons-calendar-days" class="size-4 text-violet-600" aria-hidden="true" />
      <h2 class="text-xs font-semibold text-gray-600 uppercase tracking-widest text-center">
        Organiser une réunion
      </h2>
    </div>

    <div class="px-5 py-6 flex flex-col items-center gap-6 text-sm w-full max-w-sm mx-auto">

      <Transition name="fade">
        <UAlert
          v-if="success"
          color="green"
          variant="soft"
          icon="i-heroicons-check-circle"
          title="Réunion créée avec succès !"
          aria-live="polite"
          class="text-center w-full"
        />
      </Transition>

      <Transition name="fade">
        <UAlert
          v-if="error"
          color="red"
          variant="soft"
          icon="i-heroicons-exclamation-circle"
          :title="error"
          aria-live="assertive"
          class="text-center w-full"
        />
      </Transition>

      <UFormGroup
        label="Titre de la réunion"
        name="meetingName"
        help="Définissez l'objet principal de cet échange."
        class="w-full flex flex-col items-center"
        :ui="{ label: { wrapper: 'justify-center', base: 'font-semibold' }, container: 'mt-1 w-full flex justify-center', help: 'text-center mt-1 text-gray-500' }"
      >
        <UInput
          v-model="meetingName"
          icon="i-heroicons-chat-bubble-bottom-center-text"
          placeholder="Ex: Point d'avancement"
          size="sm"
          class="w-full max-w-[260px]"
          :ui="{ input: { base: 'text-center flex items-center' } }"
        />
      </UFormGroup>

      <UFormGroup
        label="Date de début"
        name="startDate"
        help="Choisissez le jour et l'heure de démarrage."
        class="w-full flex flex-col items-center"
        :ui="{ label: { wrapper: 'justify-center', base: 'font-semibold' }, container: 'mt-1 w-full flex justify-center', help: 'text-center mt-1 text-gray-500' }"
      >
        <UInput
          v-model="startDate"
          type="datetime-local"
          icon="i-heroicons-calendar"
          size="sm"
          class="w-full max-w-[260px]"
          :ui="{ input: { base: 'text-center flex items-center' } }"
        />
      </UFormGroup>

      <UFormGroup
        label="Durée estimée"
        name="duration"
        help="Temps nécessaire (minimum 15 minutes)."
        class="w-full flex flex-col items-center"
        :ui="{ label: { wrapper: 'justify-center', base: 'font-semibold' }, container: 'mt-1 w-full flex justify-center', help: 'text-center mt-1 text-gray-500' }"
      >
        <div class="flex items-center justify-center gap-4">
          <div class="flex items-center gap-2">
            <UInput
              v-model.number="durationHours"
              type="number"
              :min="0"
              :max="23"
              :step="1"
              aria-label="Heures"
              size="sm"
              class="w-20"
              :ui="{ input: { base: 'text-center flex items-center' } }"
            />
            <span class="text-sm font-medium text-gray-600 shrink-0">h</span>
          </div>
          <div class="flex items-center gap-2">
            <UInput
              v-model.number="durationMinutes"
              type="number"
              :min="0"
              :max="45"
              :step="15"
              aria-label="Minutes"
              size="sm"
              class="w-20"
              :ui="{ input: { base: 'text-center flex items-center' } }"
            />
            <span class="text-sm font-medium text-gray-600 shrink-0">min</span>
          </div>
        </div>
      </UFormGroup>

      <div class="flex flex-col gap-3 pt-5 border-t border-gray-100 w-full">
        <div class="flex items-center justify-between px-1">
          <span class="text-xs font-semibold text-gray-700">Inviter des abonnés</span>
          <button
            v-if="followers.length > 0"
            type="button"
            class="text-xs font-medium text-violet-600 hover:text-violet-800 transition-colors"
            @click="toggleAllFollowers"
          >
            {{ isAllSelected ? 'Tout désélectionner' : 'Tout sélectionner' }}
          </button>
        </div>

        <div v-if="loadingFollowers" class="flex justify-center items-center gap-1.5 text-xs text-gray-400 italic py-2">
          <UIcon name="i-heroicons-arrow-path" class="animate-spin size-4" aria-hidden="true" />
          Chargement des abonnés…
        </div>

        <div
          v-else-if="followers.length === 0"
          class="text-xs text-gray-400 italic px-3 py-4 bg-gray-50 rounded-lg border border-gray-100 text-center"
        >
          Aucun autre abonné à inviter.
        </div>

        <div
          v-else
          role="group"
          aria-label="Liste des abonnés à inviter"
          class="max-h-40 overflow-y-auto rounded-lg border border-gray-200 bg-gray-50 divide-y divide-gray-100 shadow-inner w-full"
        >
          <label
            v-for="follower in followers"
            :key="follower.id"
            class="flex items-center gap-3 px-4 py-2.5 cursor-pointer hover:bg-white transition-colors"
          >
            <input
              type="checkbox"
              :value="follower.id"
              v-model="selectedFollowerIds"
              class="size-4 rounded border-gray-300 text-violet-600 accent-violet-600 focus:ring-violet-500 cursor-pointer"
            />
            <span class="text-sm text-gray-800 flex-1 truncate font-medium">{{ follower.name }}</span>
            <UBadge
              :label="formatRole(follower.role)"
              :color="formatRole(follower.role) === 'Professeur' ? 'violet' : 'gray'"
              variant="soft"
              size="xs"
            />
          </label>
        </div>
      </div>

      <div class="pt-2 w-full flex flex-col gap-4">
        <UButton
          :loading="submitting"
          :disabled="submitting"
          icon="i-heroicons-paper-airplane"
          label="Proposer le RDV"
          color="violet"
          size="md"
          block
          @click="handleSubmit"
        />
        <UButton
          to="/invitations"
          icon="i-heroicons-calendar"
          label="Voir mes rendez-vous"
          color="violet"
          class="ml-auto"
          variant="ghost"
          size="xs"
        />
      </div>

    </div>
  </div>

  <AvailabilityConflictModal
    v-model:open="isAvailabilityConflictOpen"
    :conflictUsers="availabilityConflictUsers"
    :selectable="true"
    @proceed="proceedDespiteConflicts"
    @cancel="cancelAvailabilityCheck"
    @select-slot="handleSelectSlot"
  />
</template>

<style scoped>
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease, transform 0.2s ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
  transform: translateY(-4px);
}
</style>
