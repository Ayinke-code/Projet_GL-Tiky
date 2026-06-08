<template>
  <div class="space-y-4">

    <div v-if="pendingMerges.length" class="bg-white rounded-xl border border-purple-200 overflow-hidden shadow-sm">
      <div class="px-5 py-3 border-b border-purple-100 bg-purple-50/50 flex items-center gap-2">
        <UIcon name="i-lucide-git-merge" class="text-purple-500" />
        <h2 class="text-xs font-bold text-purple-600 uppercase tracking-widest">Demandes de fusion</h2>
        <span class="ml-auto inline-flex items-center justify-center w-5 h-5 rounded-full bg-purple-100 text-purple-600 text-xs font-bold">
          {{ pendingMerges.length }}
        </span>
      </div>

      <div class="p-3 space-y-3">
        <div
          v-for="req in pendingMerges"
          :key="req.id"
          class="p-3 rounded-lg bg-purple-50 border border-purple-100 text-xs space-y-2"
        >
          <div class="space-y-1">
            <p class="font-semibold text-gray-700">
              Ticket à fusionner :
              <NuxtLink :to="`/issues/${req.sourceIssueId}`" class="text-purple-600 hover:underline">
                #{{ req.sourceIssueId }}
              </NuxtLink>
              <span v-if="req.sourceIssueTitle" class="font-normal text-gray-500 ml-1">— {{ req.sourceIssueTitle }}</span>
            </p>
            <p class="font-semibold text-gray-700">
              Fusionner dans :
              <NuxtLink :to="`/issues/${req.targetIssueId}`" class="text-blue-600 hover:underline">
                #{{ req.targetIssueId }}
              </NuxtLink>
              <span v-if="req.targetIssueTitle" class="font-normal text-gray-500 ml-1">— {{ req.targetIssueTitle }}</span>
            </p>
          </div>

          <div class="flex items-center gap-3 pt-1">
            <span class="flex items-center gap-1" :class="req.sourceManagerApproved ? 'text-emerald-600' : 'text-gray-400'">
              <UIcon :name="req.sourceManagerApproved ? 'i-lucide-check-circle-2' : 'i-lucide-circle-dashed'" class="w-3.5 h-3.5" />
              Gestionnaire du ticket
            </span>
            <span class="flex items-center gap-1" :class="req.targetManagerApproved ? 'text-emerald-600' : 'text-gray-400'">
              <UIcon :name="req.targetManagerApproved ? 'i-lucide-check-circle-2' : 'i-lucide-circle-dashed'" class="w-3.5 h-3.5" />
              Gestionnaire de destination
            </span>
          </div>

          <div class="flex gap-2 pt-1">
            <UButton
              size="xs"
              color="success"
              :loading="loadingId === req.id"
              :disabled="!!loadingId"
              @click="openAcceptModal(req.id)"
            >
              <template #leading>
                <UIcon name="i-lucide-check" class="w-3 h-3" />
              </template>
              Accepter
            </UButton>
            <UButton
              size="xs"
              color="error"
              variant="soft"
              :disabled="!!loadingId"
              @click="openRejectModal(req.id)"
            >
              <template #leading>
                <UIcon name="i-lucide-x" class="w-3 h-3" />
              </template>
              Refuser
            </UButton>
          </div>
        </div>
      </div>
    </div>

    <div v-if="deniedMerges.length" class="bg-white rounded-xl border border-red-200 overflow-hidden shadow-sm">
      <div class="px-5 py-3 border-b border-red-100 bg-red-50/50 flex items-center gap-2">
        <UIcon name="i-lucide-ban" class="text-red-500" />
        <h2 class="text-xs font-bold text-red-600 uppercase tracking-widest">Fusions refusées</h2>
        <span class="ml-auto inline-flex items-center justify-center w-5 h-5 rounded-full bg-red-100 text-red-600 text-xs font-bold">
          {{ deniedMerges.length }}
        </span>
      </div>

      <div class="p-3 space-y-3">
        <div
          v-for="req in deniedMerges"
          :key="req.id"
          class="p-3 rounded-lg bg-red-50/50 border border-red-100 text-xs space-y-2"
        >
          <div class="space-y-1">
            <p class="font-semibold text-gray-700">
              Tentative de fusion de :
              <NuxtLink :to="`/issues/${req.sourceIssueId}`" class="text-red-600 hover:underline">
                #{{ req.sourceIssueId }}
              </NuxtLink>
              <span class="mx-1 font-normal text-gray-500">vers</span>
              <NuxtLink :to="`/issues/${req.targetIssueId}`" class="text-blue-600 hover:underline">
                #{{ req.targetIssueId }}
              </NuxtLink>
            </p>
          </div>

          <div class="mt-2 p-2.5 bg-white rounded border border-red-100 text-gray-600 italic shadow-sm">
            <span class="font-semibold not-italic text-gray-800 block mb-1">Motif :</span>
            "{{ req.rejectionReason || 'Aucun motif renseigné' }}"
          </div>
        </div>
      </div>
    </div>

    <div
      v-if="showAcceptModal"
      class="fixed inset-0 bg-black/40 flex items-center justify-center z-50 p-4"
      @click.self="closeAcceptModal"
    >
      <div class="bg-white rounded-xl shadow-2xl w-full max-w-md overflow-hidden animate-in fade-in zoom-in duration-200">

        <div class="p-6 border-b border-gray-100 bg-gray-50 flex justify-between items-center">
          <div class="flex items-center gap-2">
            <UIcon name="i-lucide-git-merge" class="text-purple-500 w-5 h-5" />
            <h2 class="text-lg font-bold text-gray-800">Confirmer la fusion</h2>
          </div>
          <button @click="closeAcceptModal" class="text-gray-400 hover:text-gray-600 transition-colors">✕</button>
        </div>

        <div class="p-6 space-y-4">
          <p class="text-sm text-gray-600">
            Vous êtes sur le point d'accepter cette demande de fusion.
            Cette action est <span class="font-semibold text-purple-700">irréversible</span> — le ticket absorbé sera clôturé
            et ses abonnés transférés automatiquement.
          </p>

          <div class="rounded-lg bg-purple-50 border border-purple-100 px-4 py-3 text-xs text-purple-800">
            <p class="flex items-center gap-1.5">
              <UIcon name="i-lucide-info" class="w-3.5 h-3.5 shrink-0" />
              Êtes-vous sûr de vouloir confirmer ?
            </p>
          </div>

          <div class="flex justify-end gap-2 pt-1">
            <button
              @click="closeAcceptModal"
              class="px-4 py-2 text-sm rounded-lg border border-gray-300 text-gray-600 hover:bg-gray-50 transition-colors"
            >
              Annuler
            </button>
            <button
              @click="handleAccept"
              :disabled="!!loadingId"
              class="px-4 py-2 text-sm rounded-lg bg-purple-600 text-white hover:bg-purple-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors flex items-center gap-1.5"
            >
              <svg v-if="loadingId" class="animate-spin h-3.5 w-3.5" fill="none" viewBox="0 0 24 24">
                <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" />
                <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8z" />
              </svg>
              <UIcon v-else name="i-lucide-git-merge" class="w-3.5 h-3.5" />
              Confirmer la fusion
            </button>
          </div>
        </div>
      </div>
    </div>

    <div
      v-if="showRejectModal"
      class="fixed inset-0 bg-black/40 flex items-center justify-center z-50 p-4"
      @click.self="closeRejectModal"
    >
      <div class="bg-white rounded-xl shadow-2xl w-full max-w-md overflow-hidden animate-in fade-in zoom-in duration-200">

        <div class="p-6 border-b border-gray-100 bg-gray-50 flex justify-between items-center">
          <div class="flex items-center gap-2">
            <UIcon name="i-lucide-git-merge" class="text-purple-500 w-5 h-5" />
            <h2 class="text-lg font-bold text-gray-800">Motif du refus</h2>
          </div>
          <button @click="closeRejectModal" class="text-gray-400 hover:text-gray-600 transition-colors">✕</button>
        </div>

        <div class="p-6 space-y-4">
          <p class="text-sm text-gray-600">
            Veuillez indiquer la raison pour laquelle vous refusez cette demande de fusion.
            Ce motif sera visible par le demandeur.
          </p>

          <div class="space-y-1.5">
            <label class="text-xs font-semibold text-gray-600 uppercase tracking-wide">
              Motif <span class="text-red-500">*</span>
            </label>
            <textarea
              v-model="rejectReason"
              rows="4"
              placeholder="Ex : Les tickets concernent des catégories différentes..."
              maxlength="500"
              class="w-full px-3 py-2 text-sm border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-purple-300 focus:border-purple-400 resize-none transition"
              :class="{ 'border-red-400 ring-1 ring-red-300': rejectError }"
            />
            <div class="flex justify-between items-center">
              <p v-if="rejectError" class="text-xs text-red-500">{{ rejectError }}</p>
              <p v-else class="text-xs text-transparent">_</p>
              <p class="text-xs text-gray-400">{{ rejectReason.length }}/500</p>
            </div>
          </div>

          <div class="flex justify-end gap-2 pt-1">
            <button
              @click="closeRejectModal"
              class="px-4 py-2 text-sm rounded-lg border border-gray-300 text-gray-600 hover:bg-gray-50 transition-colors"
            >
              Annuler
            </button>
            <button
              @click="handleReject"
              :disabled="!!loadingId"
              class="px-4 py-2 text-sm rounded-lg bg-purple-600 text-white hover:bg-purple-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors flex items-center gap-1.5"
            >
              <svg v-if="loadingId" class="animate-spin h-3.5 w-3.5" fill="none" viewBox="0 0 24 24">
                <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" />
                <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8z" />
              </svg>
              Confirmer le refus
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'

