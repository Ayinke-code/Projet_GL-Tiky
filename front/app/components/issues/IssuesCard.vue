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
            {{ issue.categoryName || 'Sans catégorie' }}
          </UBadge>

          <span
            :id="'issue-card-statut-'+issue.title"
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
      <div v-if="canDelete" class="flex justify-end">
        <UButton
          @click.stop="emit('delete', issue)"
          variant="ghost"
          size="xs"
          color="error"
          icon="i-lucide-trash-2"
          class="cursor-pointer"
        />
      </div>

      <!-- Si l'utilisateur connecté est le créateur de l'issue et si l'issue est TERMINE -->
      <div v-if="(issue?.createdId === user?.id) && (issue?.status === 'TERMINE')" class="flex justify-end mt-2">
        <Button
          @click.stop="emit('requestAReopening', issue)"
          label="Demander la réouverture"
          variant="outline"
          size="xs"
          color="info"
          class="border border-blue-400 text-blue-600 hover:bg-blue-50"
        />
      </div>

      <!-- Si l'utilisateur connecté est l'issue manager et que l'issue est en demande de réouverture -->
      <div v-else-if="(user?.id === issue?.categoryManagerId) && (issue?.status === 'DEMANDE_REOUVERTURE')" class="flex justify-end gap-2 mt-2">
        <Button
          @click.stop="emit('acceptReopening', issue)"
          label="Accepter"
          variant="outline"
          size="xs"
          color="success"
          class="border border-green-400 text-green-600 hover:bg-green-50"
        />
        <Button
          @click.stop="emit('refuseReopening', issue)"
          label="Refuser"
          variant="outline"
          size="xs"
          color="error"
          class="border border-red-400 text-red-600 hover:bg-red-50"
        />
      </div>
  </UCard>
</template>

<script setup>
import Button from '../ui/Button.vue'

const props = defineProps({
  issue: Object,
  user: Object,
  updatingStatusId: Number
})

const canDelete = computed(() => {
  const currentUserId = props.user?.id
  const issue = props.issue
  if (!currentUserId || !issue) return false

  return currentUserId === issue.createdId
    || currentUserId === issue.categoryOwnerId
    || currentUserId === issue.categoryManagerId
})

const { isFollowedIssueUpdated } = useFollowedIssuesState()
const isUpdated = computed(() => isFollowedIssueUpdated(props.issue?.id))

/**
 * Labels statuts
 */
const getStatusLabel = (status) => {
    const labels = {
        'OUVERT': 'Ouvert',
        'EN_ATTENTE': 'En attente',
        'INCOMPLET': 'Incomplet',
        'TERMINE': 'Terminé',
        'DEMANDE_REOUVERTURE': 'Demande réouverture'
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
      'TERMINE': 'bg-green-100 text-green-800 border border-green-300',
      'DEMANDE_REOUVERTURE': 'bg-blue-100 text-blue-800 border border-blue-300'
    }
    return classes[status] || 'bg-gray-100 text-gray-800'
}

const emit = defineEmits(['delete', 'requestAReopening', 'acceptReopening', 'refuseReopening'])
</script>
