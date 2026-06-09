<template>
  <div class="bg-white rounded-xl shadow p-6 border border-orange-100">
    <h2 class="text-sm font-bold text-orange-400 uppercase tracking-widest mb-1">
      Quitter la catégorie
    </h2>
    <p class="text-xs text-gray-400 mb-4">
      Une fois la demande envoyée, le propriétaire devra la valider.
    </p>

    <div v-if="isChecking" class="text-xs text-gray-400 animate-pulse text-center py-2">
      Vérification du statut...
    </div>

    <button
      v-else
      class="w-full bg-orange-500 hover:bg-orange-600 text-white py-2 rounded-lg font-medium transition-colors disabled:opacity-50"
      :disabled="isLoading || isPending"
      :class="isLoading || isPending ? 'cursor-not-allowed' : 'cursor-pointer'"
      @click="submitRequest"
    >
      {{ isPending ? 'Demande en attente...' : 'Demander à quitter' }}
    </button>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useDeletionCategoryRequest } from '~/composables/useDeletionCategoryRequests.js'

const props = defineProps({
  categoryId: {
    type: Number,
    required: true
  }
})

const { createRequest, checkPendingRequest } = useDeletionCategoryRequest()

const isChecking = ref(true)
const isLoading = ref(false)
const isPending = ref(false)

onMounted(async () => {
  try {
    const hasPending = await checkPendingRequest(props.categoryId)
    isPending.value = hasPending
  } catch (error) {
    console.error('Erreur lors de la vérification de la demande :', error)
  } finally {
    isChecking.value = false
  }
})

const submitRequest = async () => {
  isLoading.value = true
  try {
    await createRequest(props.categoryId)
    isPending.value = true
  } catch (error) {
    console.error('Erreur lors de la demande :', error)

    if (error.response?.status === 400 || error.response?.status === 409) {
      isPending.value = true
    }
  } finally {
    isLoading.value = false
  }
}
</script>

aa