const props = defineProps({
  currentIssueId: { type: Number, required: true },
  currentUserId:  { type: Number, required: true }
})

const { pendingRequests, deniedRequests, fetchPendingRequests, fetchDeniedRequests, respondToMergeRequest } = useMergeIssues()

// État local
const showAcceptModal = ref(false)
const showRejectModal = ref(false)
const selectedRequestId = ref(null)
const rejectReason = ref('')
const rejectError = ref('')
const loadingId = ref(null)

// Filtre les demandes en attente concernant le ticket courant
const pendingMerges = computed(() =>
  pendingRequests.value.filter(r =>
    (r.sourceIssueId === props.currentIssueId || r.targetIssueId === props.currentIssueId)
    && r.requestedById !== props.currentUserId
  )
)

// Filtre l'historique des refus concernant le ticket courant
const deniedMerges = computed(() =>
  deniedRequests.value.filter(r =>
    r.sourceIssueId === props.currentIssueId || r.targetIssueId === props.currentIssueId
  )
)

const openAcceptModal = (id) => {
  selectedRequestId.value = id
  showAcceptModal.value = true
}

const closeAcceptModal = () => {
  showAcceptModal.value = false
  selectedRequestId.value = null
}

const openRejectModal = (id) => {
  selectedRequestId.value = id
  rejectReason.value = ''
  rejectError.value = ''
  showRejectModal.value = true
}

