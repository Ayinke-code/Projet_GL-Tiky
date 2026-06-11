<template>
  <div class="p-5 flex flex-col gap-4">

    <div>
      <span :class="['px-2.5 py-1 text-[10px] font-bold uppercase tracking-wider rounded border', theme.badge]">
        {{ cleanTypeLabel(event?.extendedProps?.type) }}
      </span>
    </div>

    <h3 class="text-lg font-bold text-slate-800 leading-snug">
      {{ event?.title }}
    </h3>

    <div class="space-y-4 mt-1 border-t border-slate-100 pt-3">

      <!-- Date -->
      <div class="flex items-start gap-3 text-slate-600">
        <div class="p-2 rounded-lg mt-0.5" :class="theme.iconBg">
          <i class="fa-solid fa-calendar-day text-xs w-3.5 text-center" :class="theme.iconText"></i>
        </div>
        <div class="flex flex-col">
          <span class="text-[9px] text-slate-400 font-bold uppercase tracking-wider">Date</span>
          <span class="text-xs font-semibold text-slate-700 capitalize">{{ formatDay(event?.start) }}</span>
        </div>
      </div>

      <!-- Créneau -->
      <div class="flex items-start gap-3 text-slate-600">
        <div class="p-2 rounded-lg mt-0.5" :class="theme.iconBg">
          <i class="fa-solid fa-clock text-xs w-3.5 text-center" :class="theme.iconText"></i>
        </div>
        <div class="flex flex-col">
          <span class="text-[9px] text-slate-400 font-bold uppercase tracking-wider">Créneau</span>
          <span class="text-xs font-semibold text-slate-700">
            {{ formatTime(event?.start) }} - {{ formatTime(event?.end) }}
            <span class="text-slate-400 font-normal ml-1">({{ calculateDuration(event?.start, event?.end) }})</span>
          </span>
        </div>
      </div>

      <!-- Organisateur -->
      <div v-if="event?.extendedProps?.creator" class="flex items-start gap-3 text-slate-600">
        <div class="p-2 rounded-lg mt-0.5" :class="theme.iconBg">
          <i class="fa-solid fa-user-tie text-xs w-3.5 text-center" :class="theme.iconText"></i>
        </div>
        <div class="flex flex-col">
          <span class="text-[9px] text-slate-400 font-bold uppercase tracking-wider">Organisé par</span>
          <span class="text-xs font-semibold text-slate-700">{{ event.extendedProps.creator }}</span>
        </div>
      </div>

      <div v-if="event?.extendedProps?.description" class="p-3 bg-slate-50 rounded-lg border border-slate-100 text-xs text-slate-600 leading-relaxed italic">
        {{ event.extendedProps.description }}
      </div>

      <!-- Participants -->
      <div v-if="event?.extendedProps?.participants?.length" class="mt-1 border-t border-slate-100 pt-3">
        <span class="text-[9px] text-slate-400 font-bold uppercase tracking-wider mb-2 flex items-center gap-1.5">
          <i class="fa-solid fa-users text-xs" :class="theme.iconText"></i>
          Participants
        </span>
        <div class="flex flex-col gap-1.5 mt-1.5">
          <div
            v-for="p in event.extendedProps.participants"
            :key="p.name"
            class="flex items-center justify-between"
          >
            <span class="text-xs text-slate-700 font-medium">{{ p.name }}</span>
            <span
              :class="[
                'text-[10px] font-semibold px-2 py-0.5 rounded-full',
                p.hasAccepted === true  ? 'bg-emerald-100 text-emerald-700' :
                p.hasAccepted === false ? 'bg-red-100 text-red-600' :
                                          'bg-amber-100 text-amber-700'
              ]"
            >
              {{ p.hasAccepted === true ? 'Accepté' : p.hasAccepted === false ? 'Refusé' : 'En attente' }}
            </span>
          </div>
        </div>
      </div>
    </div>

    <div v-if="event?.extendedProps?.groups?.length" class="mt-2 border-t border-slate-100 pt-3">
      <p class="text-[9px] font-bold text-slate-400 uppercase tracking-widest mb-2 flex items-center gap-1.5">
        <span class="w-1.5 h-1.5 rounded-full" :class="theme.dot"></span>
        Groupe concerné
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

    <div class="flex justify-end mt-4 border-t border-slate-100 pt-4 gap-2">
      <Button
        v-if="event?.extendedProps?.type === 'ISSUE' && event?.extendedProps?.isOwner"
        @click="confirmCancel"
        color="error"
        label="Annuler ce RDV"
        variant="subtle"
        size="sm"
      />
      <Button
        v-if="event?.extendedProps?.type === 'AVAILABILITY'"
        @click="showDeleteModal = true"
        color="error"
        label="Supprimer"
        variant="subtle"
        size="sm"
        rounded
      />
      <Button
        @click="$emit('close')"
        color="secondary"
        label="Fermer"
        variant="subtle"
        size="sm"
        rounded
      />
    </div>

    <ConfirmModal
      v-model:open="showDeleteModal"
      title="Suppression"
      description="Supprimer ce créneau de disponibilité ?"
      @confirm="emit('delete', event?.id)"
    />

    <ConfirmModal
      v-model:open="showCancelModal"
      title="Annuler le rendez-vous"
      description="Annuler ce rendez-vous ? Les participants seront notifiés."
      @confirm="emit('cancel', event?.id)"
    />

  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import Button from '~/components/ui/Button.vue'
import ConfirmModal from '~/components/ui/ConfirmModal.vue'

const props = defineProps({
  event: Object
})

const emit = defineEmits(['close', 'delete', 'cancel'])

const showDeleteModal = ref(false)
const showCancelModal = ref(false)

const confirmCancel = () => {
  showCancelModal.value = true
}

const cleanTypeLabel = (type) => {
  switch(type) {
    case 'DEFENSE': return 'Soutenance'
    case 'AUDIT': return 'Audit'
    case 'MEETING': return 'Réunion'
    case 'ISSUE': return 'Rendez-vous Ticket'
    case 'AVAILABILITY': return 'Disponible'
    default: return 'Événement'
  }
}

const theme = computed(() => {
  const type = props.event?.extendedProps?.type
  switch(type) {
    case 'AUDIT': return { badge: 'bg-indigo-100 text-indigo-700 border-indigo-200', iconBg: 'bg-indigo-50', iconText: 'text-indigo-600', dot: 'bg-indigo-500' }
    case 'DEFENSE': return { badge: 'bg-emerald-100 text-emerald-700 border-emerald-200', iconBg: 'bg-emerald-50', iconText: 'text-emerald-600', dot: 'bg-emerald-500' }
    case 'MEETING': return { badge: 'bg-amber-100 text-amber-700 border-amber-200', iconBg: 'bg-amber-50', iconText: 'text-amber-600', dot: 'bg-amber-500' }
    case 'ISSUE': return { badge: 'bg-rose-100 text-rose-700 border-rose-200', iconBg: 'bg-rose-50', iconText: 'text-rose-600', dot: 'bg-rose-500' }
    case 'AVAILABILITY': return { badge: 'bg-teal-100 text-teal-700 border-teal-200', iconBg: 'bg-teal-50', iconText: 'text-teal-600', dot: 'bg-teal-500' }
    default: return { badge: 'bg-gray-100 text-gray-600 border-gray-200', iconBg: 'bg-gray-50', iconText: 'text-gray-600', dot: 'bg-gray-500' }
  }
})

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
</script>
