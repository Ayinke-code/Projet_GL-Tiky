<template>
    <div class="p-8">
        <div class="flex justify-between items-center mb-8">
            <h1 class="text-3xl font-bold text-black">Issues</h1>
            <UButton to="/issues/create" icon="i-lucide-plus" size="lg">
                Créer une issue
            </UButton>
        </div>

      <!--Rebrique des affectations -->
      <div v-if="isTeacher" class="py-12">
        <section><AssignationManager :userId="user.id" /></section>

        <div class="relative py-8">
          <div class="absolute inset-0 flex items-center"><span class="w-full border-t border-gray-200"></span></div>
          <div class="relative flex justify-center"><span class="bg-white px-2 text-sm text-gray-400"></span></div>
        </div>

        <section><AssignationTeacher :userId="user.id" /></section>
      </div>

      <div
        v-if="user"
        v-show="afficherSectionIncomplete"
        class="bg-white p-6 mb-8 rounded-2xl shadow-sm border border-gray-100"
      >
        <IncompleteIssuesList
          :userId="user.id"
          @has-issues="afficherSectionIncomplete = $event"
        />
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
          <UCard v-for="issue in issues" :key="issue.id"
              class="hover:shadow-lg transition cursor-pointer overflow-visible"
              @click="navigateTo(`/issues/${issue.id}`)">
              <template #header>
                  <div class="flex justify-between items-start gap-4">
                      <div class="flex-1">
                          <h3 class="font-bold text-lg">{{ issue.title }}</h3>
                          <p class="text-sm text-gray-600 mt-1 line-clamp-2">{{ issue.description }}</p>
                      </div>
                      <div class="text-sm text-gray-500 whitespace-nowrap">
                          {{ formatDateTime(issue.createdAt) }}
                      </div>
                  </div>
              </template>

              <div class="flex gap-2 flex-wrap mb-4">
                  <UBadge variant="soft" color="info">
                      {{ issue.categoryName || 'Sans catégorie' }}
                  </UBadge>

                  <div class="relative group">
                      <button :class="getStatusBadgeClass(issue.status)"
                          class="px-3 py-1 rounded-full font-medium text-sm transition cursor-pointer hover:shadow-md"
                          :disabled="updatingStatusId === issue.id">
                          <span v-if="updatingStatusId !== issue.id">
                              {{ getStatusLabel(issue.status) }}
                          </span>
                          <span v-else class="flex items-center gap-2">
                              <UIcon name="i-lucide-loader" class="animate-spin" />
                          </span>
                      </button>

                      <!-- Dropdown menu : TODO: A corriger si besoin -->
                        <!-- <div
                            class="absolute hidden group-hover:block bg-white border border-gray-300 rounded-lg shadow-2xl z-50 min-w-max left-0 top-8 max-h-56 overflow-y-auto">
                            <button v-for="status in ['OUVERT', 'COMPLET', 'INCOMPLET', 'TERMINE']" :key="status"
                                @click.stop="handleStatusChange(issue.id, status)" :class="[
                                    'w-full text-left px-4 py-2 hover:bg-gray-100 transition text-sm font-medium whitespace-nowrap',
                                    issue.status === status ? 'bg-gray-50 border-l-4 border-indigo-500' : ''
                                ]" :disabled="updatingStatusId === issue.id">
                                <span :class="getStatusBadgeClass(status)">{{ getStatusLabel(status) }}</span>
                            </button>
                        </div> -->
                    </div>

                    <UBadge v-if="issue.private" color="warning" variant="soft">
                        🔒 Privé
                    </UBadge>
                </div>

                <template #footer>
                    <div class="flex gap-2 justify-end">
                        <UButton @click.stop="deleteValidation(issue)" variant="ghost" size="sm" color="error"
                            icon="i-lucide-trash-2" class="cursor-pointer">
                            Supprimer
                        </UButton>
                    </div>
                </template>
            </UCard>
        </div>

        <UAlert v-if="error" color="error" class="mt-4" @close="error = null">
            <template #title>Erreur</template>
            {{ error }}
        </UAlert>
    </div>

    <!-- Modal de suppression d'issue -->
    <ConfirmModal v-model:open="isDeleteValidateModalOpen" title="Suppression"
        :description="`Voulez-vous vraiment supprimer l'issue : ${deletingIssue?.title}`" @confirm="handleDelete" />
</template>

<script setup lang="js">
/**
 * Page liste des issues
 * - affichage liste
 * - suppression avec modal de confirmation
 */

import { formatDateTime } from '#imports'
import ConfirmModal from '~/components/ui/ConfirmModal.vue'
import AssignationManager from '~/components/layout/AssignationManager.vue'
import AssignationTeacher from '~/components/layout/AssignationTeacher.vue'
import IncompleteIssuesList from '~/components/layout/IncompleteIssuesList.vue'

const { ready, user } = useAuth()
const { issues, fetchIssues, deleteIssue, error } = useIssues()

/**
 * State UI
 */
const loading = ref(true)
const updatingStatusId = ref(null)

/**
 * Issue en cours de suppression (null = aucune)
 */
const deletingIssue = ref(null)

/**
 * ouverture modal suppression
 */
const isDeleteValidateModalOpen = ref(false)

/**
 * Labels statuts
 */
const getStatusLabel = (status) => {
    const labels = {
        'OUVERT': 'Ouvert',
        'COMPLET': 'Complet',
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
        'OUVERT': 'bg-yellow-100 text-yellow-800 border border-yellow-300',
        'COMPLET': 'bg-green-100 text-green-800 border border-green-300',
        'INCOMPLET': 'bg-orange-100 text-orange-800 border border-orange-300',
        'TERMINE': 'bg-blue-100 text-blue-800 border border-blue-300'
    }
    return classes[status] || 'bg-gray-100 text-gray-800'
}

// TODO: Refaire le popover pour changé de status
// const handleStatusChange = async (id, newStatus) => {
//   updatingStatusId.value = id
//   try {
//     const updatedIssue = await updateIssueStatus(id, newStatus)
//     const issueIndex = issues.value.findIndex(i => i.id === id)
//     if (issueIndex !== -1) {
//       issues.value[issueIndex].statut = updatedIssue.statut
//     }
//   } catch (err) {
//     error.value = err.message || 'Erreur lors de la mise à jour du statut'
//   } finally {
//     updatingStatusId.value = null
//   }
// }

/**
 * Ouvre modal suppression
 */
const deleteValidation = (issue) => {
    deletingIssue.value = {
        id: issue?.id,
        title: issue?.title
    }
    isDeleteValidateModalOpen.value = true
}

/**
 * Confirme suppression issue
 */
const handleDelete = async () => {
    if (!deletingIssue.value) return

    const id = deletingIssue.value.id;

    try {
        await deleteIssue(id)

        // update local state
        issues.value = issues.value.filter(i => i.id !== id)
    } catch (err) {
        error.value = err.message || 'Erreur lors de la suppression'
    } finally {
        deletingIssue.value = null
        isDeleteValidateModalOpen.value = false
    }
}

/**
 * Chargement initial
 */
onMounted(async () => {
    if (ready && !user?.value?.id) {
        await navigateTo('/login')
        return
    }

    try {
        await fetchIssues()
    } catch (err) {
        error.value = err.message || 'Erreur lors du chargement'
    } finally {
        loading.value = false
    }
})

const isTeacher = computed(() => {
  return user.value?.roles?.includes('TEACHER') || user.value?.roles?.includes('ADMIN') || false
})

const afficherSectionIncomplete = ref(false)

</script>
