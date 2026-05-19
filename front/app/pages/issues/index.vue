<template>
    <div class="p-8">
        <div class="flex justify-between items-center mb-8">
            <h1 class="text-3xl font-bold text-black">Issues</h1>
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

        <div v-else class="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-4 overflow-visible">
            <IssuesCard v-for="issue in issues" :key="issue.id" :issue="issue" @delete="deleteValidation" />
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

import IssuesCard from '~/components/issues/IssuesCard.vue'
import ConfirmModal from '~/components/ui/ConfirmModal.vue'

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
</script>
