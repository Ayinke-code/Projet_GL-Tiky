<template>
  <div class="bg-white rounded-xl border border-gray-200 overflow-hidden shadow-sm">
    <!-- HEADER -->
    <div class="px-5 py-3 border-b border-gray-100 flex justify-between items-center bg-slate-50/50">
      <h2 class="text-xs font-bold text-gray-500 uppercase tracking-widest flex items-center gap-1.5">
        <i class="fa-solid fa-network-wired text-indigo-500"></i>
        Tickets Liés
      </h2>
      <UBadge v-if="visibleRelations.length" size="xs" color="gray" variant="solid" class="rounded-full">
        {{ visibleRelations.length }}
      </UBadge>
    </div>

    <!-- CONTENU PRINCIPAL -->
    <div class="px-5 py-4 space-y-4">
      <div v-if="loading" class="text-center py-4 text-xs text-gray-400">
        Chargement des liaisons...
      </div>

      <p v-else-if="!visibleRelations.length" class="text-xs text-gray-400 text-center py-2">
        Aucun ticket lié ou dépendance enregistrée.
      </p>

      <!-- LISTE DES LIENS EXISTANTS -->
      <div v-else class="space-y-2 max-h-48 overflow-y-auto pr-1 custom-scrollbar">
        <div
          v-for="rel in visibleRelations"
          :key="rel.id"
          class="group flex items-center justify-between p-2 rounded-lg border border-gray-100 bg-gray-50/50 text-xs hover:bg-gray-50 transition-colors"
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

            <div class="mt-1 flex gap-1 flex-wrap items-center">
              <UBadge
                size="xs"
                :color="rel.type === 'DEPENDS_ON' ? 'error' : 'info'"
                variant="soft"
                class="text-[9px] px-1.5 py-0 uppercase font-bold tracking-wider"
              >
                {{ rel.type === 'DEPENDS_ON' ? 'Dépendance' : 'Association' }}
              </UBadge>

              <!-- Lien officiel validé -->
              <UBadge
                v-if="rel.status === 'OFFICIAL'"
                size="xs"
                color="primary"
                variant="solid"
                class="text-[9px] px-1.5 py-0 uppercase font-bold tracking-wider"
              >
                Validé
              </UBadge>

              <!-- Lien en attente de validation -->
              <UBadge
                v-else-if="rel.status === 'PENDING'"
                size="xs"
                color="warning"
                variant="solid"
                class="text-[9px] px-1.5 py-0 uppercase font-bold tracking-wider"
              >
                En attente
              </UBadge>

              <!-- Lien refusé -->
              <UBadge
                v-else-if="rel.status === 'REJECTED'"
                size="xs"
                color="error"
                variant="solid"
                class="text-[9px] px-1.5 py-0 uppercase font-bold tracking-wider"
              >
                Refusé
              </UBadge>

              <!-- Créé par l'utilisateur courant -->
              <UBadge
                v-if="rel.createdById === props.currentUserId"
                size="xs"
                color="success"
                variant="soft"
                class="text-[9px] px-1.5 py-0 uppercase font-bold tracking-wider"
              >
                Votre lien
              </UBadge>

              <!-- Boutons valider/refuser pour le manager validateur -->
              <div
                v-if="isManager && rel.status === 'PENDING' && rel.validatorId === props.currentUserId"
                class="flex gap-1 ml-1"
              >
                <UButton
                  size="xs"
                  color="success"
                  variant="soft"
                  class="text-[9px] cursor-pointer"
                  @click="handleValidate(rel.id, true)"
                >
                  ✓
                </UButton>
                <UButton
                  size="xs"
                  color="error"
                  variant="soft"
                  class="text-[9px] cursor-pointer"
                  @click="handleValidate(rel.id, false)"
                >
                  ✕
                </UButton>
              </div>
            </div>
          </div>

          <!-- BOUTON SUPPRIMER -->
          <UButton
            size="xs"
            color="error"
            variant="ghost"
            icon="i-lucide-trash-2"
            class="opacity-0 group-hover:opacity-100 transition-opacity cursor-pointer shadow-none"
            @click="openConfirmDelete(rel.id)"
          />
        </div>

        <!-- Feedback d'erreur suppression -->
        <p v-if="deleteError" class="text-[11px] text-red-500 bg-red-50 border border-red-100 rounded-lg p-2 text-center mt-1">
          {{ deleteError }}
        </p>
      </div>

      <hr class="border-gray-100 my-1" />

      <!-- FORMULAIRE D'ASSOCIATION -->
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
          Créer le lien
        </UButton>

        <p v-if="localError" class="text-[11px] text-red-500 bg-red-50 border border-red-100 rounded-lg p-2 text-center mt-1">
          {{ localError }}
        </p>
      </form>
    </div>

    <!-- MODAL DE CONFIRMATION SUPPRESSION -->
    <ConfirmModal
      v-model:open="isDeleteModalOpen"
      title="Supprimer le lien"
      description="Voulez-vous vraiment supprimer ce lien entre les deux tickets ?"
      @confirm="confirmDelete"
    />
  </div>
