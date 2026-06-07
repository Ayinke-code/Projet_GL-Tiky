<template>
  <div class="bg-white rounded-xl border border-gray-200 overflow-hidden shadow-sm">
    <div class="px-5 py-3 border-b border-gray-100 flex justify-between items-center bg-slate-50/50">
      <h2 class="text-xs font-bold text-gray-500 uppercase tracking-widest flex items-center gap-1.5">
        <i class="fa-solid fa-network-wired text-indigo-500"></i>
        Tickets Liés
      </h2>
      <UBadge v-if="visibleRelations.length || localPendingRequests.length" size="xs" color="gray" variant="solid" class="rounded-full">
        {{ visibleRelations.length + localPendingRequests.length }}
      </UBadge>
    </div>

    <div class="px-5 py-4 space-y-4">
      <div v-if="loading" class="text-center py-4 text-xs text-gray-400">
        Chargement des liaisons...
      </div>

      <p v-else-if="!visibleRelations.length && !localPendingRequests.length" class="text-xs text-gray-400 text-center py-2">
        Aucun ticket lié ou dépendance enregistrée.
      </p>

      <div v-else class="space-y-4 max-h-64 overflow-y-auto pr-1 custom-scrollbar">

        <div v-if="isManager && localPendingRequests.length" class="space-y-2">
          <h3 class="text-[10px] font-bold text-orange-500 uppercase tracking-wider mb-1 flex items-center gap-1">
            <UIcon name="i-lucide-bell-ring" class="w-3 h-3" />
            Demandes de liens en cours
          </h3>

          <div
            v-for="req in localPendingRequests"
            :key="req.id"
            class="group flex flex-col p-2.5 rounded-lg border text-xs"
            :class="hasMyApproval(req) ? 'border-gray-200 bg-gray-50/50' : 'border-orange-100 bg-orange-50/50'"
          >
            <div class="flex items-center gap-1.5 font-medium text-gray-800 mb-2">
              <span v-if="req.sourceIssueId === currentIssueId" class="truncate">
                {{ req.type === 'DEPENDS_ON' ? 'Veut dépendre de :' : 'Veut lier à :' }}
                <span class="text-[11px] text-gray-500 italic">{{ req.targetIssueTitle }}</span>
              </span>
              <span v-else class="truncate">
                {{ req.type === 'DEPENDS_ON' ? 'Requis par :' : 'Veut lier à :' }}
                <span class="text-[11px] text-gray-500 italic">{{ req.sourceIssueTitle }}</span>
              </span>
            </div>

            <div class="flex items-center justify-between mt-1">
              <UBadge size="xs" :color="hasMyApproval(req) ? 'gray' : 'orange'" variant="soft" class="text-[9px] px-1.5 py-0 uppercase font-bold tracking-wider">
                {{ hasMyApproval(req) ? 'Pré-approuvé' : 'À valider' }}
              </UBadge>

              <div v-if="hasMyApproval(req)" class="flex gap-1.5 text-gray-400 items-center text-[10px] italic font-medium">
                <UIcon name="i-lucide-hourglass" class="w-3 h-3" />
                Attente autre manager
              </div>

              <div v-else class="flex gap-1.5">
                <UButton
                  size="2xs"
                  color="green"
                  variant="soft"
                  icon="i-lucide-check"
                  class="cursor-pointer px-2"
                  @click="acceptRequest(req.id)"
                  :loading="actionLoading === req.id"
                >
                  Accepter
                </UButton>
                <UButton
                  size="2xs"
                  color="red"
                  variant="soft"
                  icon="i-lucide-x"
                  class="cursor-pointer px-2"
                  @click="openRejectModal(req.id)"
                  :disabled="actionLoading === req.id"
                >
                  Refuser
                </UButton>
              </div>
            </div>
          </div>
          <hr class="border-gray-100 my-2" />
        </div>

        <div class="space-y-2">
          <div
            v-for="rel in visibleRelations"
            :key="rel.id"
            class="group flex items-center justify-between p-2 rounded-lg border transition-colors text-xs"
            :class="rel.manager ? 'border-indigo-100 bg-indigo-50/30 hover:bg-indigo-50' : 'border-gray-100 bg-gray-50/50 hover:bg-gray-50'"
          >
            <div class="min-w-0 flex-1 pr-2">
              <div class="flex items-center gap-1.5 font-medium text-gray-800">
                <span v-if="rel.sourceIssueId === currentIssueId" class="truncate">
                  {{ rel.type === 'DEPENDS_ON' ? 'Dépend de :' : 'Lié à :' }}
                  <span class="text-[11px] text-gray-400 italic">{{ rel.targetIssueTitle }}</span>
                </span>
                <span v-else class="truncate">
                  {{ rel.type === 'DEPENDS_ON' ? 'Requis par :' : 'Lié à :' }}
                  <span class="text-[11px] text-gray-400 italic">{{ rel.sourceIssueTitle }}</span>
                </span>
              </div>

              <div class="mt-1 flex gap-1 flex-wrap">
                <UBadge
                  size="xs"
                  :color="rel.type === 'DEPENDS_ON' ? 'error' : 'info'"
                  variant="soft"
                  class="text-[9px] px-1.5 py-0 uppercase font-bold tracking-wider"
                >
                  {{ rel.type === 'DEPENDS_ON' ? 'Dépendance' : 'Association' }}
                </UBadge>
                <UBadge
                  v-if="rel.manager"
                  size="xs"
                  color="primary"
                  variant="solid"
                  class="text-[9px] px-1.5 py-0 uppercase font-bold tracking-wider"
                >
                  <UIcon name="i-lucide-shield-check" class="w-2.5 h-2.5 mr-0.5" />
                  Lien Officiel
                </UBadge>
                <UBadge
                  v-else-if="rel.createdById === props.currentUserId"
                  size="xs"
                  color="success"
                  variant="solid"
                  class="text-[9px] px-1.5 py-0 uppercase font-bold tracking-wider"
                >
                  Votre suggestion
                </UBadge>
              </div>
            </div>

            <UButton
              v-if="rel.createdById === props.currentUserId || isManager"
              size="xs"
              color="error"
              variant="ghost"
              icon="i-lucide-trash-2"
              class="opacity-0 group-hover:opacity-100 transition-opacity cursor-pointer shadow-none"
              @click="openConfirmDelete(rel.id)"
            />
          </div>

          <p v-if="deleteError" class="text-[11px] text-red-500 bg-red-50 border border-red-100 rounded-lg p-2 text-center mt-1">
            {{ deleteError }}
          </p>
        </div>
      </div>

      <hr class="border-gray-100 my-1" />

      <form v-if="canCreate" @submit.prevent="handleSubmit" class="space-y-3">
        <div>
          <label class="block text-[10px] font-bold text-gray-400 uppercase tracking-wider mb-1">
            Rechercher un ticket
          </label>
          <USelectMenu
            v-model="selectedTarget"
            :items="searchableIssues"
            :search-input="{ placeholder: 'Rechercher par titre ...' }"
            placeholder="Sélectionner un ticket..."
            class="w-full text-xs"
            @update:model-value="localError = null"
          />
        </div>

        <div>
          <label class="block text-[10px] font-bold text-gray-400 uppercase tracking-wider mb-1">
            Type de lien
          </label>
          <select
            v-model="form.type"
            class="w-full text-xs px-2 py-2 border border-gray-200 rounded-lg bg-gray-50/50 focus:bg-white outline-none transition-all"
          >
            <option value="LINKED">Liaison</option>
            <option value="DEPENDS_ON">Dépendance</option>
          </select>
        </div>

        <UButton
          type="submit"
          block
          size="sm"
          :loading="submitting"
          :disabled="!selectedTarget"
          color="secondary"
          :ui="{
            base: 'bg-violet-500 hover:bg-violet-600 disabled:bg-violet-300 text-white font-medium tracking-wide cursor-pointer shadow-sm w-full justify-center',
            loading: 'bg-violet-500'
          }"
        >
          Suggérer un nouveau lien
        </UButton>

        <p v-if="localError" class="text-[11px] text-red-500 bg-red-50 border border-red-100 rounded-lg p-2 text-center mt-1">
          {{ localError }}
        </p>
      </form>
    </div>

    <ConfirmModal
      v-model:open="isDeleteModalOpen"
      title="Supprimer le lien"
      description="Voulez-vous vraiment supprimer ce lien entre les deux tickets ?"
      @confirm="confirmDelete"
    />

    <Modal v-model:open="isRejectModalOpen" title="Refuser la demande">
      <p class="text-gray-600 mb-4 text-sm">Veuillez indiquer la raison de votre refus pour ce lien :</p>

      <div class="flex flex-col gap-3">
        <button
          v-for="option in rejectOptions"
          :key="option.value"
          type="button"
          @click.stop="selectedRejectReason = option.value"
          :class="[
            'w-full px-4 py-3 rounded-lg border-2 text-left transition',
            selectedRejectReason === option.value
              ? 'border-red-500 bg-red-50 text-red-700 font-semibold'
              : 'border-gray-200 bg-white text-gray-700 hover:border-red-300'
          ]"
        >
          <div class="font-medium text-sm">{{ option.label }}</div>
          <div class="text-xs mt-0.5" :class="selectedRejectReason === option.value ? 'text-red-600/80' : 'text-gray-500'">
            {{ option.description }}
          </div>
        </button>
      </div>

      <div class="flex justify-end gap-2 mt-6">
        <UButton color="gray" variant="ghost" @click="isRejectModalOpen = false">
          Annuler
        </UButton>
        <UButton
          color="red"
          :disabled="!selectedRejectReason || submittingReject"
          :loading="submittingReject"
          @click="confirmReject"
        >
          Confirmer le refus
        </UButton>
      </div>
    </Modal>
  </div>
