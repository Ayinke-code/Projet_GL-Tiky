<template>
  <div class="p-6 bg-gray-50 min-h-screen relative overflow-x-hidden">

    <div
      v-if="isDetailOpen"
      class="fixed z-50 select-none shadow-2xl rounded-xl border border-slate-200 bg-white w-96 transition-shadow dynamic-window"
      :style="{ left: windowPos.x + 'px', top: windowPos.y + 'px' }"
    >
      <div
        @mousedown="startDrag"
        class="bg-slate-50 border-b border-slate-100 px-4 py-3 cursor-move flex justify-between items-center rounded-t-xl drag-handle"
      >
        <span class="text-xs font-bold text-slate-500 uppercase tracking-wider flex items-center gap-1.5">
          <span class="i-lucide-move w-3.5 h-3.5 text-slate-400"></span>
          Détails de l'événement
        </span>
        <button
          @click="isDetailOpen = false"
          class="text-slate-400 hover:text-slate-600 transition-colors"
        >
          <i class="fa-solid fa-xmark text-sm"></i>
        </button>
      </div>

      <EventCardCalendar
        :event="selectedEvent"
        @close="isDetailOpen = false"
        @delete="handleDeleteAvailability"
        @cancel="handleCancelMeeting"
      />
    </div>

    <div class="mb-8 flex flex-wrap items-start justify-between gap-4">
      <div>
        <h1 class="text-3xl font-bold text-slate-900 tracking-tight">Mon Calendrier</h1>
        <p class="text-slate-500 italic">Retrouvez vos soutenances, audits et rendez-vous ticket.</p>
      </div>
      <button
        @click="toggleAvailabilityMode"
        :class="[
          'flex items-center gap-2 px-4 py-2 text-sm font-semibold rounded-xl transition-all shadow-sm border',
          availabilityMode
            ? 'bg-teal-600 text-white border-teal-600 ring-2 ring-teal-200'
            : 'bg-white text-teal-700 border-teal-300 hover:bg-teal-50'
        ]"
      >
        <i :class="availabilityMode ? 'fa-solid fa-xmark' : 'fa-solid fa-calendar-check'"></i>
        {{ availabilityMode ? 'Mode disponibilité actif · Cliquez sur le calendrier' : 'Indiquer mes disponibilités' }}
      </button>
    </div>

    <!-- Mini popup contextuel (sélection sur le calendrier) -->
    <div
      v-if="selectPopup.open"
      class="fixed z-60 bg-white rounded-xl shadow-2xl border border-teal-200 w-72 flex flex-col overflow-hidden"
      :style="{ left: selectPopup.x + 'px', top: selectPopup.y + 'px' }"
    >
      <div class="flex items-center justify-between px-4 py-3 bg-teal-50 border-b border-teal-100">
        <span class="text-xs font-bold text-teal-700 uppercase tracking-wider flex items-center gap-1.5">
          <i class="fa-solid fa-calendar-check text-teal-500"></i>
          Nouvelle disponibilité
        </span>
        <button @click="closeSelectPopup" class="text-teal-400 hover:text-teal-600 transition-colors">
          <i class="fa-solid fa-xmark text-sm"></i>
        </button>
      </div>
      <div class="p-4 flex flex-col gap-3">
        <div class="bg-teal-50 rounded-lg px-3 py-2 border border-teal-100">
          <p class="text-xs font-semibold text-teal-800 capitalize">{{ formatDisplayDate(selectPopup.startDate) }}</p>
          <p class="text-sm font-bold text-teal-700 mt-0.5">
            {{ formatDisplayTime(selectPopup.startDate) }}
            <span class="text-teal-400 font-normal mx-1">→</span>
            {{ formatDisplayTime(selectPopup.endDate) }}
          </p>
        </div>
        <textarea
          v-model="selectPopup.description"
          placeholder="Note optionnelle..."
          rows="2"
          class="w-full border border-gray-200 rounded-lg p-2.5 text-sm outline-none focus:ring-2 focus:ring-teal-400 bg-white resize-none text-gray-700 placeholder-gray-400"
        />
        <p v-if="selectPopup.error" class="text-xs text-red-500">{{ selectPopup.error }}</p>
        <div class="flex gap-2">
          <button
            @click="closeSelectPopup"
            class="flex-1 px-3 py-2 text-sm font-semibold text-gray-600 bg-gray-100 hover:bg-gray-200 rounded-lg transition-colors"
          >Annuler</button>
          <button
            @click="confirmSelectPopup"
            :disabled="selectPopup.saving"
            class="flex-1 px-3 py-2 text-sm font-semibold text-white bg-teal-600 hover:bg-teal-700 disabled:opacity-60 rounded-lg transition-colors"
          >{{ selectPopup.saving ? '...' : 'Confirmer' }}</button>
        </div>
      </div>
    </div>

    <ClientOnly>
      <div :class="['calendar-wrapper shadow-sm border rounded-xl overflow-hidden bg-white transition-all', availabilityMode ? 'border-teal-400 ring-2 ring-teal-100' : 'border-slate-200']">
        <FullCalendar
          ref="fullCalendarRef"
          :options="calendarOptions"
        />
      </div>
    </ClientOnly>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, reactive } from 'vue'
