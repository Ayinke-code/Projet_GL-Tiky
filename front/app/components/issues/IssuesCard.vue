<template>
  <UCard
    :class="[
      'hover:shadow-md transition cursor-pointer',
      isUpdated ? 'bg-green-50 border-green-200 shadow-sm' : ''
    ]"
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
            {{ issue.categoryName || $t('no_category') }}
          </UBadge>

          <span
            :class="getStatusBadgeClass(issue.status)"
            class="px-2 py-0.5 rounded-full text-xs font-medium"
          >
            {{ getStatusLabel(issue.status) }}
          </span>

          <UBadge v-if="issue.private" color="warning" variant="soft" size="sm">
            <UIcon name="i-heroicons-lock-closed" />
          </UBadge>
        </div>
      </div>

      <!-- DATE -->
      <div
        :class="[
          'text-[11px] whitespace-nowrap',
          isUpdated ? 'text-green-600 font-bold' : 'text-gray-500'
        ]"
      >
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

const { isFollowedIssueUpdated } = useFollowedIssuesState()
const isUpdated = computed(() => isFollowedIssueUpdated(props.issue?.id))

/**
 * Labels statuts
 */
const { t } = useI18n()
const getStatusLabel = (status) => {
  const labels = {
    OUVERT: t('status.open'),
    EN_ATTENTE: t('status.pending'),
    INCOMPLET: t('status.incomplete'),
    TERMINE: t('status.finished')
  }
  return labels[status] || status
}

/**
 * UI status colors
 */
const getStatusBadgeClass = (status) => {
  const classes = {
    OUVERT: 'bg-yellow-100 text-yellow-800 border border-yellow-300',
    EN_ATTENTE: 'bg-gray-100 text-gray-800 border border-gray-300',
    INCOMPLET: 'bg-orange-100 text-orange-800 border border-orange-300',
    TERMINE: 'bg-green-100 text-green-800 border border-green-300'
  }
  return classes[status] || 'bg-gray-100 text-gray-800'
}

const emit = defineEmits(['delete'])
</script>