</template>

<script setup>
import { computed, ref, onMounted } from 'vue'
import ConfirmModal from '~/components/ui/ConfirmModal.vue'
import Modal from '~/components/ui/Modal.vue'

const props = defineProps({
  currentIssueId: {
    type: Number,
    required: true
  },
  currentCategoryId: {
    type: Number,
    required: true
  },
  currentCategoryManagerId: {
    type: Number,
    default: null
  },
  canCreate: {
    type: Boolean,
    default: false
  },
  currentUserId: {
    type: Number,
    default: null
  }
})

const {
  relations,
  pendingRequests,
  loading,
  fetchRelationsByIssue,
  fetchPendingRequests,
  createRelation,
  deleteRelation,
  handleLinkRequest
} = useIssueRelations()
const { issues, fetchIssues } = useIssues()

// State Formulaire
const submitting = ref(false)
const localError = ref(null)
const deleteError = ref(null)
const selectedTarget = ref(null)
const form = ref({ type: 'LINKED' })

// State Suppression
const isDeleteModalOpen = ref(false)
const relationIdToDelete = ref(null)

// State Manager Actions
const actionLoading = ref(null)
const isRejectModalOpen = ref(false)
const requestToReject = ref(null)
const submittingReject = ref(false)
const selectedRejectReason = ref(null)

