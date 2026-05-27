<template>
  <div class="p-8 pb-8 min-h-screen bg-gray-50">

    <div class="flex justify-between items-center mb-6">
      <h1 class="text-2xl font-bold text-gray-800">Gestion des Invitations</h1>
    </div>

    <Alert v-if="error" color="error" title="Erreur système" class="mb-6">
      {{ error }}
    </Alert>

    <TabBar
      v-if="ready"
      :activeTab="activeTab"
      :isTeacher="isTeacher"
      :pendingCount="myPendingInvitations.length"
      @switch="switchTab"
    />

    <div v-if="activeTab === 'pending'" class="animate-in fade-in duration-300">
      <div v-if="myPendingInvitations && myPendingInvitations.length > 0" class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        <InvitationCard
          v-for="invitation in myPendingInvitations"
          :key="invitation.id"
          :invitation="invitation"
          @accept="handleAccept"
          @decline="openDeclineModal"
        />
      </div>

      <div v-else class="flex flex-col items-center justify-center p-12 bg-white rounded-xl border border-dashed border-gray-300 text-gray-500">
        <span class="i-lucide-inbox w-12 h-12 mb-3 text-gray-300"></span>
        <p class="text-lg font-medium text-gray-700">Aucune invitation</p>
        <p class="text-sm">Vous n'avez aucune invitation en attente pour le moment.</p>
      </div>
    </div>

    <div v-else-if="activeTab === 'tracking' || activeTab === 'demands'" class="animate-in fade-in duration-300">

      <div class="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 mb-6">
        <p class="text-sm text-gray-500">
          {{ tabDescription }}
        </p>

        <div class="flex items-center gap-2 self-end sm:self-auto">
          <Button
            v-if="isCancelMode"
            @click="exitCancelMode"
            label="Annuler la sélection"
            color="secondary"
            variant="subtle"
            size="sm"
            rounded
          />
          <Button
            @click="handleCancelAction"
            :icon="isCancelMode ? 'i-lucide-trash-2' : 'i-lucide-x-circle'"
            :label="cancelActionLabel"
            color="error"
            variant="solid"
            size="sm"
            rounded
          />
        </div>
      </div>

      <div v-if="events.length > 0" class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        <EventCard
          v-for="event in events"
          :key="event.id"
          :event="event"
          :isCancelMode="isCancelMode"
          :isTeacher="isTeacher"
          v-model:selectedEventIds="selectedEventIds"
          @view-statuses="viewEventStatuses"
          @open-reschedule="openRescheduleModal"
        />
      </div>

      <div class="flex flex-col items-center justify-center p-12 bg-white rounded-xl border border-dashed border-gray-300 text-gray-500" v-else>
        <span :class="[emptyIcon, 'w-12 h-12 mb-3 text-gray-300']"></span>
        <p class="text-lg font-medium text-gray-700">{{ emptyTitle }}</p>
        <p class="text-sm">{{ emptyDesc }}</p>
      </div>
    </div>

    <DeclineModal
      v-model:open="isDeclineModalOpen"
      v-model:reason="declineReason"
      @confirm="handleDecline"
    />

    <StatusModal
      v-model:open="isStatusModalOpen"
      :isTeacher="isTeacher"
      :possibleUsers="possibleUsers"
      :isLoadingStatuses="isLoadingStatuses"
      :eventStatuses="eventStatuses"
      v-model:newParticipantId="newParticipantId"
      @add-participant="handleAddParticipant"
      @remove-participant="handleRemoveParticipant"
    />

    <RescheduleModal
      v-model:open="isRescheduleModalOpen"
      :isTeacher="isTeacher"
      v-model:rescheduleForm="rescheduleForm"
      @confirm="handleReschedule"
    />

    <CancelConfirmationModal
      v-model:open="isCancelConfirmationOpen"
      :confirmCancelText="confirmCancelText"
      @confirm="confirmCancellation"
    />

  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useEvents } from '~/composables/useEvents'
import { useAuth } from '~/composables/useAuth'
import { ROLE } from '~/utils/roles'

import Button from '~/components/ui/Button.vue'
import Alert from '~/components/ui/Alert.vue'

