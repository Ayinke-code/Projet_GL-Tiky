<template>
  <div>
    <div class="p-8">
      <div class="flex justify-between items-center mb-8">
        <h1 class="text-3xl font-bold text-black">
          Tickets suivis
        </h1>
        <UButton to="/issues" variant="outline" icon="i-lucide-arrow-left">
          Retour aux tickets
        </UButton>
      </div>

      <div v-if="loading" class="text-center py-8">
        <USkeleton class="h-20 w-full mb-4" />
        <USkeleton class="h-20 w-full mb-4" />
        <USkeleton class="h-20 w-full" />
      </div>

      <div v-else-if="followedIssues.length === 0" class="text-center py-12">
        <p class="text-gray-500 text-lg">
          Vous ne suivez aucun ticket pour le moment
        </p>
        <UButton to="/issues" variant="outline" class="mt-4">
          Voir tous les tickets
        </UButton>
      </div>

      <div v-else class="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-4">
        <IssuesCard
          v-for="issue in followedIssues"
          :key="issue.id"
          :issue="issue"
          :user="user"
          @delete="deleteValidation"
        />
      </div>

      <UAlert v-if="error" color="error" class="mt-4">
        <template #title>
          Erreur
        </template>
        {{ error }}
      </UAlert>
    </div>

    <!-- Modal de suppression d'issue -->
    <ConfirmModal
      v-model:open="isDeleteValidateModalOpen"
      title="Suppression"
      :description="`Voulez-vous vraiment supprimer le ticket : ${deletingIssue?.title}`"
      @confirm="handleDelete"
    />
  </div>
</template>

<script setup lang="js">
/**
 * Page listant les issues suivies par l'utilisateur courant.
 * - Charge les issues via GET /issues/followed
 * - Affiche uniquement les issues avec statut OUVERT
 * - Redirige vers /login si non connecté
 * - Utilise un state global pour rester synchronisé avec les autres pages
 */

import IssuesCard from '~/components/issues/IssuesCard.vue'
import ConfirmModal from '~/components/ui/ConfirmModal.vue'

const { ready, user } = useAuth()
const { deleteIssue } = useIssues()
const { followedIssues, refreshFollowedIssues, error: stateError } = useFollowedIssuesState()

const loading = ref(true)
const error = computed(() => stateError.value)

const isDeleteValidateModalOpen = ref(false)
const deletingIssue = ref(null)

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
  } catch (err) {
    // L'erreur 404 est déjà gérée dans deleteIssue
    if (err?.response?.status !== 404) {
      console.error('Erreur lors de la suppression:', err)
    }
  } finally {
    deletingIssue.value = null
    isDeleteValidateModalOpen.value = false
  }
}

const onNotificationReceived = async (e) => {
  if (e.detail?.type === 'NEW_FOLLOWER' || e.detail?.type === 'NEW_UNFOLLOW') {
    await refreshFollowedIssues()
  }
}

onMounted(async () => {
  if (ready && !user?.value?.id) {
    await navigateTo('/login')
    return
  }

  try {
    await refreshFollowedIssues()
  } catch (err) {
    console.error('Erreur au chargement des issues suivies:', err)
  } finally {
    loading.value = false
  }
  window.addEventListener('notification:received', onNotificationReceived)
})

onUnmounted(() => {
  window.removeEventListener('notification:received', onNotificationReceived)
})
</script>