const rejectOptions = [
  { value: 'DUPLICATE_RELATION', label: 'Lien redondant', description: 'Un lien similaire ou équivalent existe déjà.' },
  { value: 'IRRELEVANT', label: 'Non pertinent', description: 'Il n\'y a pas de lien technique ou métier réel entre ces tickets.' },
  { value: 'WRONG_RELATION_TYPE', label: 'Type incorrect', description: 'Le type de relation (Dépendance/Association) est inadapté.' },
  { value: 'OTHER', label: 'Autre raison', description: 'Autre motif d\'annulation.' }
]

const isManager = computed(() => props.currentUserId && props.currentUserId === props.currentCategoryManagerId)

// On affiche tous les liens pour le manager,
// et pour l'étudiant on affiche les liens officiels + SES PROPRES suggestions
const visibleRelations = computed(() => {
  if (!props.currentUserId) return relations.value
  if (isManager.value) return relations.value

  return relations.value.filter(r => r.manager || r.createdById === props.currentUserId)
})

const localPendingRequests = computed(() => {
  if (!isManager.value) return []
  return pendingRequests.value.filter(r =>
    r.sourceIssueId === props.currentIssueId || r.targetIssueId === props.currentIssueId
  )
})

const searchableIssues = computed(() => {
  const myLinkedIds = new Set(
    relations.value
      .filter(r => r.createdById === props.currentUserId || r.manager)
      .flatMap(r => [r.sourceIssueId, r.targetIssueId])
  )
  return issues.value
    .filter(issue =>
      issue.id !== props.currentIssueId &&
      !myLinkedIds.has(issue.id) &&
      issue.categoryId === props.currentCategoryId
    )
    .map(issue => ({
      id: issue.id,
      label: `Ticket : ${issue.title}`
    }))
})

