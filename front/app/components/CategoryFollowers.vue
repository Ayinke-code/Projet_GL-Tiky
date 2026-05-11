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
    <div v-if="followers.length > 0" class="space-y-2">
      <div v-for="follower in followers" :key="follower.id" class="flex justify-between items-center p-2 bg-gray-100 rounded">
        <span>{{ follower.name }} ({{ follower.email }})</span>
        <Button v-if="isOwner" @click="removeFollower(follower.id)" color="error" size="sm" label="Remove" />
      </div>
    </div>
    <p v-else class="text-gray-500">No followers yet</p>
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
const { fetchFollowers, addFollower: apiAddFollower, removeFollower: apiRemoveFollower, fetchAvailableTeachers } = useCategory()

const followers = ref([])
const availableTeachers = ref([])
const selectedTeacherId = ref('')

const isOwner = computed(() => user.value?.id === props.category.ownerId) // Assuming ownerId is added to category

const loadFollowers = async () => {
  followers.value = await fetchFollowers(props.category.id)
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
    alert('Error removing follower: ' + e.message)
  }
}

onMounted(() => {
  loadFollowers()
  loadAvailableTeachers()
})
</script>