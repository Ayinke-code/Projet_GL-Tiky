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

    <!-- Affichage des Issues -->
    <template v-else>

      <!-- Barre de recherche + filtres + tri -->
      <div class="bg-white p-4 mb-6 rounded-2xl shadow-sm border border-gray-100">

        <div class="flex flex-col lg:flex-row gap-3 lg:items-center">

          <!-- Recherche -->
          <UInput
            v-model="searchQuery"
            icon="i-lucide-search"
            placeholder="Rechercher un ticket (titre, description, catégorie)..."
            size="lg"
            class="flex-1"
            color="neutral"
          >
            <template v-if="searchQuery" #trailing>
              <UButton
                color="neutral"
                variant="link"
                icon="i-lucide-x"
                aria-label="Effacer la recherche"
                @click="searchQuery = ''"
              />
            </template>
          </UInput>

          <!-- Filtre par statut -->
          <USelectMenu
            v-model="selectedStatuses"
            :items="statusItems"
            value-key="value"
            multiple
            icon="i-lucide-filter"
            placeholder="Statut"
            size="lg"
            class="w-full lg:w-48"
          />

          <!-- Filtre par catégorie -->
          <USelectMenu
            v-model="selectedCategories"
            :items="categoryItems"
            value-key="value"
            multiple
            icon="i-lucide-folder"
            placeholder="Catégorie"
            size="lg"
            class="w-full lg:w-56"
          />

          <!-- Tri -->
          <USelect
            v-model="sortBy"
            :items="sortItems"
            value-key="value"
            icon="i-lucide-arrow-up-down"
            size="lg"
            class="w-full lg:w-48"
            color="neutral"
          />
        </div>

        <!-- Mes catégories + réinitialiser -->
        <div class="mt-3 flex flex-wrap items-center gap-2">

          <!-- Toggle "Mes catégories" -->
          <UButton
            v-if="canOwnCategories"
            :color="onlyMyCategories ? 'primary' : 'neutral'"
            :variant="onlyMyCategories ? 'solid' : 'subtle'"
            icon="i-lucide-user-check"
            size="sm"
            class="cursor-pointer"
            @click="onlyMyCategories = !onlyMyCategories"
          >
            Mes catégories
          </UButton>

          <UButton
            v-if="hasActiveFilters"
            color="neutral"
            variant="ghost"
            icon="i-lucide-rotate-ccw"
            size="sm"
            class="cursor-pointer"
            @click="clearFilters"
          >
            Réinitialiser
          </UButton>

          <span class="text-sm text-gray-500 ml-auto">
            {{ totalFilteredCount }} ticket{{ totalFilteredCount > 1 ? 's' : '' }}
            <template v-if="hasActiveFilters">sur {{ issues.length }}</template>
          </span>
        </div>

        <!-- Chips des filtres actifs -->
        <div v-if="selectedStatuses.length || selectedCategories.length" class="mt-3 flex flex-wrap items-center gap-2">
          <UBadge
            v-for="status in selectedStatuses"
            :key="`chip-status-${status}`"
            :color="STATUS_META[status]?.color || 'neutral'"
            variant="soft"
            class="cursor-pointer"
            @click="removeStatus(status)"
          >
            {{ STATUS_META[status]?.label || status }}
            <UIcon name="i-lucide-x" class="ml-1" />
          </UBadge>

          <UBadge
            v-for="catId in selectedCategories"
            :key="`chip-cat-${catId}`"
            color="neutral"
            variant="soft"
            class="cursor-pointer"
            @click="removeCategory(catId)"
          >
            {{ categoryNameById[catId] || catId }}
            <UIcon name="i-lucide-x" class="ml-1" />
          </UBadge>
        </div>
      </div>

      <!-- Aucun résultat après filtrage -->
      <div v-if="totalFilteredCount === 0" class="text-center py-12">
        <p class="text-gray-500 text-lg">Aucun ticket ne correspond à votre recherche</p>
        <UButton variant="outline" class="mt-4 cursor-pointer" @click="clearFilters">
          Réinitialiser les filtres
        </UButton>
      </div>

      <!-- Grille de tous les tickets filtrés -->
      <div v-else class="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-4">
        <template v-if="user">
          <IssuesCard
            v-for="issue in displayedIssues"
            :key="issue.id"
            :issue="issue"
            :user="user"
            @delete="deleteValidation"
            @requestAReopening="openReopenModal"
            @acceptReopening="openReopenAccessModal"
          />
        </template>
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
 * - affichage en grille
 * - recherche + filtres (statut / catégorie) + tri
 * - suppression avec modal de confirmation
 */

import IssuesCard from '~/components/issues/IssuesCard.vue'
import ConfirmModal from '~/components/ui/ConfirmModal.vue'
import { computed } from 'vue'
import AssignationManager from '~/components/assignation/AssignationManager.vue'
import AssignationTeacher from '~/components/assignation/AssignationTeacher.vue'
import IncompleteIssuesList from '~/components/layout/IncompleteIssuesList.vue'
import Modal from '~/components/ui/Modal.vue'

const { ready, user } = useAuth()
const { issues, getDisplayableIssues, deleteIssue, requestOpening, updateIssueStatus, error } = useIssues()

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

