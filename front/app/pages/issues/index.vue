<template>
    <div class="p-8">
        <div class="flex justify-between items-center mb-8">
            <h1 class="text-3xl font-bold text-black">Tickets</h1>
            <UButton to="/issues/create" icon="i-lucide-plus" size="lg">
                Créer un ticket
            </UButton>
        </div>

      <!--Rebrique des affectations -->
      <div v-if="isTeacher" class="py-12">
        <section><AssignationManager :userId="user.id" @issue-updated="fetchIssues"/></section>

        <div class="relative py-8">
          <div class="absolute inset-0 flex items-center"><span class="w-full border-t border-gray-200"></span></div>
          <div class="relative flex justify-center"><span class="bg-white px-2 text-sm text-gray-400"></span></div>
        </div>

        <section><AssignationTeacher :userId="user.id" @issue-updated="fetchIssues"/></section>
      </div>

      <div
        v-if="user"
        v-show="afficherSectionIncomplete"
        class="bg-white p-6 mb-8 rounded-2xl shadow-sm border border-gray-100"
      >
        <IncompleteIssuesList
          :userId="user.id"
          @has-issues="afficherSectionIncomplete = $event"
          @issue-updated="fetchIssues"
        />
      </div>

      <div v-if="loading" class="text-center py-8">
          <USkeleton class="h-20 w-full mb-4" />
          <USkeleton class="h-20 w-full mb-4" />
          <USkeleton class="h-20 w-full" />
      </div>

      <div v-else-if="issues.length === 0" class="text-center py-12">
          <p class="text-gray-500 text-lg">Aucun ticket pour le moment</p>
          <UButton to="/issues/create" variant="outline" class="mt-4">
              Créer le premier ticket
          </UButton>
      </div>

        <template v-else>
            <!-- Les issues dont l'utilisateur est categoryOwner -->
            <div v-if="Object.keys(ownedIssuesByCategory).length > 0">
                <h2 class="text-xl font-bold text-black mb-3">Vos catégories</h2>

                <div v-for="(group, categoryId) in ownedIssuesByCategory" :key="categoryId" class="mb-4">
                    <!-- Composant collapse pour ouvrir/fermer l'affichage des issues de la catégorie -->
                    <UCollapsible class="w-full">

                        <!-- Bouton / Header -->
                        <UButton class="group justify-between cursor-pointer" color="neutral" variant="subtle"
                            trailing-icon="i-lucide-chevron-down" :label="group.name" :ui="{
                                trailingIcon: 'group-data-[state=open]:rotate-180 transition-transform duration-200'
                            }" block />

                        <!-- Contenu affiché quand ouvert -->
                        <template #content>
                            <div class="overflow-visible p-2">
                                <div class="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-4">
                                    <IssuesCard v-for="issue in group.issues" :key="issue.id" :issue="issue"
                                        :show-delete="canDeleteIssue(issue)"
                                        @delete="deleteValidation" />
                                </div>
                            </div>
                        </template>

                    </UCollapsible>
                </div>
            </div>

            <!-- Séparateur -->
            <template v-if="Object.keys(otherIssuesByCategory).length > 0 && Object.keys(ownedIssuesByCategory).length > 0">
                <Separator class="p-5" />
            </template>

            <!-- Les autres issues groupées par catégorie -->
            <div v-if="Object.keys(otherIssuesByCategory).length > 0">
                <h2 v-if="Object.keys(ownedIssuesByCategory).length === 0" class="text-xl font-bold text-black mb-3">Vos tickets</h2>
                <h2 v-else class="text-xl font-bold text-black mb-3">Autres</h2>

                <div v-for="(group, categoryId) in otherIssuesByCategory" :key="categoryId" class="mb-4">
                    <UCollapsible class="w-full">
                        <UButton class="group justify-between cursor-pointer" color="neutral" variant="subtle"
                            trailing-icon="i-lucide-chevron-down" :label="group.name" :ui="{
                                trailingIcon: 'group-data-[state=open]:rotate-180 transition-transform duration-200'
                            }" block />
                        <template #content>
                            <div class="overflow-visible p-2">
                                <div class="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-4">
                                    <IssuesCard v-for="issue in group.issues" :key="issue.id" :issue="issue"
                                        @delete="deleteValidation" />
                                </div>
                            </div>
                        </template>
                    </UCollapsible>
                </div>
            </div>
        </template>

        <UAlert v-if="error" color="error" class="mt-4" @close="error = null">
            <template #title>Erreur</template>
            {{ error }}
        </UAlert>
    </div>

    <!-- Modal de suppression d'issue -->
    <ConfirmModal v-model:open="isDeleteValidateModalOpen" title="Suppression"
        :description="`Voulez-vous vraiment supprimer le ticket : ${deletingIssue?.title}`" @confirm="handleDelete" />
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
import AssignationManager from '~/components/assignation/AssignationManager.vue'
import AssignationTeacher from '~/components/assignation/AssignationTeacher.vue'
import IncompleteIssuesList from '~/components/layout/IncompleteIssuesList.vue'

const { ready, user } = useAuth()
const { issues, fetchIssues, deleteIssue, error } = useIssues()
const { refreshFollowedIssues } = useFollowedIssuesState()

/**
 * State UI
 */
const loading = ref(true)

onMounted(async () => {
    if (ready && !user?.value?.id) {
        await navigateTo('/login')
        return
    }

    try {
        await Promise.all([
            fetchIssues(),
            refreshFollowedIssues()
        ])
    } catch (err) {
        console.error(err)
    } finally {
        loading.value = false
    }
})

/**
 * Issue en cours de suppression (null = aucune)
 */
const deletingIssue = ref(null)

/**
 * ouverture modal suppression
 */
const isDeleteValidateModalOpen = ref(false)

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

/*
 * Regroupement des autres issues par catégorie
 */
const otherIssuesByCategory = computed(() => {
    return otherIssues.value.reduce((groups, issue) => {
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

const canDeleteIssue = (issue) => {
    const currentUserId = user.value?.id
    if (!currentUserId || !issue) return false

    return currentUserId === issue.createdId
        || currentUserId === issue.categoryOwnerId
        || currentUserId === issue.categoryManagerId
}

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
