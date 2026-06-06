<template>
  <Modal v-model:open="openModel" title="Disponibilité insuffisante">
    <div class="p-4 flex flex-col gap-5 max-h-[70vh] overflow-y-auto">

      <!-- Warning banner -->
      <div class="bg-amber-50 border border-amber-200 rounded-xl p-3 flex items-start gap-2.5">
        <i class="fa-solid fa-triangle-exclamation text-amber-500 mt-0.5 shrink-0"></i>
        <div>
          <p class="text-sm font-semibold text-amber-800">
            {{ conflictUsers.length > 1
              ? 'Certains utilisateurs ne sont pas disponibles sur ce créneau'
              : `${conflictUsers[0]?.name} n'est pas disponible sur ce créneau` }}
          </p>
          <ul v-if="conflictUsers.length > 1" class="mt-1.5 space-y-0.5">
            <li v-for="u in conflictUsers" :key="u.id" class="text-xs text-amber-700 font-medium">
              · {{ u.name }}
            </li>
          </ul>
        </div>
      </div>

      <!-- Créneaux communs (meilleure correspondance) -->
      <div v-if="commonSlots.length > 0" class="flex flex-col gap-2">
        <p class="text-xs font-bold text-emerald-700 uppercase tracking-wider flex items-center gap-1.5">
          <i class="fa-solid fa-star text-emerald-500"></i>
          Meilleure correspondance — créneaux communs
        </p>
        <div class="flex flex-col gap-1.5">
          <div
            v-for="(slot, idx) in commonSlots"
            :key="'common-' + idx"
            class="flex items-center gap-2.5 bg-emerald-50 border border-emerald-200 rounded-lg px-3 py-2.5"
          >
            <i class="fa-solid fa-check-circle text-emerald-500 text-xs shrink-0"></i>
            <div class="flex-1 min-w-0">
              <span class="text-xs font-semibold text-emerald-800 capitalize block">
                {{ formatAvailDate(slot.start) }}
              </span>
              <span class="text-xs text-emerald-600">
                {{ formatAvailTime(slot.start) }} → {{ formatAvailTime(slot.end) }}
                <span class="text-emerald-400 ml-1">({{ formatDuration(slot.start, slot.end) }})</span>
              </span>
            </div>
            <button
              v-if="selectable"
              @click="selectSlot(slot)"
              class="ml-2 shrink-0 px-3 py-1.5 text-xs font-bold text-white bg-emerald-600 hover:bg-emerald-700 rounded-lg transition-colors whitespace-nowrap"
            >
              Choisir
            </button>
          </div>
        </div>
      </div>

      <!-- Available slots per user -->
      <div v-for="u in conflictUsers" :key="'slots-' + u.id" class="flex flex-col gap-2">
        <p class="text-xs font-bold text-gray-600 uppercase tracking-wider flex items-center gap-1.5">
          <i class="fa-solid fa-calendar-days text-teal-500"></i>
          Créneaux déclarés{{ conflictUsers.length > 1 ? ` — ${u.name}` : '' }}
        </p>

        <div
          v-if="!u.availabilities || u.availabilities.length === 0"
          class="text-xs text-gray-400 italic bg-gray-50 rounded-lg p-3 text-center border border-gray-100"
        >
          Aucune disponibilité déclarée pour le moment.
        </div>

        <div v-else class="flex flex-col gap-1.5">
          <div
            v-for="avail in u.availabilities"
            :key="avail.id"
            class="flex items-center gap-2.5 bg-teal-50 border border-teal-100 rounded-lg px-3 py-2"
          >
            <i class="fa-solid fa-clock text-teal-500 text-xs shrink-0"></i>
            <span class="text-xs font-semibold text-teal-800 capitalize">
              {{ formatAvailDate(avail.startDate) }}
            </span>
            <span class="text-xs text-teal-600 ml-auto whitespace-nowrap">
              {{ formatAvailTime(avail.startDate) }} → {{ formatAvailTime(avail.endDate) }}
            </span>
          </div>
        </div>
      </div>

      <!-- Actions -->
      <div class="flex gap-2 pt-2 border-t border-gray-100">
        <button
          @click="$emit('cancel')"
          class="flex-1 px-4 py-2 text-sm font-semibold text-gray-600 bg-gray-100 hover:bg-gray-200 rounded-lg transition-colors"
        >
          Annuler
        </button>
        <button
          @click="$emit('proceed')"
          class="flex-1 px-4 py-2 text-sm font-semibold text-white bg-indigo-600 hover:bg-indigo-700 rounded-lg transition-colors"
        >
          Inviter quand même
        </button>
      </div>
    </div>
  </Modal>
</template>

<script setup>
import Modal from '~/components/ui/Modal.vue'
import { computed } from 'vue'

const props = defineProps({
  open: { type: Boolean, required: true },
  conflictUsers: { type: Array, default: () => [] },
  /** Si true, un bouton "Choisir" apparaît sur les créneaux communs */
  selectable: { type: Boolean, default: false }
})

const emit = defineEmits(['update:open', 'cancel', 'proceed', 'select-slot'])

const openModel = computed({
  get: () => props.open,
  set: (val) => emit('update:open', val)
})

/**
 * Calcule l'intersection des disponibilités de tous les utilisateurs en conflit.
 * Retourne un tableau de { start: Date, end: Date }.
 */
const commonSlots = computed(() => {
  if (props.conflictUsers.length === 0) return []

  const toDate = (str) => new Date(String(str).replace(' ', 'T'))

  // Intervalles initiaux = créneaux du premier utilisateur
  let intervals = (props.conflictUsers[0].availabilities ?? []).map(a => ({
    start: toDate(a.startDate),
    end: toDate(a.endDate)
  }))

  // Intersection avec chaque utilisateur suivant
  for (let i = 1; i < props.conflictUsers.length; i++) {
    const userSlots = (props.conflictUsers[i].availabilities ?? []).map(a => ({
      start: toDate(a.startDate),
      end: toDate(a.endDate)
    }))

    const next = []
    for (const iv of intervals) {
      for (const slot of userSlots) {
        const overlapStart = new Date(Math.max(iv.start.getTime(), slot.start.getTime()))
        const overlapEnd = new Date(Math.min(iv.end.getTime(), slot.end.getTime()))
        if (overlapStart < overlapEnd) {
          next.push({ start: overlapStart, end: overlapEnd })
        }
      }
    }
    intervals = next
  }

  return intervals
})

const selectSlot = (slot) => {
  emit('select-slot', {
    startDate: slot.start.toISOString(),
    endDate: slot.end.toISOString()
  })
}

const formatAvailDate = (dateOrStr) => {
  if (!dateOrStr) return ''
  const d = dateOrStr instanceof Date ? dateOrStr : new Date(String(dateOrStr).replace(' ', 'T'))
  return new Intl.DateTimeFormat('fr-FR', { weekday: 'long', day: 'numeric', month: 'long' }).format(d)
}

const formatAvailTime = (dateOrStr) => {
  if (!dateOrStr) return ''
  const d = dateOrStr instanceof Date ? dateOrStr : new Date(String(dateOrStr).replace(' ', 'T'))
  return new Intl.DateTimeFormat('fr-FR', { hour: '2-digit', minute: '2-digit' }).format(d)
}

const formatDuration = (start, end) => {
  const diffMin = Math.round((end.getTime() - start.getTime()) / 60000)
  const h = Math.floor(diffMin / 60)
  const m = diffMin % 60
  if (h === 0) return `${m}min`
  if (m === 0) return `${h}h`
  return `${h}h${m.toString().padStart(2, '0')}`
}
</script>