const closeRejectModal = () => {
  showRejectModal.value = false
  selectedRequestId.value = null
  rejectReason.value = ''
  rejectError.value = ''
}

const handleAccept = async () => {
  loadingId.value = selectedRequestId.value
  try {
    const result = await respondToMergeRequest(selectedRequestId.value, true)
    closeAcceptModal()
    if (result?.status === 'ACCEPTED' && result?.targetIssueId) {
      await navigateTo(`/issues/${result.targetIssueId}`)
    }
  } catch (e) {
    console.error(e)
  } finally {
    loadingId.value = null
  }
}

const handleReject = async () => {
  if (!rejectReason.value.trim()) {
    rejectError.value = 'Le motif de refus est obligatoire.'
    return
  }
  if (rejectReason.value.trim().length < 10) {
    rejectError.value = 'Le motif doit contenir au moins 10 caractères.'
    return
  }

  loadingId.value = selectedRequestId.value
  try {
    await respondToMergeRequest(selectedRequestId.value, false, rejectReason.value.trim())
    closeRejectModal()
  } catch (e) {
    console.error(e)
    rejectError.value = 'Une erreur est survenue, veuillez réessayer.'
  } finally {
    loadingId.value = null
  }
}

onMounted(() => {
  fetchPendingRequests()
  fetchDeniedRequests()
})
</script>
