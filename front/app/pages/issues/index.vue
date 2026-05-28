<template>
  <!-- Conteneur principal (Mode sombre d'origine restauré) -->
  <div class="p-8 bg-gray-950 min-h-screen text-white">

    <!-- En-tête de la page -->
    <div class="flex justify-between items-center mb-8">
      <h1 class="text-3xl font-bold text-white">Issues</h1>
      <UButton to="/issues/create" icon="i-lucide-plus" size="lg">
        Créer une issue
      </UButton>
    </div>

    <!-- Rubrique des affectations (Uniquement Admin/Teacher, condensée et pleine largeur) -->
    <div v-if="isTeacher" class="py-4 flex flex-col gap-4 w-full">
      <section class="w-full">
        <AssignationManager :userId="user.id" @issue-updated="fetchIssues"/>
      </section>

      <section class="w-full">
        <AssignationTeacher :userId="user.id" @issue-updated="fetchIssues"/>
      </section>
    </div>

    <!-- Section des tickets incomplets -->
    <div v-if="user" v-show="afficherSectionIncomplete" class="bg-gray-900 p-6 mb-8 rounded-2xl shadow-sm border border-gray-800 w-full">
      <IncompleteIssuesList :userId="user.id" @has-issues="afficherSectionIncomplete = $event" @issue-updated="fetchIssues" />
    </div>

    <!-- États de chargement -->
    <div v-if="loading" class="text-center py-8">
      <USkeleton class="h-20 w-full mb-4 bg-gray-800" />
      <USkeleton class="h-20 w-full mb-4 bg-gray-800" />
      <USkeleton class="h-20 w-full bg-gray-800" />
    </div>

    <!-- État vide -->
    <div v-else-if="issues.length === 0" class="text-center py-12">
      <p class="text-gray-400 text-lg">Aucune issue pour le moment</p>
      <UButton to="/issues/create" variant="outline" class="mt-4">
        Créer le premier ticket
      </UButton>
    </div>

    <!-- Affichage unifié en accordéons par catégorie (Pleine largeur) -->
    <template v-else>
      <div v-if="Object.keys(issuesByCategory).length > 0" class="w-full">
        <div v-for="(group, categoryId) in issuesByCategory" :key="categoryId" class="mb-4 w-full">
          <UCollapsible class="w-full">

            <!-- Bouton de l'accordéon -->
            <UButton class="group justify-between cursor-pointer"
                     color="neutral"
                     variant="subtle"
                     trailing-icon="i-lucide-chevron-down"
                     :label="group.name"
                     :ui="{ trailingIcon: 'group-data-[state=open]:rotate-180 transition-transform duration-200' }"
                     block
            />

            <!-- Contenu de l'accordéon (Lignes sur toute la largeur) -->
            <template #content>
              <div class="overflow-visible p-2 w-full">
                <div class="flex flex-col gap-2 w-full">
                  <IssuesCard v-for="issue in group.issues" :key="issue.id" :issue="issue" @delete="deleteValidation" />
                </div>
              </div>
            </template>

          </UCollapsible>
        </div>
      </div>
    </template>

    <!-- Alertes d'erreurs -->
    <UAlert v-if="error" color="error" class="mt-4" @close="error = null">
      <template #title>Erreur</template>
      {{ error }}
    </UAlert>
  </div>

  <!-- Fenêtre modale de confirmation de suppression -->
  <ConfirmModal v-model:open="isDeleteValidateModalOpen" title="Suppression" :description="`Voulez-vous vraiment supprimer le ticket : ${deletingIssue?.title}`" @confirm="handleDelete" />
</template>

<script setup lang="js">
import IssuesCard from '~/components/issues/IssuesCard.vue'
import ConfirmModal from '~/components/ui/ConfirmModal.vue'
import { computed, ref, onMounted } from 'vue'
import AssignationManager from '~/components/assignation/AssignationManager.vue'
import AssignationTeacher from '~/components/assignation/AssignationTeacher.vue'
import IncompleteIssuesList from '~/components/layout/IncompleteIssuesList.vue'

const { ready, user } = useAuth()
const { issues, fetchIssues, deleteIssue, error } = useIssues()

const loading = ref(true)
const deletingIssue = ref(null)
const isDeleteValidateModalOpen = ref(false)

const issuesByCategory = computed(() => {
  return issues.value.reduce((groups, issue) => {
    const key = issue.categoryId

    if (!groups[key]) {
      groups[key] = {
        name: issue.categoryName || 'Sans catégorie',
        issues: []
      }
    }

    groups[key].issues.push(issue)
    return groups
  }, {})
})

const deleteValidation = (issue) => {
  deletingIssue.value = {
    id: issue?.id,
    title: issue?.title
  }
  isDeleteValidateModalOpen.value = true
}

const handleDelete = async () => {
  if (!deletingIssue.value) return
  const id = deletingIssue.value.id

  try {
    await deleteIssue(id)
    issues.value = issues.value.filter(i => i.id !== id)
  } catch (err) {
    error.value = err.message || 'Erreur lors de la suppression'
  } finally {
    deletingIssue.value = null
    isDeleteValidateModalOpen.value = false
  }
}

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
