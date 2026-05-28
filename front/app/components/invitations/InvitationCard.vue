<template>
  <div class="bg-white rounded-xl shadow-sm p-6 border border-gray-100 flex flex-col justify-between hover:shadow-md transition-shadow">
    <div>
      <div class="flex justify-between items-start mb-2">
        <h3 class="text-lg font-bold text-gray-800 line-clamp-1">{{ invitation.name }}</h3>
        <span class="px-2 py-1 bg-indigo-50 text-indigo-700 text-xs font-bold rounded">
          {{ invitation.categoryName }}
        </span>
      </div>
      <p class="text-sm text-gray-600 mb-4 line-clamp-2">{{ invitation.description }}</p>

      <div v-if="invitation.startDate" class="text-xs space-y-2 p-3 bg-indigo-50/40 border border-indigo-100/50 rounded-lg mb-6">
        <p class="flex items-center gap-2 text-indigo-900 font-semibold">
          <span class="i-lucide-calendar w-4 h-4 text-indigo-600"></span>
          {{ parsedDates.day }}
        </p>
        <p class="flex items-center gap-2 text-gray-600">
          <span class="i-lucide-clock w-4 h-4 text-gray-400"></span>
          <strong>Horaire :</strong> {{ parsedDates.start }} - {{ parsedDates.end }}
        </p>
        <p class="flex items-center gap-2 text-gray-500 pt-1.5 border-t border-indigo-100/40 mt-1.5">
          <span class="i-lucide-user w-4 h-4 text-gray-400"></span>
          <strong>Organisateur :</strong> {{ invitation.createdBy }}
        </p>
      </div>
    </div>

    <div class="flex justify-end gap-3 border-t pt-4">
      <Button @click="$emit('decline', invitation)" color="error" label="Refuser" variant="subtle" size="sm" rounded />
      <Button @click="$emit('accept', invitation.id)" color="success" label="Accepter" variant="solid" size="sm" rounded />
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import Button from '~/components/ui/Button.vue'

const props = defineProps({
  invitation: {
    type: Object,
    required: true
  }
})

defineEmits(['accept', 'decline'])

const parsedDates = computed(() => {
  const { startDate, endDate } = props.invitation
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
