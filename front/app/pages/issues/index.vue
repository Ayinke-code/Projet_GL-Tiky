<template>
  <ClientOnly>
    <div class="p-8">
      <div class="flex justify-between items-center mb-8">
        <h1 class="text-3xl font-bold">Issues</h1>
        <UButton to="/issues/create" icon="i-lucide-plus" size="lg">
          Créer une issue
        </UButton>
        </div>

      <div v-if="loading" class="text-center py-8">
        <USkeleton class="h-20 w-full mb-4" />
        <USkeleton class="h-20 w-full mb-4" />
        <USkeleton class="h-20 w-full" />
      </div>

      <div v-else-if="issues.length === 0" class="text-center py-12">
        <p class="text-gray-500 text-lg">Aucune issue pour le moment</p>
        <UButton to="/issues/create" variant="outline" class="mt-4">
          Créer la première issue
        </UButton>
      </div>

      <div v-else class="grid gap-4 overflow-visible">
      <UCard
        v-for="issue in issues"
        :key="issue.idTick"
        class="hover:shadow-lg transition cursor-pointer overflow-visible"
        @click="navigateTo(`/issues/${issue.idTick}`)"
      >
        <template #header>
          <div class="flex justify-between items-start gap-4">
            <div class="flex-1">
              <h3 class="font-bold text-lg text-gray-900">{{ issue.titre }}</h3>
              <p class="text-sm text-gray-600 mt-1 line-clamp-2">{{ issue.description }}</p>
            </div>
            <div class="text-sm text-gray-500 whitespace-nowrap">
              {{ formatDate(issue.dateCreation) }}
            </div>
          </div>
        </template>

        <div class="flex gap-2 flex-wrap mb-4">
          <UBadge variant="soft" color="blue">
            {{ issue.categorie?.nom || 'Sans catégorie' }}
          </UBadge>

          <div class="relative group">
            <button
              :class="getStatusBadgeClass(issue.statut)"
              class="px-3 py-1 rounded-full font-medium text-sm transition cursor-pointer hover:shadow-md"
              :disabled="updatingStatusId === issue.idTick"
            >
              <span v-if="updatingStatusId !== issue.idTick">
                {{ getStatusLabel(issue.statut) }}
              </span>
              <span v-else class="flex items-center gap-2">
                <UIcon name="i-lucide-loader" class="animate-spin" />
              </span>
            </button>

            <!-- Dropdown menu -->
            <div class="absolute hidden group-hover:block bg-white border border-gray-300 rounded-lg shadow-2xl z-50 min-w-max left-0 top-8 max-h-56 overflow-y-auto">
              <button
                v-for="status in ['OUVERT', 'COMPLET', 'INCOMPLET', 'TERMINE']"
                :key="status"
                @click.stop="handleStatusChange(issue.idTick, status)"
                :class="[
                  'w-full text-left px-4 py-2 hover:bg-gray-100 transition text-sm font-medium whitespace-nowrap',
                  issue.statut === status ? 'bg-gray-50 border-l-4 border-indigo-500' : ''
                ]"
                :disabled="updatingStatusId === issue.idTick"
              >
                <span :class="getStatusBadgeClass(status)">{{ getStatusLabel(status) }}</span>
              </button>
            </div>
          </div>

          <UBadge v-if="issue.estPrive" color="orange" variant="soft">
            🔒 Privé
          </UBadge>
        </div>

        <template #footer>
          <div class="flex gap-2 justify-end">
            <UButton
              @click.stop="handleDelete(issue.idTick)"
              variant="ghost"
              size="sm"
              color="red"
              icon="i-lucide-trash-2"
              :loading="deletingId === issue.idTick"
            >
              Supprimer
            </UButton>
          </div>
        </template>
      </UCard>
    </div>

    <UAlert
      v-if="error"
      color="red"
      class="mt-4"
      @close="error = null"
    >
      <template #title>Erreur</template>
      {{ error }}
    </UAlert>
    </div>
  </ClientOnly>
</template>

<script setup lang="ts">
definePageMeta({
  layout: 'default'
})

const { getIssues, deleteIssue, updateIssueStatus } = useApi()
const issues = ref<any[]>([])
const loading = ref(true)
const error = ref<string | null>(null)
const deletingId = ref<number | null>(null)
const updatingStatusId = ref<number | null>(null)

const statusColor = (status: string): any => {
  const colors: Record<string, string> = {
    'OUVERT': 'yellow',
    'COMPLET': 'green',
    'INCOMPLET': 'orange',
    'TERMINE': 'blue'
  }
  return colors[status] || 'gray'
}

const getStatusLabel = (status: string): string => {
  const labels: Record<string, string> = {
    'OUVERT': 'Ouvert',
    'COMPLET': 'Complet',
    'INCOMPLET': 'Incomplet',
    'TERMINE': 'Terminé'
  }
  return labels[status] || status
}

const getStatusBadgeClass = (status: string): string => {
  const classes: Record<string, string> = {
    'OUVERT': 'bg-yellow-100 text-yellow-800 border border-yellow-300',
    'COMPLET': 'bg-green-100 text-green-800 border border-green-300',
    'INCOMPLET': 'bg-orange-100 text-orange-800 border border-orange-300',
    'TERMINE': 'bg-blue-100 text-blue-800 border border-blue-300'
  }
  return classes[status] || 'bg-gray-100 text-gray-800'
}

const formatDate = (date: string) => {
  return new Date(date).toLocaleDateString('fr-FR', {
    year: 'numeric',
    month: 'short',
    day: 'numeric'
  })
}

const handleStatusChange = async (id: number, newStatus: string) => {
  updatingStatusId.value = id
  try {
    const updatedIssue = await updateIssueStatus(id, newStatus)
    const issueIndex = issues.value.findIndex(i => i.idTick === id)
    if (issueIndex !== -1) {
      issues.value[issueIndex].statut = updatedIssue.statut
    }
  } catch (err: any) {
    error.value = err.message || 'Erreur lors de la mise à jour du statut'
  } finally {
    updatingStatusId.value = null
  }
}

const handleDelete = async (id: number) => {
  if (!confirm('Êtes-vous sûr de vouloir supprimer cette issue?')) {
    return
  }

  deletingId.value = id
  try {
    await deleteIssue(id)
    issues.value = issues.value.filter(i => i.idTick !== id)
  } catch (err: any) {
    error.value = err.message || 'Erreur lors de la suppression'
  } finally {
    deletingId.value = null
  }
}

onMounted(async () => {
  try {
    issues.value = await getIssues()
  } catch (err: any) {
    error.value = err.message || 'Erreur lors du chargement'
  } finally {
    loading.value = false
  }
})
</script>
