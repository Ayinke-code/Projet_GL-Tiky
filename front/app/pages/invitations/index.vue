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

    <div v-if="activeTab === 'sessions'" class="animate-in fade-in duration-300">
      <!-- Demandes de RDV en attente (ISSUE) -->
      <div v-if="pendingIssueInvitations.length > 0" class="mb-6">
        <h2 class="text-sm font-semibold text-amber-700 uppercase tracking-wide mb-3">Demandes de RDV en attente</h2>
        <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
          <div
            v-for="rdv in pendingIssueInvitations"
            :key="'pending-' + rdv.id"
            class="bg-white rounded-2xl shadow-sm border border-amber-200 p-5 hover:shadow-md transition-all flex flex-col gap-3"
          >
            <div class="flex items-start justify-between gap-2">
              <span class="inline-flex items-center gap-1.5 text-[10px] font-bold uppercase tracking-widest px-2.5 py-1 rounded-full bg-indigo-100 text-indigo-600">
                {{ rdv.categoryType }}
              </span>
              <span class="text-[10px] font-semibold px-2 py-0.5 rounded-full bg-amber-100 text-amber-700">
                En attente
              </span>
            </div>
            <div>
              <h3 class="text-base font-bold text-gray-900">{{ rdv.name }}</h3>
              <p v-if="rdv.description" class="text-xs text-gray-400 mt-0.5 line-clamp-1">{{ rdv.description }}</p>
            </div>
            <div v-if="rdv.startDate" class="rounded-xl bg-amber-50 px-3 py-2.5 text-xs">
              <div class="font-semibold mb-1 capitalize text-amber-900">
                <i class="fa-solid fa-calendar-days mr-1"></i>
                {{ new Date(rdv.startDate).toLocaleDateString('fr-FR', { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' }) }}
              </div>
              <div class="grid grid-cols-2 gap-2">
                <div>
                  <span class="uppercase tracking-wide text-[9px] opacity-60">Début</span>
                  <p class="font-medium">{{ new Date(rdv.startDate).toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' }) }}</p>
                </div>
                <div>
                  <span class="uppercase tracking-wide text-[9px] opacity-60">Fin</span>
                  <p class="font-medium">{{ new Date(rdv.endDate).toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' }) }}</p>
                </div>
              </div>
            </div>
            <p class="text-xs text-gray-400"><i class="fa-solid fa-user mr-1"></i>Demande de {{ rdv.createdBy }}</p>
            <div class="flex gap-2 mt-auto pt-1">
              <Button
                color="success" variant="soft" size="sm" rounded
                :loading="pendingRdvResponse === rdv.id + '-accept'"
                @click="respondToRdv(rdv, true)"
                label="Accepter"
              />
              <Button
                color="error" variant="subtle" size="sm" rounded
                :loading="pendingRdvResponse === rdv.id + '-refuse'"
                @click="openDeclineRdvModal(rdv)"
                label="Refuser"
              />
            </div>
          </div>
        </div>
      </div>

      <!-- Sessions acceptées -->
      <div v-if="myParticipations.length > 0">
        <h2 v-if="pendingIssueInvitations.length > 0" class="text-sm font-semibold text-gray-500 uppercase tracking-wide mb-3">Sessions confirmées</h2>
        <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
          <div
            v-for="session in myParticipations"
            :key="session.id"
            class="bg-white rounded-2xl shadow-sm border border-gray-100 p-5 hover:shadow-md hover:border-indigo-200 transition-all flex flex-col gap-3"
          >
            <div class="flex items-start justify-between gap-2">
              <span class="inline-flex items-center gap-1.5 text-[10px] font-bold uppercase tracking-widest px-2.5 py-1 rounded-full bg-indigo-100 text-indigo-600">
                {{ session.categoryType }}
              </span>
              <span class="text-[10px] font-semibold px-2 py-0.5 rounded-full bg-emerald-100 text-emerald-600">
                Acceptée
              </span>
            </div>
            <div>
              <h3 class="text-base font-bold text-gray-900">{{ session.name }}</h3>
              <p v-if="session.description" class="text-xs text-gray-400 mt-0.5 line-clamp-1">{{ session.description }}</p>
            </div>
            <div v-if="session.startDate" class="rounded-xl bg-indigo-50 px-3 py-2.5 text-xs">
              <div class="font-semibold mb-1 capitalize">
                <i class="fa-solid fa-calendar-days mr-1"></i>
                {{ new Date(session.startDate).toLocaleDateString('fr-FR', { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' }) }}
              </div>
              <div class="grid grid-cols-2 gap-2">
                <div>
                  <span class="uppercase tracking-wide text-[9px] opacity-60">Début</span>
                  <p class="font-medium">{{ new Date(session.startDate).toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' }) }}</p>
                </div>
                <div>
                  <span class="uppercase tracking-wide text-[9px] opacity-60">Fin</span>
                  <p class="font-medium">{{ new Date(session.endDate).toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' }) }}</p>
                </div>
              </div>
            </div>
            <p class="text-xs text-gray-400"><i class="fa-solid fa-user mr-1"></i>Organisé par {{ session.createdBy }}</p>
          </div>
        </div>
      </div>

      <div v-if="pendingIssueInvitations.length === 0 && myParticipations.length === 0" class="flex flex-col items-center justify-center p-12 bg-white rounded-xl border border-dashed border-gray-300 text-gray-500">
        <span class="i-lucide-calendar-check w-12 h-12 mb-3 text-gray-300"></span>
        <p class="text-lg font-medium text-gray-700">Aucune session</p>
        <p class="text-sm">Vous n'avez aucune session pour le moment.</p>
      </div>

      <!-- Modal de refus RDV -->
      <DeclineModal
        v-if="isDeclineRdvModalOpen"
        v-model:open="isDeclineRdvModalOpen"
        v-model:reason="declineRdvReason"
        @confirm="handleDeclineRdv"
      />
    </div>

    <div v-if="activeTab === 'pending'" class="animate-in fade-in duration-300">
      <div v-if="myPendingInvitations && pendingNonIssueInvitations.length > 0" class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        <InvitationCard
          v-for="invitation in pendingNonIssueInvitations"
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

    <AvailabilityConflictModal
      v-model:open="isAvailabilityConflictOpen"
      :conflictUsers="availabilityConflictUsers"
      @proceed="handleAvailabilityConflictProceed"
      @cancel="handleAvailabilityConflictCancel"
    />

  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, computed } from 'vue'
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
import AvailabilityConflictModal from '~/components/invitations/AvailabilityConflictModal.vue'

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
  fetchPossibleUsersInterested,
  fetchMyParticipations,
  fetchUserAvailabilities
} = useEvents()

const myParticipations = ref([])
const pendingRdvResponse = ref(null)
const isDeclineRdvModalOpen = ref(false)
const selectedRdvToDecline = ref(null)
const declineRdvReason = ref('')

const route = useRoute()
const activeTab = ref(route.query.tab || 'pending')

// --- États liés à la suppression multiple ---
const isCancelMode = ref(false)
const selectedEventIds = ref([])
const isCancelConfirmationOpen = ref(false)

// --- États liés à l'ajout de participant ---
const activeEventIdForStatus = ref(null)
const newParticipantId = ref('')
const possibleUsers = ref([])

// --- États liés au contrôle de disponibilité ---
const isAvailabilityConflictOpen = ref(false)
const availabilityConflictUsers = ref([])
const pendingInviteParticipantId = ref(null)

// --- Computed ---
const isTeacher = computed(() => {
  return ready.value && user?.value?.roles?.includes(ROLE.TEACHER)
})

const pendingIssueInvitations = computed(() =>
  myPendingInvitations.value.filter(inv => inv.categoryType === 'ISSUE')
)

// Pour l'onglet "Mes invitations" : toutes les invitations en attente (y compris ISSUE du prof reçues)
const pendingNonIssueInvitations = computed(() => myPendingInvitations.value)

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

const EVENT_NOTIFICATION_TYPES = new Set(['NEW_EVENT_INVITATION', 'UPDATE_EVENT', 'EVENT_RESPONSE', 'EVENT_CANCEL'])

const onNotificationReceived = async (e) => {
  if (EVENT_NOTIFICATION_TYPES.has(e.detail?.type)) {
    await fetchMyInvitations()
    await fetchMyCreatedEvents()
  }
}

// --- Lifecycle ---
onMounted(async () => {
  await fetchMyInvitations()
  await fetchMyCreatedEvents()
  myParticipations.value = await fetchMyParticipations()
  globalThis.addEventListener('notification:received', onNotificationReceived)
})

onUnmounted(() => {
  globalThis.removeEventListener('notification:received', onNotificationReceived)
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
  } catch (e) {
    console.error('Erreur lors de l\'annulation :', e)
  }
}

// --- Méthodes Invitations ---
const handleAccept = async (eventId) => {
  await respondToInvitation(eventId, true)
  myParticipations.value = await fetchMyParticipations()
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
    myParticipations.value = await fetchMyParticipations()
  }
}

// --- Répondre à une demande de RDV depuis l'onglet sessions ---
const respondToRdv = async (rdv, accepted) => {
  pendingRdvResponse.value = rdv.id + (accepted ? '-accept' : '-refuse')
  try {
    await respondToInvitation(rdv.id, accepted)
    myParticipations.value = await fetchMyParticipations()
  } finally {
    pendingRdvResponse.value = null
  }
}

const openDeclineRdvModal = (rdv) => {
  selectedRdvToDecline.value = rdv
  declineRdvReason.value = ''
  isDeclineRdvModalOpen.value = true
}

const handleDeclineRdv = async () => {
  if (!selectedRdvToDecline.value) return
  pendingRdvResponse.value = selectedRdvToDecline.value.id + '-refuse'
  try {
    await respondToInvitation(selectedRdvToDecline.value.id, false, declineRdvReason.value)
    isDeclineRdvModalOpen.value = false
    selectedRdvToDecline.value = null
  } finally {
    pendingRdvResponse.value = null
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
  } catch (e) {
    console.error('Erreur lors du chargement des participants :', e)
  }
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
const isAvailableForSlot = (availabilities, eventStart, eventEnd) => {
  if (!availabilities || availabilities.length === 0) return false
  const eStart = new Date(String(eventStart).replace(' ', 'T'))
  const eEnd = new Date(String(eventEnd).replace(' ', 'T'))
  return availabilities.some(avail => {
    const aStart = new Date(String(avail.startDate).replace(' ', 'T'))
    const aEnd = new Date(String(avail.endDate).replace(' ', 'T'))
    return aStart <= eStart && aEnd >= eEnd
  })
}

const doAddParticipant = async (participantId) => {
  try {
    await inviteUserToEvent(activeEventIdForStatus.value, participantId)
    newParticipantId.value = ''
    await loadEventStatuses(activeEventIdForStatus.value)
  } catch (e) {
    console.error('Erreur lors de l\'invitation :', e)
  }
}

const handleAddParticipant = async () => {
  if (!activeEventIdForStatus.value || !newParticipantId.value) return

  const activeEvent = events.value.find(e => String(e.id) === String(activeEventIdForStatus.value))

  if (activeEvent?.startDate && activeEvent?.endDate) {
    const userId = Number(newParticipantId.value)
    const availabilities = await fetchUserAvailabilities(userId)

    if (!isAvailableForSlot(availabilities, activeEvent.startDate, activeEvent.endDate)) {
      const user = possibleUsers.value.find(u => Number(u.id) === userId)
      availabilityConflictUsers.value = [{
        id: userId,
        name: user?.name || 'Utilisateur inconnu',
        availabilities
      }]
      pendingInviteParticipantId.value = newParticipantId.value
      isAvailabilityConflictOpen.value = true
      return
    }
  }

  await doAddParticipant(newParticipantId.value)
}

const handleAvailabilityConflictProceed = async () => {
  isAvailabilityConflictOpen.value = false
  if (pendingInviteParticipantId.value) {
    await doAddParticipant(pendingInviteParticipantId.value)
    pendingInviteParticipantId.value = null
  }
}

const handleAvailabilityConflictCancel = () => {
  isAvailabilityConflictOpen.value = false
  pendingInviteParticipantId.value = null
}

const handleRemoveParticipant = async (participantId) => {
  if (!activeEventIdForStatus.value || !participantId) return
  try {
    await removeParticipantFromMeeting(activeEventIdForStatus.value, participantId)
    await loadEventStatuses(activeEventIdForStatus.value) // Refresh
  } catch (e) {
    console.error('Erreur lors de la suppression du participant :', e)
  }
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
  } catch (e) {
    console.error('Erreur lors de la reprogrammation :', e)
  }
}
</script>
