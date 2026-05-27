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
      />
    </div>

    <div class="mb-8">
      <h1 class="text-3xl font-bold text-slate-900 tracking-tight">Mon Calendrier</h1>
      <p class="text-slate-500 italic">Retrouvez vos soutenances, audits et rendez-vous ticket.</p>
    </div>

    <ClientOnly>
      <div class="calendar-wrapper shadow-sm border border-slate-200 rounded-xl overflow-hidden bg-white">
        <FullCalendar
          :key="mappedEvents.length"
          :options="calendarOptions"
        />
      </div>
    </ClientOnly>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, reactive } from 'vue'
import EventCardCalendar from "~/components/events/EventCardCalendar.vue"

import FullCalendar from '@fullcalendar/vue3'
import dayGridPlugin from '@fullcalendar/daygrid'
import timeGridPlugin from '@fullcalendar/timegrid'
import interactionPlugin from '@fullcalendar/interaction'
import listPlugin from '@fullcalendar/list'
import frLocale from '@fullcalendar/core/locales/fr'
import { useEvents } from '../composables/useEvents'

const { events, fetchCalendarEvents } = useEvents()

const isDetailOpen = ref(false)
const selectedEvent = ref(null)

// Position initiale de la fenêtre flottante
const windowPos = ref({ x: 100, y: 150 })
let dragOffset = { x: 0, y: 0 }

// --- Logique du Drag & Drop ---
const startDrag = (e) => {
  dragOffset.x = e.clientX - windowPos.value.x
  dragOffset.y = e.clientY - windowPos.value.y
  window.addEventListener('mousemove', onDrag)
  window.addEventListener('mouseup', stopDrag)
}

const onDrag = (e) => {
  windowPos.value = {
    x: e.clientX - dragOffset.x,
    y: e.clientY - dragOffset.y
  }
}

const stopDrag = () => {
  window.removeEventListener('mousemove', onDrag)
  window.removeEventListener('mouseup', stopDrag)
}

// Code couleur propre et distinct sur le calendrier
const mappedEvents = computed(() => {
  return events.value.map(evt => {
    let color = '#4f46e5' // Indigo par défaut
    if (evt.categoryType === 'DEFENSE') color = '#ef4444' // Rouge soutenance
    if (evt.categoryType === 'AUDIT') color = '#f59e0b' // Orange Audit
    if (evt.categoryType === 'MEETING' || evt.categoryType === 'ISSUE') color = '#0ea5e9' // Bleu ciel pour les Tickets

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
        groups: evt.projectGroups
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
  // échelle de temps
  slotDuration: '02:00:00',

  contentHeight: 'auto',

  eventDisplay: 'block',

  displayEventTime: false,

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
  editable: false,
  selectable: true,
  dayMaxEvents: true,
})

onMounted(async () => {
  await fetchCalendarEvents()
})
</script>

<style scoped>
.calendar-wrapper {
  background: #ffffff;
  margin: 0 auto;
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

:deep(.fc-event) {
  border-radius: 6px !important;
  padding: 2px 6px !important;
  font-size: 0.8rem !important;
  border: none !important;
  cursor: pointer;
}

:deep(.fc-list-day-cushion) {
  background: #f8fafc !important;
  color: #334155 !important;
  font-weight: 600 !important;
}

:deep(.fc-timegrid-slot) {
  height: 50px !important;
}
</style>