</template>

<script setup>
import ConfirmModal from '~/components/ui/ConfirmModal.vue'

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

const { relations, loading, fetchRelationsByIssue, createRelation, deleteRelation, validateRelation } = useIssueRelations()
const { issues, fetchIssues } = useIssues()

const submitting = ref(false)
const localError = ref(null)
const deleteError = ref(null)
const selectedTarget = ref(null)
const isDeleteModalOpen = ref(false)
const relationIdToDelete = ref(null)

const form = ref({ type: 'LINKED' })

const isManager = computed(() => props.currentUserId === props.currentCategoryManagerId)

const visibleRelations = computed(() => {
  if (!props.currentUserId) return relations.value

  if (isManager.value) {
    // Le manager voit tous les liens sauf les liens non-officiels
    // qui ont un équivalent officiel (OFFICIAL) pour la même paire
    const officialPairs = new Set(
      relations.value
        .filter(r => r.status === 'OFFICIAL')
        .map(r => [r.sourceIssueId, r.targetIssueId].sort().join('-'))
    )
    return relations.value.filter(r => {
      if (r.status === 'OFFICIAL') return true
      const pair = [r.sourceIssueId, r.targetIssueId].sort().join('-')
      return !officialPairs.has(pair)
    })
  }

  // Étudiant : voit ses propres liens et les liens OFFICIAL uniquement
  return relations.value.filter(r =>
    r.status === 'OFFICIAL' || r.createdById === props.currentUserId
  )
})

const searchableIssues = computed(() => {
  const myLinkedIds = new Set(
    relations.value
      .filter(r => r.createdById === props.currentUserId)
      .flatMap(r => [r.sourceIssueId, r.targetIssueId])
  )
  const officialLinkedIds = new Set(
    relations.value
      .filter(r => r.status === 'OFFICIAL')
      .flatMap(r => [r.sourceIssueId, r.targetIssueId])
  )
  return issues.value
    .filter(issue =>
      issue.id !== props.currentIssueId &&
      !myLinkedIds.has(issue.id) &&
      !officialLinkedIds.has(issue.id) &&
      issue.categoryId === props.currentCategoryId
    )
    .map(issue => ({
      id: issue.id,
      label: `Ticket : ${issue.title}`
    }))
})

onMounted(async () => {
  fetchRelationsByIssue(props.currentIssueId)
  if (!issues.value.length) {
    await fetchIssues()
  }
})

const handleSubmit = async () => {
  if (!selectedTarget.value) return
  submitting.value = true
  localError.value = null
  try {
    await createRelation(props.currentIssueId, selectedTarget.value.id, form.value.type)
    selectedTarget.value = null
  } catch (err) {
    const status = err?.response?.status || err?.status || err?.statusCode
    if (status === 409) {
      localError.value = err?.data?.message || "Ce lien existe déjà."
    } else if (status === 403) {
      localError.value = "Vous devez suivre ce ticket pour créer une liaison."
    } else {
      localError.value = err?.data?.message || "Erreur lors de la liaison."
    }
  } finally {
    submitting.value = false
  }
}

const handleValidate = async (relationId, accept) => {
  try {
    await validateRelation(relationId, accept, props.currentIssueId)
  } catch (err) {
    deleteError.value = err?.data?.message || "Erreur lors de la validation."
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
</script>