import EventCardCalendar from "~/components/events/EventCardCalendar.vue"

import FullCalendar from '@fullcalendar/vue3'
import dayGridPlugin from '@fullcalendar/daygrid'
import timeGridPlugin from '@fullcalendar/timegrid'
import interactionPlugin from '@fullcalendar/interaction'
import listPlugin from '@fullcalendar/list'
import frLocale from '@fullcalendar/core/locales/fr'
import { useEvents } from '../composables/useEvents'
import Button from '~/components/ui/Button.vue'

const { events, fetchCalendarEvents, createAvailability, deleteAvailability, cancelEvent, error: availabilityError } = useEvents()
const { user } = useAuth()

const isDetailOpen = ref(false)
const selectedEvent = ref(null)
const fullCalendarRef = ref(null)

// --- Mode disponibilité ---
const availabilityMode = ref(false)
const previousView = ref('dayGridMonth')

const toggleAvailabilityMode = () => {
  const api = fullCalendarRef.value?.getApi()
  if (!availabilityMode.value) {
    // Activation : mémoriser la vue courante puis passer en semaine horaire
    previousView.value = api?.view?.type ?? 'dayGridMonth'
    availabilityMode.value = true
    api?.changeView('timeGridWeek')
    api?.scrollToTime('08:00:00')
  } else {
    // Désactivation : retour à la vue précédente
    availabilityMode.value = false
    closeSelectPopup()
    api?.changeView(previousView.value)
  }
}
const selectPopup = reactive({
  open: false,
  startDate: '',
  endDate: '',
  description: '',
  saving: false,
  error: '',
  x: 100,
  y: 100
})

