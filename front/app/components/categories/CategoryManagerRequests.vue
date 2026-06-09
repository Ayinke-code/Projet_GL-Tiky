<template>
  <div class="bg-white rounded-xl shadow p-6 border border-indigo-50">
    <h2 class="text-sm font-bold text-gray-500 uppercase tracking-widest mb-4">
      Demandes de désinscription
    </h2>

    <div v-if="isLoading" class="text-sm text-gray-400 animate-pulse">
      Chargement des demandes...
    </div>

    <div v-else-if="filteredRequests.length > 0" class="space-y-3">
      <div
        v-for="req in filteredRequests"
        :key="req.id"
        class="bg-gray-50 p-3 rounded-lg border border-gray-100 flex flex-col gap-3"
      >
        <div>
          <p class="font-medium text-gray-900 text-sm">
            {{ getRequesterName(req.requesterId) }}
          </p>
          <p class="text-xs text-gray-500">Souhaite se retirer de la catégorie</p>
        </div>

        <div v-if="rejectingId !== req.id" class="flex gap-2">
          <button
            class="flex-1 bg-indigo-600 hover:bg-indigo-700 text-white px-3 py-1.5 rounded-md text-xs font-medium cursor-pointer transition-colors"
            @click="handleAccept(req.id)"
          >
            Accepter
          </button>
          <button
            class="flex-1 bg-white border border-gray-300 hover:bg-gray-50 text-gray-700 px-3 py-1.5 rounded-md text-xs font-medium cursor-pointer transition-colors"
            @click="startReject(req.id)"
          >
            Refuser
          </button>
        </div>

        <div v-else class="flex flex-col gap-2">
          <input
            v-model="rejectionReason"
            type="text"
            placeholder="Motif du refus..."
            class="w-full border border-red-200 rounded-md p-2 text-xs focus:ring-red-500 focus:border-red-500"
          />
          <div class="flex gap-2">
            <button
              class="flex-1 bg-red-500 hover:bg-red-600 text-white px-3 py-1.5 rounded-md text-xs font-medium cursor-pointer"
              @click="confirmReject(req.id)"
              :disabled="!rejectionReason.trim()"
              :class="!rejectionReason.trim() ? 'opacity-50 cursor-not-allowed' : ''"
            >
              Confirmer
            </button>
            <button
              class="flex-1 bg-gray-200 hover:bg-gray-300 text-gray-700 px-3 py-1.5 rounded-md text-xs font-medium cursor-pointer"
              @click="cancelReject"
            >
              Annuler
            </button>
          </div>
        </div>
      </div>
    </div>

    <div v-else class="text-sm text-gray-400">
      Aucune demande en attente.
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useDeletionCategoryRequest } from '~/composables/useDeletionCategoryRequests.js'

const props = defineProps({
  managerId: { type: Number, required: true },
  categoryId: { type: Number, required: true },
  followers: { type: Array, default: () => [] }
})

const emit = defineEmits(['request-processed'])

const { getPendingRequestsForManager, acceptRequest, rejectRequest } = useDeletionCategoryRequest()
const requests = ref([])
const isLoading = ref(true)
const rejectingId = ref(null)
const rejectionReason = ref('')

const filteredRequests = computed(() => {
  return requests.value.filter(r => r.categoryId === props.categoryId)
})

const getRequesterName = (id) => {
  const follower = props.followers.find(f => f.id === id)
  return follower ? follower.name : `Utilisateur #${id}`
}

const fetchRequests = async () => {
  isLoading.value = true
  try {
    requests.value = await getPendingRequestsForManager(props.managerId)
  } catch (error) {
    console.error(error)
  } finally {
    isLoading.value = false
  }
}

onMounted(() => {
  fetchRequests()
})

const handleAccept = async (id) => {
  try {
    await acceptRequest(id)
    requests.value = requests.value.filter(r => r.id !== id)
    emit('request-processed')
  } catch (error) {
    console.error(error)
  }
}

const startReject = (id) => {
  rejectingId.value = id
  rejectionReason.value = ''
}

const cancelReject = () => {
  rejectingId.value = null
  rejectionReason.value = ''
}

const confirmReject = async (id) => {
  if (!rejectionReason.value.trim()) return
  try {
    await rejectRequest(id, rejectionReason.value)
    requests.value = requests.value.filter(r => r.id !== id)
    cancelReject()
  } catch (error) {
    console.error(error)
  }
}
</script>
