<template>
  <div class="p-8">
    <div class="flex justify-between items-center mb-8">
      <h1 class="text-3xl font-bold text-black">Tickets</h1>
      <UButton to="/issues/create" id="btn-create-issue" icon="i-lucide-plus" size="lg" color="secondary" class="rounded-full font-bold">
        Créer un ticket
      </UButton>
    </div>

    <!--Rebrique des affectations -->
    <div v-if="isTeacher || isManager" class="py-12">
      <!-- Visible uniquement par les Managers -->
      <section v-if="isManager">
        <AssignationManager :userId="user.id" @issue-updated="getDisplayableIssues"/>
      </section>

      <!-- Séparateur visible uniquement si l'utilisateur est à la fois Manager ET Teacher -->
      <div v-if="isManager && isTeacher" class="relative py-8">
        <div class="absolute inset-0 flex items-center"><span class="w-full border-t border-gray-200"></span></div>
        <div class="relative flex justify-center"><span class="bg-white px-2 text-sm text-gray-400"></span></div>
      </div>

      <!-- Visible uniquement par les Professeurs -->
      <section v-if="isTeacher">
        <AssignationTeacher :userId="user.id" @issue-updated="getDisplayableIssues"/>
      </section>
    </div>

    <div
      v-if="user"
      v-show="afficherSectionIncomplete"
      class="bg-white p-6 mb-8 rounded-2xl shadow-sm border border-gray-100"
    >
      <IncompleteIssuesList
        :userId="user.id"
        @has-issues="afficherSectionIncomplete = $event"
        @issue-updated="getDisplayableIssues"
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
                    <IssuesCard v-if="user" v-for="issue in group.issues" :key="issue.id" :issue="issue" :user="user"
                        @delete="deleteValidation"
                        @requestAReopening="openReopenModal"
                        @acceptReopening="openReopenAccessModal"
                    />
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
                  <IssuesCard v-if="user" v-for="issue in group.issues" :key="issue.id" :issue="issue" :user="user"
                                        @delete="deleteValidation"
                                        @requestAReopening="openReopenModal"
                                        @acceptReopening="openReopenAccessModal"/>
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

  <!-- Modal permettant d'ajouter la description pour la demande de réouverture -->
    <Modal
    v-model:open="isRequestReopeningModalOpen"
    title="Demander la réouverture"
    >
        <!-- Texte explicatif et text area pour la description -->
        <div class="space-y-4 mb-5 text-center">
            <p class="text-sm text-gray-600">
                Cette demande sera envoyée à
                <span class="font-medium">
                    {{ selectedIssue?.categoryManagerName }}
                </span>
            </p>
            <UTextarea
                v-model="reopenDescription"
                class="w-full"
                color="info"
                placeholder="Décris pourquoi la résolution ne te convient pas..."
                :rows="6"
            />
        </div>

        <!-- Boutons footer -->
        <div class="flex justify-end gap-2">
            <UButton
                color="neutral"
                variant="ghost"
                @click="isRequestReopeningModalOpen = false"
                class="cursor-pointer"
            >
                Annuler
            </UButton>
            <UButton
                color="info"
                :disabled="!reopenDescription?.trim()"
                @click="submitReopenRequest"
                class="cursor-pointer"
            >
                Confirmer
            </UButton>
        </div>
    </Modal>

    <!-- Modal permettant d'accepter la demande de réouverture -->
    <Modal
    v-model:open="isAcceptRequestReopeningModalOpen"
    title="Accepter la demander la réouverture"
    >
        <!-- Texte explicatif et text area pour la description -->
        <div class="space-y-4 mb-5 text-center">
            <p class="text-sm text-gray-600">
                Demande faite par
                <span class="font-medium">
                {{ selectedIssue?.createdName }}
                </span>
            </p>

            <!-- Description fournie par l'utilisateur -->
            <div class="text-left p-3 rounded-lg bg-gray-50 border">
                <p class="text-xs text-gray-500 mb-1">
                Motif de la réouverture
                </p>

                <p class="text-sm">
                    {{ selectedIssue?.reopeningDescription }}
                </p>
            </div>
        </div>

        <!-- Boutons footer -->
        <div class="flex justify-end gap-2">
            <UButton
                color="neutral"
                variant="ghost"
                @click="isAcceptRequestReopeningModalOpen = false"
                class="cursor-pointer"
            >
                Annuler
            </UButton>
            <UButton
                color="info"
                @click="acceptReopening"
                class="cursor-pointer"
            >
                Confirmer
            </UButton>
        </div>
    </Modal>
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
import Modal from '~/components/ui/Modal.vue'

