<template>
  <div class="space-y-3">
    <div v-if="!availabilities.length" class="rounded-xl border border-dashed border-slate-200 bg-slate-50 p-4 text-sm text-slate-500">
      Aucune disponibilité enregistrée pour le moment.
    </div>

    <ul v-else class="space-y-3">
      <li v-for="availability in availabilities" :key="availability.id" class="flex flex-col gap-3 rounded-xl border border-slate-200 bg-white p-4 shadow-sm">
        <div class="flex items-center justify-between gap-4">
          <div>
            <p class="text-sm font-semibold text-slate-900">Disponibilité</p>
            <p class="text-xs text-slate-500">{{ formatDate(availability.date) }}</p>
          </div>
          <span class="rounded-full bg-sky-100 px-3 py-1 text-xs font-semibold text-sky-700">Mes créneaux</span>
        </div>

        <div class="flex items-center justify-between gap-4 text-sm text-slate-600">
          <span>{{ formatTime(availability.startTime) }} - {{ formatTime(availability.endTime) }}</span>
          <button
            type="button"
            class="inline-flex items-center justify-center rounded-md bg-rose-500 px-3 py-1.5 text-xs font-semibold text-white transition hover:bg-rose-600"
            @click="$emit('delete', availability.id)"
          >
            Supprimer
          </button>
        </div>
      </li>
    </ul>
  </div>
</template>

<script setup>
const props = defineProps({
  availabilities: {
    type: Array,
    default: () => []
  }
})

const formatDate = (value) => {
  if (!value) return ''
  return new Intl.DateTimeFormat('fr-FR', {
    weekday: 'long',
    day: '2-digit',
    month: 'long',
    year: 'numeric'
  }).format(new Date(value))
}

const formatTime = (value) => {
  if (!value) return ''
  return value.slice(0, 5)
}
</script>
