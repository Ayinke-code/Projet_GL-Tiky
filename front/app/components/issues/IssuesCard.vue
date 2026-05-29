<template>
  <UCard
    class="hover:shadow-md transition cursor-pointer"
    @click="navigateTo(`/issues/${issue.id}`)"
  >
    <!-- HEADER -->
    <div class="flex justify-between gap-4">
      <div class="flex flex-col gap-2 min-w-0">
        <h3 class="font-semibold text-sm truncate">
          {{ issue.title }}
        </h3>

        <div class="flex items-center gap-2 flex-wrap">
          <UBadge variant="soft" color="info" size="xs">
            {{ issue.categoryName || 'Sans catégorie' }}
          </UBadge>

          <span
            :class="getStatusBadgeClass(issue.status)"
            class="px-2 py-0.5 rounded-full text-xs font-medium"
          >
            {{ getStatusLabel(issue.status) }}
          </span>

          <UBadge v-if="issue.private" color="warning" variant="soft" size="xs">
            🔒
          </UBadge>
        </div>
      </div>

      <!-- DATE -->
      <div class="text-[11px] text-gray-500 whitespace-nowrap">
        {{ formatDateTime(issue.createdAt) }}
      </div>
    </div>

    <!-- FOOTER -->
      <div v-if="showDelete" class="flex justify-end">
        <UButton
          @click.stop="emit('delete', issue)"
          variant="ghost"
          size="xs"
          color="error"
          icon="i-lucide-trash-2"
          class="cursor-pointer"
        />
      </div>
  </UCard>
</template>

<script setup>
const props = defineProps({
  issue: Object,
  updatingStatusId: Number,
  showDelete: { type: Boolean, default: false }
})

/**
 * Labels statuts
 */
const getStatusLabel = (status) => {
    const labels = {
        'OUVERT': 'Ouvert',
        'EN_ATTENTE': 'En attente',
        'INCOMPLET': 'Incomplet',
        'TERMINE': 'Terminé'
    }
    return labels[status] || status
}

/**
 * UI status colors
 */
const getStatusBadgeClass = (status) => {
    const classes = {
      'OUVERT':'bg-yellow-100 text-yellow-800 border border-yellow-300',
      'EN_ATTENTE': 'bg-gray-100 text-gray-800 border border-gray-300',
      'INCOMPLET': 'bg-orange-100 text-orange-800 border border-orange-300',
      'TERMINE': 'bg-green-100 text-green-800 border border-green-300'
    }
    return classes[status] || 'bg-gray-100 text-gray-800'
}

const emit = defineEmits(['delete'])
</script>