/** Recherche et filtre */
/** Texte de recherche libre */
const searchQuery = ref('')

/** Statuts sélectionnés (tableau de codes statut) */
const selectedStatuses = ref([])

/** Catégories sélectionnées (tableau d'ids de catégorie) */
const selectedCategories = ref([])

/** N'afficher que les tickets de mes catégories (catégorie dont je suis responsable) */
const onlyMyCategories = ref(false)

/** Critère de tri courant */
const sortBy = ref('default')

const sortItems = [
  { value: 'default', label: 'Tri par défaut' },
  { value: 'title-asc', label: 'Titre (A → Z)' },
  { value: 'title-desc', label: 'Titre (Z → A)' },
  { value: 'status', label: 'Par statut' },
  { value: 'category', label: 'Par catégorie' },
]

/**
 * Métadonnées d'affichage des statuts (nom + couleur du badge).
 * Les statuts inconnus sont de couleur neutre.
 */
const STATUS_META = {
  OUVERT: { label: 'Ouvert', color: 'info' },
  EN_COURS: { label: 'En cours', color: 'warning' },
  EN_ATTENTE: { label: 'En attente', color: 'warning' },
  INCOMPLET: { label: 'Incomplet', color: 'neutral' },
  TERMINE: { label: 'Terminé', color: 'success' },
  DEMANDE_REOUVERTURE: { label: 'Demande de réouverture', color: 'secondary' },
}

/** L'utilisateur est-il responsable d'au moins une catégorie ? */
const canOwnCategories = computed(() =>
  issues.value.some(issue => issue.categoryOwnerId === user.value?.id)
)

/** Liste des statuts réellement présents dans les tickets */
const statusItems = computed(() => {
  const set = new Set(issues.value.map(i => i.status).filter(Boolean))
  return [...set].map(value => ({
    value,
    label: STATUS_META[value]?.label || value,
  }))
})

/** Liste des catégories présentes dans les tickets */
const categoryItems = computed(() => {
  const map = new Map()
  for (const issue of issues.value) {
    if (issue.categoryId != null && !map.has(issue.categoryId)) {
      map.set(issue.categoryId, issue.categoryName)
    }
  }
  return [...map].map(([value, label]) => ({ value, label }))
})

/** Accès rapide nom de catégorie par id */
const categoryNameById = computed(() =>
  Object.fromEntries(categoryItems.value.map(c => [c.value, c.label]))
)

/** Au moins un filtre est actif ? */
const hasActiveFilters = computed(() =>
  searchQuery.value.trim() !== '' ||
  selectedStatuses.value.length > 0 ||
  selectedCategories.value.length > 0 ||
  onlyMyCategories.value
)

/**
 * Issues après application de la recherche et des filtres.
 */
const filteredIssues = computed(() => {
  let result = issues.value

  const q = searchQuery.value.trim().toLowerCase()
  if (q) {
    result = result.filter(issue =>
      issue.title?.toLowerCase().includes(q) ||
      issue.description?.toLowerCase().includes(q) ||
      issue.categoryName?.toLowerCase().includes(q)
    )
  }

  if (selectedStatuses.value.length) {
    result = result.filter(issue => selectedStatuses.value.includes(issue.status))
  }

  if (selectedCategories.value.length) {
    result = result.filter(issue => selectedCategories.value.includes(issue.categoryId))
  }

  if (onlyMyCategories.value) {
    result = result.filter(issue => issue.categoryOwnerId === user.value?.id)
  }

  return result
})

/**
 * Issues filtrées puis triées (utilisé pour l'affichage de la grille).
 */
const displayedIssues = computed(() => {
  const list = [...filteredIssues.value]

  switch (sortBy.value) {
    case 'title-asc':
      return list.sort((a, b) => (a.title || '').localeCompare(b.title || ''))
    case 'title-desc':
      return list.sort((a, b) => (b.title || '').localeCompare(a.title || ''))
    case 'status':
      return list.sort((a, b) => (a.status || '').localeCompare(b.status || ''))
    case 'category':
      return list.sort((a, b) => (a.categoryName || '').localeCompare(b.categoryName || ''))
    default:
      return list
  }
})

/** Nombre total de tickets visibles après filtrage */
const totalFilteredCount = computed(() => filteredIssues.value.length)

/** Réinitialise tous les filtres */
const clearFilters = () => {
  searchQuery.value = ''
  selectedStatuses.value = []
  selectedCategories.value = []
  onlyMyCategories.value = false
}

const removeStatus = (status) => {
  selectedStatuses.value = selectedStatuses.value.filter(s => s !== status)
}

const removeCategory = (catId) => {
  selectedCategories.value = selectedCategories.value.filter(c => c !== catId)
}

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

const isTeacher = computed(() =>
  user.value?.roles?.includes('TEACHER') || user.value?.roles?.includes('ADMIN') || false
)

const isManager = computed(() =>
  // Manager s'il possède des catégories ou s'il est admin
  canOwnCategories.value || user.value?.roles?.includes('ADMIN') || false
)

const afficherSectionIncomplete = ref(false)



</script>
