<template>
  <div :class="['relative bg-white rounded-xl shadow-sm border-l-4 p-5 flex flex-col justify-between hover:shadow-md transition-shadow', isTeacher ? 'border-indigo-500' : 'border-blue-500']">
    <input
      v-if="isCancelMode"
      type="checkbox"
      :checked="isSelected"
      @change="onCheckboxChange"
      class="absolute top-4 right-4 z-10 w-5 h-5 rounded-none border-2 border-gray-300 text-red-600 focus:ring-red-500 accent-red-600 bg-white cursor-pointer"
    />

    <div class="mb-4">
      <h3 class="text-lg font-bold text-gray-800 mb-2 line-clamp-1" :class="{ 'pr-8': isCancelMode }">
        {{ event.name }}
      </h3>

      <div v-if="event.startDate" class="space-y-1 text-xs text-gray-500 bg-gray-50 p-2.5 rounded-lg border border-gray-100">
        <p class="flex items-center gap-2 font-medium text-gray-700">
          <span class="i-lucide-calendar w-3.5 h-3.5" :class="isTeacher ? 'text-indigo-500' : 'text-blue-500'"></span>
          {{ parsedDates.day }}
        </p>
        <p class="flex items-center gap-2">
          <span class="i-lucide-clock w-3.5 h-3.5 text-gray-400"></span>
          De {{ parsedDates.start }} à {{ parsedDates.end }}
        </p>
      </div>
    </div>

    <div class="flex gap-3 border-t pt-4 mt-2">
      <Button :disabled="isCancelMode" @click="$emit('view-statuses', event.id)" icon="i-lucide-eye" :label="viewStatusLabel" color="primary" variant="solid" class="flex-1" size="sm" rounded />
      <Button :disabled="isCancelMode" @click="$emit('open-reschedule', event)" icon="i-lucide-calendar" :label="rescheduleLabel" :class="['flex-1 !text-white font-medium', isTeacher ? '!bg-indigo-500 hover:!bg-indigo-600 disabled:!bg-indigo-300' : '!bg-blue-500 hover:!bg-blue-600 disabled:!bg-blue-300']" size="sm" rounded />
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import Button from '~/components/ui/Button.vue'

const props = defineProps({
  event: {
    type: Object,
    required: true
  },
  isCancelMode: {
    type: Boolean,
    default: false
  },
  isTeacher: {
    type: Boolean,
    default: false
  },
  selectedEventIds: {
    type: Array,
    default: () => []
  }
})

const emit = defineEmits(['view-statuses', 'open-reschedule', 'update:selectedEventIds'])

const isSelected = computed(() => props.selectedEventIds.includes(props.event.id))

const onCheckboxChange = (e) => {
  const newIds = e.target.checked
    ? [...props.selectedEventIds, props.event.id]
    : props.selectedEventIds.filter(id => id !== props.event.id)
  emit('update:selectedEventIds', newIds)
}

const viewStatusLabel = computed(() => props.isTeacher ? 'Voir statuts' : 'Voir la réponse')
const rescheduleLabel = computed(() => props.isTeacher ? 'Reprogrammer' : 'Modifier date')

const parsedDates = computed(() => {
  const { startDate, endDate } = props.event
  if (!startDate) return { day: '', start: '', end: '' }
  const startDateObj = new Date(startDate)
  const rawDay = startDateObj.toLocaleDateString('fr-FR', {
    weekday: 'long', day: 'numeric', month: 'long', year: 'numeric'
  })
  const day = rawDay.charAt(0).toUpperCase() + rawDay.slice(1)
  const start = startDateObj.toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' })
  let end = ''
  if (endDate) {
    end = new Date(endDate).toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' })
  }
  return { day, start, end }
})
</script>