// Vérifie si le manager courant a déjà validé cette demande (ou s'il en est l'initiateur)
const hasMyApproval = (req) => {
  if (req.sourceManagerId === props.currentUserId && req.sourceManagerApproved === true) return true
  if (req.targetManagerId === props.currentUserId && req.targetManagerApproved === true) return true
  return false
}

onMounted(async () => {
  await fetchRelationsByIssue(props.currentIssueId)
  if (!issues.value.length) {
    await fetchIssues()
  }
  if (isManager.value) {
    await fetchPendingRequests()
  }
})

const handleSubmit = async () => {
  if (!selectedTarget.value) return
  submitting.value = true
  localError.value = null

  try {
    await createRelation(props.currentIssueId, selectedTarget.value.id, form.value.type)
    await fetchRelationsByIssue(props.currentIssueId)
    if (isManager.value) {
      await fetchPendingRequests()
    }
    selectedTarget.value = null
  } catch (err) {
    const status = err?.response?.status || err?.status || err?.statusCode
    if (status === 409) localError.value = err?.data?.message || "Ce lien existe déjà."
    else if (status === 403) localError.value = "Vous devez suivre ce ticket pour créer une liaison."
    else localError.value = err?.data?.message || "Erreur lors de la liaison."
  } finally {
    submitting.value = false
  }
}

const openConfirmDelete = (id) => {
  relationIdToDelete.value = id
  deleteError.value = null
  isDeleteModalOpen.value = true
}

const confirmDelete = async () => {
  if (!relationIdToDelete.value) return
  try {
    await deleteRelation(relationIdToDelete.value, props.currentIssueId)
    isDeleteModalOpen.value = false
    relationIdToDelete.value = null
  } catch (err) {
    deleteError.value = err?.data?.message || "Impossible de supprimer le lien."
    isDeleteModalOpen.value = false
  }
}

const acceptRequest = async (requestId) => {
  actionLoading.value = requestId
  try {
    await handleLinkRequest(requestId, true)
    await fetchRelationsByIssue(props.currentIssueId)
    await fetchPendingRequests()
  } catch (err) {
    console.error("Erreur approbation", err)
  } finally {
    actionLoading.value = null
  }
}

const openRejectModal = (requestId) => {
  requestToReject.value = requestId
  selectedRejectReason.value = null
  isRejectModalOpen.value = true
}

const confirmReject = async () => {
  if (!requestToReject.value || !selectedRejectReason.value) return
  submittingReject.value = true
  try {
    await handleLinkRequest(requestToReject.value, false, selectedRejectReason.value)
    isRejectModalOpen.value = false
    requestToReject.value = null
    await fetchRelationsByIssue(props.currentIssueId)
    await fetchPendingRequests()
  } catch (err) {
    console.error("Erreur refus", err)
  } finally {
    submittingReject.value = false
  }
}
</script>