const { ready, user } = useAuth()
const { issues, getDisplayableIssues, fetchIssues, deleteIssue, requestOpening, updateIssueStatus, error } = useIssues()
const { refreshFollowedIssues } = useFollowedIssuesState()

const nuxtApp = useNuxtApp()

/**
 * State UI
 */
const loading = ref(true)

/**
 * Issue en cours de suppression (null = aucune)
 */
const deletingIssue = ref(null)

/**
 * ouverture modal suppression
 */
const isDeleteValidateModalOpen = ref(false)

/**
 * Ouverture du modal de demande de réouverture
 */
const isRequestReopeningModalOpen = ref(false)

/**
 * Ouverture du modal pour accepter la demande de réouverture
 */
const isAcceptRequestReopeningModalOpen = ref(false)

/**
 * Ticket sélectionné
 */
const selectedIssue = ref(null)

/**
 * Description de la réouverture
 */
const reopenDescription = ref(null)

/**
 * Affiche le modal de description de la demande de réouverture
 */
const openReopenModal = (issue) => {
  selectedIssue.value = issue
  isRequestReopeningModalOpen.value = true
}

/**
 * Modifier le status et envoie la description de réouverture
 */
const submitReopenRequest = async () => {
    isRequestReopeningModalOpen.value = false
    if(selectedIssue.value?.status !== "TERMINE") return

    try {
        requestOpening(selectedIssue.value?.id, reopenDescription.value)
    } catch (err) {
        error.value = err.message || 'Erreur lors de la suppression'
    }
}

/**
 * Affiche le modal de description de la demande de réouverture
 */
const openReopenAccessModal = (issue) => {
  selectedIssue.value = issue
  isAcceptRequestReopeningModalOpen.value = true
}

/**
 * Accepte la demande de réouverture d'un créateur d'issue
 */
const acceptReopening = async () => {
    isAcceptRequestReopeningModalOpen.value = false
    if(selectedIssue.value?.status !== "DEMANDE_REOUVERTURE") return

    try {
        updateIssueStatus(selectedIssue.value?.id, "OUVERT")
    } catch (err) {
        error.value = err.message || 'Erreur lors de la suppression'
    }
}

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
  } catch (err) {
    // L'erreur 404 est déjà gérée dans deleteIssue
    if (err?.response?.status !== 404) {
      error.value = err.message || 'Erreur lors de la suppression'
    }
  } finally {
    deletingIssue.value = null
    isDeleteValidateModalOpen.value = false
  }
}

/**
 * Handler nommé pour pouvoir le retirer proprement
 */
const ISSUE_NOTIFICATION_TYPES = [
  'NEW_ISSUE_IN_CATEGORIE',
  'RESPONSE_TO_MANAGER_ISSUE',
  'VALIDE_ISSUE',
  'CLOSING_ISSUE',
  'INCOMPLETE_ISSUE',
  'NEW_DEMANDE_MANAGER',
  'OPEN_ISSUE',
  'NEW_OPEN_ISSUE',
  'ACCEPTED_OPEN_ISSUE',
]

const onNotificationReceived = async (e) => {
  if (ISSUE_NOTIFICATION_TYPES.includes(e.detail?.type)) {
    await getDisplayableIssues()
  }
}

/**
 * Chargement initial
 */
onMounted(async () => {
  if (!ready.value || !user?.value?.id) {
    await navigateTo('/login')
    return
  }

  try {
    await getDisplayableIssues()
  } catch (err) {
    error.value = err.message || 'Erreur lors du chargement'
  } finally {
    loading.value = false
  }
  window.addEventListener('notification:received', onNotificationReceived)
})

onUnmounted(() => {
  window.removeEventListener('notification:received', onNotificationReceived)
})

const isTeacher = computed(() => {
  return user.value?.roles?.includes('TEACHER') || user.value?.roles?.includes('ADMIN') || false
})

const isManager = computed(() => {
  // Il est manager s'il possède des catégories ou s'il est admin
  return Object.keys(ownedIssuesByCategory.value).length > 0 || user.value?.roles?.includes('ADMIN') || false
})

const afficherSectionIncomplete = ref(false)



</script>
