<script setup>
import { ref, computed, onMounted } from 'vue'

const props = defineProps({
  issue: {
    type: Object,
    required: true
  }
})

// Récupération de askForMeeting au lieu de createMeeting
const { askForMeeting, checkUpcomingMeeting } = useEvents()
const { user } = useAuth()

const meetingName = ref(`Demande de point : ${props.issue.title}`)
const startDate = ref('')
const durationHours = ref(1)
const durationMinutes = ref(0)

const submitting = ref(false)
const error = ref(null)
const success = ref(false)

const hasUpcomingMeeting = ref(false)
const loadingMeetingCheck = ref(true)

const pad = (n) => n.toString().padStart(2, '0')

const formatDateTimeLocal = (date) => {
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`
}

const totalDurationMinutes = computed(() =>
  (durationHours.value * 60) + durationMinutes.value
)

onMounted(async () => {
  startDate.value = formatDateTimeLocal(new Date())

  try {
    // Vérifie si une réunion ou demande est déjà prévue pour bloquer le multi-spam
    hasUpcomingMeeting.value = await checkUpcomingMeeting(props.issue.id)
  } catch (err) {
    console.error('Erreur lors de l’initialisation du composant', err)
  } finally {
    loadingMeetingCheck.value = false
  }
})

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

    const formattedStartDate = `${startDate.value}:00`
    const formattedEndDate = `${formatDateTimeLocal(endObj)}:00`

    await askForMeeting({
      name: meetingName.value,
      description: `Demande de rendez-vous pour le ticket #${props.issue.id}`,
      categoryId: props.issue.categoryId,
      startDate: formattedStartDate,
      endDate: formattedEndDate
    }, props.issue.id)

    success.value = true
    hasUpcomingMeeting.value = true
    setTimeout(() => { success.value = false }, 5000)

  } catch (err) {
    const status = err?.response?.status || err?.statusCode || err?.status

    if (status === 400) {
      error.value = err?.data?.message || "Impossible de soumettre la demande : vérifiez les dates."
    } else if (status === 409) {
      error.value = err?.data?.message || "L'enseignant n'est pas disponible sur ce créneau."
    } else {
      error.value = err?.data?.message || "Une erreur inattendue est survenue lors de la demande."
    }
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="bg-white rounded-xl border border-gray-200 overflow-hidden mt-4 shadow-sm">

    <div class="px-5 py-3 border-b border-gray-100 bg-blue-50 flex items-center justify-center gap-2">
      <UIcon name="i-heroicons-hand-raised" class="size-4 text-blue-600" aria-hidden="true" />
      <h2 class="text-xs font-semibold text-blue-800 uppercase tracking-widest text-center">
        Demander un rendez-vous
      </h2>
    </div>

    <div v-if="loadingMeetingCheck" class="px-5 py-8 flex justify-center items-center gap-2 text-xs text-gray-400 italic">
      <UIcon name="i-heroicons-arrow-path" class="animate-spin size-4" />
      Vérification en cours…
    </div>

    <div v-else-if="hasUpcomingMeeting" class="px-5 py-8 flex flex-col items-center justify-center text-center gap-4 min-h-[220px]">
      <div class="p-3 bg-blue-50 text-blue-600 rounded-full flex items-center justify-center">
        <UIcon name="i-heroicons-calendar-days" class="size-6" />
      </div>
      <div class="space-y-1 max-w-[280px]">
        <h3 class="text-sm font-semibold text-gray-900">Demande ou réunion en cours</h3>
        <p class="text-xs text-gray-500 leading-relaxed">
          Une réunion est déjà prévue ou une demande est en attente de validation pour ce ticket.
        </p>
      </div>
      <div class="w-full max-w-[240px] pt-2">
        <UButton
          to="/invitations"
          icon="i-heroicons-eye"
          label="Suivre mes demandes"
          color="blue"
          variant="solid"
          block
          size="sm"
        />
      </div>
    </div>

    <div v-else class="px-5 py-6 flex flex-col items-center gap-6 text-sm w-full max-w-sm mx-auto">

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
        label="Sujet de la demande"
        name="meetingName"
        class="w-full flex flex-col items-center"
        :ui="{ label: { wrapper: 'justify-center', base: 'font-semibold' }, container: 'mt-1 w-full flex justify-center' }"
      >
        <UInput
          v-model="meetingName"
          icon="i-heroicons-chat-bubble-bottom-center-text"
          placeholder="Ex: Besoin d'aide sur l'architecture"
          size="sm"
          class="w-full max-w-[260px]"
          :ui="{ input: { base: 'text-center flex items-center' } }"
        />
      </UFormGroup>

      <UFormGroup
        label="Proposition de date"
        name="startDate"
        class="w-full flex flex-col items-center"
        :ui="{ label: { wrapper: 'justify-center', base: 'font-semibold' }, container: 'mt-1 w-full flex justify-center' }"
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
        class="w-full flex flex-col items-center"
        :ui="{ label: { wrapper: 'justify-center', base: 'font-semibold' }, container: 'mt-1 w-full flex justify-center' }"
      >
        <div class="flex items-center justify-center gap-4">
          <div class="flex items-center gap-2">
            <UInput
              v-model.number="durationHours"
              type="number"
              :min="0"
              :max="23"
              :step="1"
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
              size="sm"
              class="w-20"
              :ui="{ input: { base: 'text-center flex items-center' } }"
            />
            <span class="text-sm font-medium text-gray-600 shrink-0">min</span>
          </div>
        </div>
      </UFormGroup>

      <div class="pt-4 w-full border-t border-gray-100">
        <UButton
          :loading="submitting"
          :disabled="submitting"
          icon="i-heroicons-paper-airplane"
          label="Soumettre la demande"
          color="blue"
          size="md"
          block
          @click="handleSubmit"
        />
      </div>

    </div>
  </div>
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
