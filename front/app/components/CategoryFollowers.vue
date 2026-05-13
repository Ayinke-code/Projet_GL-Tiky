<template>
  <div class="mt-6">
    <h3 class="text-lg font-semibold mb-4">Followers</h3>

    <!-- Add follower section - only for owner -->
    <div v-if="isOwner" class="mb-4">
      <div class="flex gap-2">
        <select v-model="selectedTeacherId" class="flex-1 p-2 border rounded">
          <option value="">Select a teacher</option>
          <option v-for="teacher in availableTeachers" :key="teacher.id" :value="teacher.id">
            {{ teacher.name }} ({{ teacher.email }})
          </option>
        </select>
        <Button @click="addFollower" :disabled="!selectedTeacherId" label="Add" />
      </div>
    </div>

    <!-- Followers list -->
    <div v-if="followers.length > 0" class="space-y-2 mb-6">
      <div v-for="follower in followers" :key="follower.id"
        class="flex justify-between items-center p-2 bg-gray-100 rounded">
        <span class="text-sm">{{ follower.name }} ({{ follower.email }})</span>
        <Button v-if="isOwner" @click="removeFollower(follower.id)" color="error" size="sm" label="Retirer" />
      </div>
    </div>
    <p v-else class="text-gray-500 text-sm mb-6">Aucun abonné pour le moment</p>

    <!-- Pending Requests - only for owner -->
    <div v-if="isOwner && pendingRequests.length > 0" class="mt-8 pt-6 border-t border-gray-100">
      <h3 class="text-xs font-bold text-amber-600 uppercase mb-4 flex items-center">
        <i class="fa-solid fa-clock mr-2"></i> Demandes en attente
      </h3>
      <div class="space-y-3">
        <div v-for="req in pendingRequests" :key="req.id"
          class="flex items-center justify-between p-3 bg-amber-50 rounded-lg border border-amber-100">
          <div class="flex flex-col">
            <span class="text-xs font-bold text-amber-800">ID Professeur: {{ req.teacherId }}</span>
          </div>
          <div class="flex gap-2">
            <button @click="handleApprove(req.teacherId)"
              class="p-1.5 bg-green-600 text-white rounded hover:bg-green-700 transition-colors" title="Accepter">
              <i class="fa-solid fa-check"></i>
            </button>
            <button @click="handleReject(req.teacherId)"
              class="p-1.5 bg-red-600 text-white rounded hover:bg-red-700 transition-colors" title="Refuser">
              <i class="fa-solid fa-xmark"></i>
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useCategory } from '~/composables/useCategory'
import { useAuth } from '~/composables/useAuth'
import Button from '~/components/ui/Button.vue'

const props = defineProps({
  category: {
    type: Object,
    required: true
  }
})

const { user } = useAuth()
const {
  fetchFollowers,
  addFollower: apiAddFollower,
  removeFollower: apiRemoveFollower,
  fetchAvailableTeachers,
  approveFollower: apiApprove,
  rejectFollower: apiReject,
  fetchPendingRequests
} = useCategory()

const followers = ref([])
const pendingRequests = ref([])
const availableTeachers = ref([])
const selectedTeacherId = ref('')

const isOwner = computed(() => String(user.value?.id) === String(props.category.owner))

const loadFollowers = async () => {
  followers.value = await fetchFollowers(props.category.id)
  if (isOwner.value) {
    pendingRequests.value = await fetchPendingRequests(props.category.id)
  }
}

const loadAvailableTeachers = async () => {
  if (isOwner.value) {
    availableTeachers.value = await fetchAvailableTeachers(props.category.id)
  }
}

const addFollower = async () => {
  if (!selectedTeacherId.value) return
  try {
    await apiAddFollower(props.category.id, selectedTeacherId.value)
    await loadFollowers()
    await loadAvailableTeachers()
    selectedTeacherId.value = ''
  } catch (e) {
    alert('Error adding follower: ' + e.message)
  }
}

const removeFollower = async (userId) => {
  try {
    await apiRemoveFollower(props.category.id, userId)
    await loadFollowers()
    await loadAvailableTeachers()
  } catch (e) {
    alert('Erreur lors de la suppression: ' + e.message)
  }
}

const handleApprove = async (teacherId) => {
  try {
    await apiApprove(props.category.id, teacherId)
    await loadFollowers()
    await loadAvailableTeachers()
  } catch (e) {
    alert('Erreur lors de l\'approbation: ' + e.message)
  }
}

const handleReject = async (teacherId) => {
  try {
    await apiReject(props.category.id, teacherId)
    await loadFollowers()
    await loadAvailableTeachers()
  } catch (e) {
    alert('Erreur lors du rejet: ' + e.message)
  }
}

onMounted(() => {
  loadFollowers()
  loadAvailableTeachers()
})
</script>