import TabBar from '~/components/invitations/TabBar.vue'
import InvitationCard from '~/components/invitations/InvitationCard.vue'
import EventCard from '~/components/invitations/EventCard.vue'
import DeclineModal from '~/components/invitations/DeclineModal.vue'
import StatusModal from '~/components/invitations/StatusModal.vue'
import RescheduleModal from '~/components/invitations/RescheduleModal.vue'
import CancelConfirmationModal from '~/components/invitations/CancelConfirmationModal.vue'

// --- États & Composables ---
const { user, ready } = useAuth()

const {
  myPendingInvitations,
  events,
  error,
  fetchMyInvitations,
  respondToInvitation,
  fetchEventInvitationStatus,
  fetchMyCreatedEvents,
  updateMeetingDueToChange,
  cancelEvent,
  inviteUserToEvent,
  removeParticipantFromMeeting,
  fetchPossibleUsersInterested
} = useEvents()

const activeTab = ref('pending')

// --- États liés à la suppression multiple ---
const isCancelMode = ref(false)
const selectedEventIds = ref([])
const isCancelConfirmationOpen = ref(false)

// --- États liés à l'ajout de participant ---
const activeEventIdForStatus = ref(null)
const newParticipantId = ref('')
const possibleUsers = ref([])

// --- Computed ---
const isTeacher = computed(() => {
  return ready.value && user?.value?.roles?.includes(ROLE.TEACHER)
})

const tabDescription = computed(() => isTeacher.value
  ? "Gérez vos réunions : suivez les réponses des invités ou reprogrammez un créneau à tout moment."
  : "Suivez l'état de vos demandes de rendez-vous et relancez les enseignants si besoin."
)
const emptyIcon = computed(() => isTeacher.value ? 'i-lucide-calendar-x' : 'i-lucide-send')
const emptyTitle = computed(() => isTeacher.value ? 'Aucun événement' : 'Aucune demande')
const emptyDesc = computed(() => isTeacher.value
  ? 'Vous n\'avez organisé aucun événement pour le moment.'
  : 'Vous n\'avez fait aucune demande de rendez-vous pour le moment.'
)

const cancelActionLabel = computed(() => {
  if (!isCancelMode.value) return isTeacher.value ? "Annuler un événement" : "Annuler une demande"
  if (selectedEventIds.value.length > 1) return isTeacher.value ? "Annuler des événements" : "Annuler des demandes"
  return isTeacher.value ? "Annuler l'événement" : "Annuler la demande"
})

const confirmCancelText = computed(() => {
  if (selectedEventIds.value.length > 1) {
    return `ces ${selectedEventIds.value.length} ${isTeacher.value ? 'événements' : 'demandes'}`
  }
  return isTeacher.value ? 'cet événement' : 'cette demande'
})

// --- États des modales ---
const isDeclineModalOpen = ref(false)
const isStatusModalOpen = ref(false)
const isRescheduleModalOpen = ref(false)

const selectedInvitationEventId = ref(null)
const declineReason = ref('')

const eventStatuses = ref([])
const isLoadingStatuses = ref(false)

const selectedEventToReschedule = ref(null)
const rescheduleForm = ref({
  startDate: '',
  duration: 60,
  addAll: true
})

// --- Lifecycle ---
onMounted(async () => {
  await fetchMyInvitations()
  await fetchMyCreatedEvents()
})

const switchTab = (tab) => {
  activeTab.value = tab
  if (tab !== 'tracking' && tab !== 'demands') {
    exitCancelMode()
  }
}

// --- Méthodes d'annulation (Bulk) ---
const handleCancelAction = () => {
  if (!isCancelMode.value) {
    isCancelMode.value = true
    selectedEventIds.value = []
    return
  }
  if (selectedEventIds.value.length === 0) {
    exitCancelMode()
    return
  }
  isCancelConfirmationOpen.value = true
}

const exitCancelMode = () => {
  isCancelMode.value = false
  selectedEventIds.value = []
}

const confirmCancellation = async () => {
  try {
    for (const id of selectedEventIds.value) {
      await cancelEvent(id)
    }
    isCancelConfirmationOpen.value = false
    exitCancelMode()
  } catch (e) {}
}

