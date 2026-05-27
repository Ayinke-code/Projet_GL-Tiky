<template>
  <div class="p-5 flex flex-col gap-4">

    <div>
      <span :class="['px-2.5 py-1 text-[10px] font-bold uppercase tracking-wider rounded border', getBadgeStyle(event?.extendedProps?.type)]">
        {{ cleanTypeLabel(event?.extendedProps?.type) }}
      </span>
    </div>

    <h3 class="text-lg font-bold text-slate-800 leading-snug">
      {{ event?.title }}
    </h3>

    <div class="space-y-4 mt-1 border-t border-slate-100 pt-3">

      <div class="flex items-start gap-3 text-slate-600">
        <div class="p-2 bg-indigo-50 rounded-lg mt-0.5">
          <i class="fa-solid fa-calendar-day text-indigo-600 text-xs w-3.5 text-center"></i>
        </div>
        <div class="flex flex-col">
          <span class="text-[9px] text-slate-400 font-bold uppercase tracking-wider">Date</span>
          <span class="text-xs font-semibold text-slate-700 capitalize">{{ formatDay(event?.start) }}</span>
        </div>
      </div>

      <div class="flex items-start gap-3 text-slate-600">
        <div class="p-2 bg-indigo-50 rounded-lg mt-0.5">
          <i class="fa-solid fa-clock text-indigo-600 text-xs w-3.5 text-center"></i>
        </div>
        <div class="flex flex-col">
          <span class="text-[9px] text-slate-400 font-bold uppercase tracking-wider">Créneau</span>
          <span class="text-xs font-semibold text-slate-700">
            {{ formatTime(event?.start) }} - {{ formatTime(event?.end) }}
            <span class="text-slate-400 font-normal ml-1">({{ calculateDuration(event?.start, event?.end) }})</span>
          </span>
        </div>
      </div>

      <div v-if="event?.extendedProps?.creator" class="flex items-start gap-3 text-slate-600">
        <div class="p-2 bg-indigo-50 rounded-lg mt-0.5">
          <i class="fa-solid fa-user-tie text-indigo-600 text-xs w-3.5 text-center"></i>
        </div>
        <div class="flex flex-col">
          <span class="text-[9px] text-slate-400 font-bold uppercase tracking-wider">Organisé par</span>
          <span class="text-xs font-semibold text-slate-700">{{ event.extendedProps.creator }}</span>
        </div>
      </div>

      <div v-if="event?.extendedProps?.description" class="p-3 bg-slate-50 rounded-lg border border-slate-100 text-xs text-slate-600 leading-relaxed italic">
        {{ event.extendedProps.description }}
      </div>
    </div>

    <div v-if="event?.extendedProps?.groups?.length" class="mt-2 border-t border-slate-100 pt-3">
      <p class="text-[9px] font-bold text-slate-400 uppercase tracking-widest mb-2 flex items-center gap-1.5">
        <span class="w-1.5 h-1.5 bg-indigo-500 rounded-full"></span>
        Groupes concernés
      </p>
      <div class="flex flex-wrap gap-1.5">
        <span
          v-for="g in event.extendedProps.groups"
          :key="g.id"
          class="px-2.5 py-0.5 bg-slate-100 text-slate-600 border border-slate-200 rounded text-[11px] font-medium"
        >
          {{ g.name }}
        </span>
      </div>
    </div>

    <div class="flex justify-end mt-4 border-t border-slate-100 pt-4">
      <Button
        @click="$emit('close')"
        color="secondary"
        label="Fermer"
        variant="subtle"
        size="sm"
        rounded
      />
    </div>

  </div>
</template>

<script setup>
// Importation de TON composant Button personnalisé
import Button from '~/components/ui/Button.vue'

defineProps({
  event: Object
})

// Déclaration de l'événement pour que le parent puisse intercepter le clic de fermeture
defineEmits(['close'])

const cleanTypeLabel = (type) => {
  switch(type) {
    case 'DEFENSE': return 'Soutenance'
    case 'AUDIT': return 'Audit Académique'
    case 'MEETING':
    case 'ISSUE': return 'Rendez-vous Ticket'
    default: return 'Événement'
  }
}

const formatDay = (date) => {
  if (!date) return ''
  return new Intl.DateTimeFormat('fr-FR', { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' }).format(new Date(date))
}

const formatTime = (date) => {
  if (!date) return ''
  return new Intl.DateTimeFormat('fr-FR', { hour: '2-digit', minute: '2-digit' }).format(new Date(date))
}

const calculateDuration = (start, end) => {
  if (!start || !end) return ''
  const diffMs = new Date(end) - new Date(start)
  const diffMins = Math.round(diffMs / 60000)

  const hours = Math.floor(diffMins / 60)
  const minutes = diffMins % 60

  if (hours > 0) {
    return `${hours}h${minutes > 0 ? String(minutes).padStart(2, '0') : ''}`
  }
  return `${minutes} min`
}

const getBadgeStyle = (type) => {
  switch(type) {
    case 'DEFENSE': return 'bg-red-50 text-red-700 border-red-200'
    case 'AUDIT': return 'bg-orange-50 text-orange-700 border-orange-200'
    case 'MEETING':
    case 'ISSUE': return 'bg-sky-50 text-sky-700 border-sky-200'
    default: return 'bg-gray-50 text-gray-700 border-gray-200'
  }
}
</script>