const formatForInput = (date) => {
  if (!date) return ''
  const d = new Date(date)
  const pad = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}:00`
}

const formatDisplayDate = (isoStr) => {
  if (!isoStr) return ''
  return new Intl.DateTimeFormat('fr-FR', { weekday: 'long', day: 'numeric', month: 'long' }).format(new Date(isoStr))
}

const formatDisplayTime = (isoStr) => {
  if (!isoStr) return ''
  return new Intl.DateTimeFormat('fr-FR', { hour: '2-digit', minute: '2-digit' }).format(new Date(isoStr))
}

const confirmSelectPopup = async () => {
  selectPopup.error = ''
  availabilityError.value = null
  console.log('[confirmSelectPopup] payload:', {
    startDate: selectPopup.startDate,
    endDate: selectPopup.endDate,
    description: selectPopup.description || null
  })
  try {
    selectPopup.saving = true
    const result = await createAvailability({
      startDate: selectPopup.startDate,
      endDate: selectPopup.endDate,
      description: selectPopup.description || null
    })
    console.log('[confirmSelectPopup] succès:', result)
    selectPopup.open = false
    fullCalendarRef.value?.getApi().unselect()
  } catch (e) {
    console.error('[confirmSelectPopup] erreur:', e)
    selectPopup.error = availabilityError.value || e?.message || "Erreur lors de l'enregistrement."
  } finally {
    selectPopup.saving = false
  }
}

const closeSelectPopup = () => {
  selectPopup.open = false
  fullCalendarRef.value?.getApi().unselect()
}

const handleDeleteAvailability = async (eventId) => {
  try {
    await deleteAvailability(eventId)
    isDetailOpen.value = false
  } catch {}
}

const handleCancelMeeting = async (eventId) => {
  try {
    await cancelEvent(eventId)
    isDetailOpen.value = false
    await fetchCalendarEvents()
  } catch {}
}

const windowPos = ref({ x: 100, y: 150 })
let dragOffset = { x: 0, y: 0 }

const startDrag = (e) => {
  const isTouch = e.type.includes('touch')
  const clientX = isTouch ? e.touches[0].clientX : e.clientX
  const clientY = isTouch ? e.touches[0].clientY : e.clientY

  dragOffset.x = clientX - windowPos.value.x
  dragOffset.y = clientY - windowPos.value.y

  if (isTouch) {
    window.addEventListener('touchmove', onDrag, { passive: false })
    window.addEventListener('touchend', stopDrag)
  } else {
    window.addEventListener('mousemove', onDrag)
    window.addEventListener('mouseup', stopDrag)
  }
}

const onDrag = (e) => {
  const isTouch = e.type.includes('touch')
  if (isTouch) e.preventDefault()

  const clientX = isTouch ? e.touches[0].clientX : e.clientX
  const clientY = isTouch ? e.touches[0].clientY : e.clientY

  let newX = clientX - dragOffset.x
  let newY = clientY - dragOffset.y

  const maxX = window.innerWidth - 384
  const maxY = window.innerHeight - 80

  windowPos.value = {
    x: Math.max(0, Math.min(newX, maxX)),
    y: Math.max(0, Math.min(newY, maxY))
  }
}

const stopDrag = () => {
  window.removeEventListener('mousemove', onDrag)
  window.removeEventListener('mouseup', stopDrag)
  window.removeEventListener('touchmove', onDrag)
  window.removeEventListener('touchend', stopDrag)
}

const mappedEvents = computed(() => {
  const now = new Date()
  return events.value
    .filter(evt => {
      const end = evt.endDate ? new Date(evt.endDate.replace(' ', 'T')) : null
      return !end || end >= now
    })
    .map(evt => {
    let color = '#64748b' // slate-500 par défaut

    if (evt.categoryType === 'AUDIT') color = '#6366f1'   // indigo-500
    if (evt.categoryType === 'DEFENSE') color = '#10b981' // emerald-500
    if (evt.categoryType === 'MEETING') color = '#f59e0b' // amber-500
    if (evt.categoryType === 'ISSUE') color = '#f43f5e'   // rose-500
    if (evt.categoryType === 'AVAILABILITY') color = '#14b8a6' // teal-500

    return {
      id: String(evt.id),
      title: evt.name,
      start: evt.startDate?.replace(' ', 'T'),
      end: evt.endDate?.replace(' ', 'T'),
      backgroundColor: color,
      borderColor: 'transparent',
      extendedProps: {
        description: evt.description,
        type: evt.categoryType,
        creator: evt.createdBy,
        groups: evt.projectGroups,
        participants: evt.participants,
        isOwner: evt.createdBy === user.value?.name
      }
    }
  })
})

const calendarOptions = reactive({
  plugins: [dayGridPlugin, timeGridPlugin, interactionPlugin, listPlugin],
  initialView: 'dayGridMonth',
  locale: frLocale,
  events: mappedEvents,
  allDaySlot: false,
  slotMinTime: '06:00:00',
  slotMaxTime: '22:00:00',
  slotDuration: '00:30:00',
  snapDuration: '00:30:00',
  slotLabelInterval: '01:00:00',
  slotLabelFormat: { hour: '2-digit', minute: '2-digit', hour12: false },
  contentHeight: 'auto',
  eventDisplay: 'block',
  displayEventTime: true,
  headerToolbar: {
    left: 'prev,next today',
    center: 'title',
    right: 'dayGridMonth,timeGridWeek,listWeek'
  },
  buttonText: {
    today: "Aujourd'hui",
    month: "Mois",
    week: "Semaine",
    list: "Planning"
  },
  eventClick: (info) => {
    selectedEvent.value = info.event
    if (typeof window !== 'undefined') {
      windowPos.value = {
        x: (window.innerWidth / 2) - (384 / 2),
        y: (window.innerHeight / 2) - 200
      }
    }
    isDetailOpen.value = true
  },
  select: (info) => {
    if (!availabilityMode.value) return
    const clientX = info.jsEvent?.clientX ?? window.innerWidth / 2
    const clientY = info.jsEvent?.clientY ?? window.innerHeight / 2
    selectPopup.startDate = formatForInput(info.start)
    selectPopup.endDate = formatForInput(info.end)
    selectPopup.description = ''
    selectPopup.error = ''
    selectPopup.x = Math.min(clientX + 12, window.innerWidth - 300)
    selectPopup.y = Math.max(10, Math.min(clientY - 10, window.innerHeight - 290))
    selectPopup.open = true
  },
  selectAllow: () => availabilityMode.value,
  selectOverlap: true,
  selectMirror: true,
  editable: false,
  selectable: true,
  dayMaxEvents: 3,
  slotEventOverlap: false,
  eventMinHeight: 28,
  nowIndicator: true,
})

const EVENT_NOTIFICATION_TYPES = new Set(['NEW_EVENT_INVITATION', 'UPDATE_EVENT', 'EVENT_RESPONSE', 'EVENT_CANCEL'])

const onNotificationReceived = async (e) => {
  if (EVENT_NOTIFICATION_TYPES.has(e.detail?.type)) {
    await fetchCalendarEvents()
  }
}

onMounted(async () => {
  await fetchCalendarEvents()
  window.addEventListener('notification:received', onNotificationReceived)
})

onUnmounted(() => {
  window.removeEventListener('notification:received', onNotificationReceived)
})
</script>

<style scoped>
.calendar-wrapper {
  background: #ffffff;
  margin: 0 auto;
}

.calendar-wrapper.ring-2 :deep(.fc-daygrid-day),
.calendar-wrapper.ring-2 :deep(.fc-timegrid-slot) {
  cursor: crosshair;
}

:deep(.fc-highlight) {
  background: rgba(20, 184, 166, 0.18) !important;
}

/* On s'assure que la fenêtre dynamique possède une ombre marquée pour ressortir du blanc */
.dynamic-window {
  box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.1), 0 10px 10px -5px rgba(0, 0, 0, 0.04);
}

:deep(.fc) {
  --fc-border-color: #e2e8f0;
  --fc-daygrid-event-dot-width: 8px;
  --fc-today-bg-color: #eef2ff;
  background: #ffffff;
  color: #334155;
  font-family: inherit;
  padding: 1.5rem;
}

:deep(.fc-header-toolbar) {
  margin-bottom: 2rem !important;
}

:deep(.fc-toolbar-title) {
  font-size: 1.25rem !important;
  font-weight: 700;
  color: #0f172a;
  text-transform: capitalize;
}

:deep(.fc-button) {
  background-color: #ffffff !important;
  color: #475569 !important;
  border: 1px solid #cbd5e1 !important;
  font-weight: 600 !important;
  text-transform: capitalize !important;
  transition: all 0.2s;
  box-shadow: 0 1px 2px 0 rgba(0, 0, 0, 0.05);
}

:deep(.fc-button-active) {
  background-color: #4f46e5 !important;
  color: #ffffff !important;
  border-color: #4f46e5 !important;
}

:deep(.fc-button:hover:not(.fc-button-active)) {
  background-color: #f8fafc !important;
  color: #0f172a !important;
}

:deep(.fc-col-header-cell) {
  padding: 12px 0 !important;
  background: #f8fafc;
  font-size: 0.75rem;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  color: #64748b;
  border-bottom: 1px solid #e2e8f0 !important;
}

:deep(.fc-daygrid-day-number) {
  color: #475569 !important;
  padding: 8px !important;
  font-size: 0.85rem;
  font-weight: 500;
}

:deep(.fc-day-today .fc-daygrid-day-number) {
  color: #4f46e5 !important;
  font-weight: 800;
}

/* Événements communs */
:deep(.fc-event) {
  border-radius: 6px !important;
  border: none !important;
  cursor: pointer;
  transition: filter 0.15s, transform 0.1s;
}
:deep(.fc-event:hover) {
  filter: brightness(0.92);
  transform: translateY(-1px);
}

/* Événements vue mois (daygrid) */
:deep(.fc-daygrid-event) {
  padding: 2px 7px !important;
  font-size: 0.78rem !important;
  font-weight: 500 !important;
  margin-bottom: 2px !important;
}
:deep(.fc-daygrid-event .fc-event-title) {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* Événements vue semaine (timegrid) */
:deep(.fc-timegrid-event) {
  border-radius: 6px !important;
  border-left: 3px solid rgba(0,0,0,0.15) !important;
  padding: 3px 6px !important;
  box-shadow: 0 1px 3px rgba(0,0,0,0.12);
}
:deep(.fc-timegrid-event .fc-event-title) {
  font-size: 0.78rem !important;
  font-weight: 600 !important;
  line-height: 1.3;
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}
:deep(.fc-timegrid-event .fc-event-time) {
  font-size: 0.7rem !important;
  font-weight: 500 !important;
  opacity: 0.85;
  margin-bottom: 1px;
}

/* Indicateur de l'heure courante */
:deep(.fc-timegrid-now-indicator-line) {
  border-color: #ef4444 !important;
  border-width: 2px !important;
}
:deep(.fc-timegrid-now-indicator-arrow) {
  border-color: #ef4444 !important;
}

/* Vue planning (list) */
:deep(.fc-list-day-cushion) {
  background: #f8fafc !important;
  color: #334155 !important;
  font-weight: 600 !important;
}
:deep(.fc-list-event:hover td) {
  background: #f1f5f9 !important;
}
:deep(.fc-list-event-dot) {
  border-width: 7px !important;
}

/* Grille horaire */
:deep(.fc-timegrid-slot) {
  height: 30px !important;
}
:deep(.fc-timegrid-slot-minor) {
  border-top: 1px dashed #e2e8f0 !important;
}

/* Indicateur « + N événements » */
:deep(.fc-daygrid-more-link) {
  font-size: 0.72rem !important;
  font-weight: 600 !important;
  color: #4338ca !important; /* indigo-700 — ratio ~6.7:1 sur #eef2ff, WCAG AA ✓ */
  padding: 1px 5px;
    background-color: #ffffff;
  border-radius: 4px;
}
</style>