// --- Méthodes Invitations ---
const handleAccept = async (eventId) => {
  await respondToInvitation(eventId, true)
}

const openDeclineModal = (invitation) => {
  selectedInvitationEventId.value = invitation.id
  declineReason.value = ''
  isDeclineModalOpen.value = true
}

const handleDecline = async () => {
  if (!selectedInvitationEventId.value) return
  const res = await respondToInvitation(selectedInvitationEventId.value, false, declineReason.value)
  if (res) {
    isDeclineModalOpen.value = false
    selectedInvitationEventId.value = null
  }
}

// --- Chargement et rafraîchissement des statuts ---
const loadEventStatuses = async (eventId) => {
  try {
    const statuses = await fetchEventInvitationStatus(eventId)
    if (statuses) {
      eventStatuses.value = statuses.filter(status => status.participantName !== user.value?.name)
    }

    if (isTeacher.value) {
      possibleUsers.value = await fetchPossibleUsersInterested(eventId)
    }
  } catch (e) {}
}

const viewEventStatuses = async (eventId) => {
  activeEventIdForStatus.value = eventId
  newParticipantId.value = ''
  possibleUsers.value = []
  isStatusModalOpen.value = true
  isLoadingStatuses.value = true
  eventStatuses.value = []

  try {
    await loadEventStatuses(eventId)
  } finally {
    isLoadingStatuses.value = false
  }
}

// --- Actions directes depuis la modale (Ajout / Suppression) ---
const handleAddParticipant = async () => {
  if (!activeEventIdForStatus.value || !newParticipantId.value) return
  try {
    await inviteUserToEvent(activeEventIdForStatus.value, newParticipantId.value)
    newParticipantId.value = ''
    await loadEventStatuses(activeEventIdForStatus.value) // Refresh
  } catch (e) {}
}

const handleRemoveParticipant = async (participantId) => {
  if (!activeEventIdForStatus.value || !participantId) return
  try {
    await removeParticipantFromMeeting(activeEventIdForStatus.value, participantId)
    await loadEventStatuses(activeEventIdForStatus.value) // Refresh
  } catch (e) {}
}

// --- Reprogrammation ---
const openRescheduleModal = (event) => {
  selectedEventToReschedule.value = event

  const formatToInput = (dateStr) => {
    if (!dateStr) return ''
    const d = new Date(dateStr)
    return d.toISOString().slice(0, 16)
  }

  let initialDuration = 60
  if (event.startDate && event.endDate) {
    const diffMs = new Date(event.endDate) - new Date(event.startDate)
    initialDuration = Math.round(diffMs / 60000)
  }

  rescheduleForm.value = {
    startDate: formatToInput(event.startDate),
    duration: initialDuration,
    addAll: true
  }
  isRescheduleModalOpen.value = true
}

const handleReschedule = async () => {
  if (!selectedEventToReschedule.value || !rescheduleForm.value.startDate) return

  const startTimestamp = new Date(rescheduleForm.value.startDate).getTime()
  const endTimestamp = startTimestamp + (rescheduleForm.value.duration * 60 * 1000)
  const endDateObj = new Date(endTimestamp)

  const year = endDateObj.getFullYear()
  const month = String(endDateObj.getMonth() + 1).padStart(2, '0')
  const day = String(endDateObj.getDate()).padStart(2, '0')
  const hours = String(endDateObj.getHours()).padStart(2, '0')
  const minutes = String(endDateObj.getMinutes()).padStart(2, '0')
  const seconds = String(endDateObj.getSeconds()).padStart(2, '0')

  const calculatedEndDate = `${year}-${month}-${day}T${hours}:${minutes}:${seconds}`
  const calculatedStartDate = `${rescheduleForm.value.startDate}:00`

  const payload = {
    startDate: calculatedStartDate,
    endDate: calculatedEndDate
  }

  try {
    const res = await updateMeetingDueToChange(
      selectedEventToReschedule.value.id,
      rescheduleForm.value.addAll,
      payload
    )

    if (res) {
      isRescheduleModalOpen.value = false
      selectedEventToReschedule.value = null
    }
  } catch (e) {}
}
</script>
