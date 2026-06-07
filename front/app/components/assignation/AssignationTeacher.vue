<template>
  <UCard class="bg-primary-50/30">
    <template #header>
      <div class="flex items-center justify-between">
        <h2 class="text-lg font-semibold text-primary-700">Mes affectations reçues</h2>
        <UBadge v-if="tasks.length > 0" color="primary" variant="soft" size="sm">
          {{ tasks.length }} demande(s)
        </UBadge>
      </div>
    </template>

    <div v-if="tasks.length > 0" class="space-y-3">
      <div v-for="task in displayedTasks" :key="task.id" class="p-4 bg-white border rounded shadow-sm flex items-center justify-between">

        <!-- Infos -->
        <div class="flex items-center gap-3 w-1/3">
          <div>
            <div class="font-bold text-gray-900 truncate">{{ task.issueTitle }}</div>
            <div class="text-xs text-gray-500 italic">Affecté par {{ task.creator }}</div>
          </div>
        </div>

        <!-- Bouton voir : centré -->
        <div class="flex justify-center w-1/3">
          <UButton
            label="Voir le détail"
            icon="i-heroicons-eye"
            color="neutral"
            variant="outline"
            size="sm"
            @click="openDetail(task)"
          />
        </div>

        <!-- Actions : droite -->
        <div class="flex justify-end gap-3 w-1/3">
          <UButton
            color="green"
            variant="outline"
            label="Accepter"
            icon="i-heroicons-check"
            :loading="processingId === task.id"
            @click="reply(task.id, true)"
            class="transition-all duration-200 hover:bg-green-600 hover:text-white border-2 border-green-500 text-green-600 font-bold"
          />
          <UButton
            color="red"
            variant="outline"
            label="Refuser"
            icon="i-heroicons-x-mark"
            :loading="processingId === task.id"
            @click="reply(task.id, false)"
            class="transition-all duration-200 hover:bg-red-600 hover:text-white border-2 border-red-500 text-red-600 font-bold"
          />
        </div>
      </div>

      <!-- Message si plus de 5 affectations -->
      <div v-if="tasks.length > 5" class="text-center pt-2 text-sm text-gray-500 font-medium">
        + {{ tasks.length - 5 }} autre(s) affectation(s) en attente
      </div>
    </div>

    <div v-else class="text-center py-8 text-gray-400">
      Aucune affectation en attente de réponse.
    </div>

    <!-- Modale détails des issues -->
    <IssueDetailModal
      v-model:open="isDetailOpen"
      :issue-detail="issueDetail"
      :loading="loadingDetail"
    />
  </UCard>
</template>

<script setup>
import IssueDetailModal from '~/components/issues/IssueDetailModal.vue'

const props = defineProps(['userId'])
const emit = defineEmits(['issue-updated'])

const { getTeacherAssignments, respondToRequest, getIssueDtoById } = useAssignation()

const tasks = ref([])
const processingId = ref(null)

// --- Gestion de la Modale ---
const isDetailOpen = ref(false)
const issueDetail = ref(null)
const loadingDetail = ref(false)

const openDetail = async (task) => {
  issueDetail.value = null
  loadingDetail.value = true
  isDetailOpen.value = true
  try {
    issueDetail.value = await getIssueDtoById(task.issueId)
  } catch (e) {
    console.error("Erreur lors du chargement de l'issue :", e)
  } finally {
    loadingDetail.value = false
  }
}

// --- Limitation à 5  ---
const displayedTasks = computed(() => {
  return tasks.value.slice(0, 5)
})

// --- Chargement ---
const load = async () => {
  const data = await getTeacherAssignments(props.userId)
  tasks.value = Array.isArray(data) ? data.filter(t => t.status === 'PENDING') : []
}

const reply = async (id, accept) => {
  processingId.value = id
  try {
    await respondToRequest(id, accept, props.userId)
    await load()

    emit('issue-updated')
  } catch (e) {
    console.error(e)
  } finally {
    processingId.value = null
  }
}

const onNotificationReceived = async (e) => {
  if (e.detail?.type === 'NEW_DEMANDE_MANAGER') {
    await load()
  }
}

onMounted(async () => {
  await load()
  window.addEventListener('notification:received', onNotificationReceived)
})

onUnmounted(() => {
  window.removeEventListener('notification:received', onNotificationReceived)
})
</script>
