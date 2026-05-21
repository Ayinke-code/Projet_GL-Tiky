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

        <template v-else>
            <!-- Les issues dont l'utilisateur est categoryOwner -->
            <div v-if="Object.keys(ownedIssuesByCategory).length > 0">
                <h2 class="text-xl font-bold text-black mb-3">Vos catégories</h2>
                <div v-for="(group, categoryId) in ownedIssuesByCategory" :key="categoryId" class="mb-8">
                    <h3 class="mb-3 text-xl font-semibold text-gray-700">
                        {{ group.name }}
                    </h3>

                    <div class="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-4">
                        <IssuesCard v-for="issue in group.issues" :key="issue.id" :issue="issue"
                            @delete="deleteValidation" />
                    </div>
                </div>
            </div>

            <!-- Séparateur -->
            <template v-if="Object.keys(ownedIssuesByCategory).length > 0 && ownedIssues.length > 0">
                <Separator class="p-5" />
            </template>

            <!-- Les autres issues -->
            <h2 v-if="ownedIssues.length > 0" class="text-xl font-bold text-black mb-3">Autres</h2>
            <div class="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-4 overflow-visible">
                <IssuesCard v-for="issue in otherIssues" :key="issue.id" :issue="issue" @delete="deleteValidation" />
            </div>
        </template>

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
import { computed } from 'vue'
import Separator from '~/components/ui/Separator.vue'

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

// Issues appartenant à l’utilisateur connecté
// Filtre sur le champ categoryOwnerId
const ownedIssues = computed(() => {
    return issues.value.filter(
        issue => issue.categoryOwnerId === user.value.id
    )
})

/*
 * Regroupement des issues du user par catégorie
 * Clé utilisée : categoryId
 */
const ownedIssuesByCategory = computed(() => {
    return ownedIssues.value.reduce((groups, issue) => {
        const key = issue.categoryId

        if (!groups[key]) {
            groups[key] = {
                name: issue.categoryName,
                issues: []
            }
        }

        groups[key].issues.push(issue)

        return groups
    }, {})
})

// Les autres issues n'appartenant pas à l'utilisateur connecté
const otherIssues = computed(() => {
    return issues.value.filter(
        issue => issue.categoryOwnerId !== user.value.id
    )
})

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
