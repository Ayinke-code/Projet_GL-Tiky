<template>
  <div v-if="category?.category?.id !== null && ready" class="min-h-screen bg-gray-50 p-8">
    <div class="max-w-7xl mx-auto px-4 py-6 space-y-4">

      <div class="flex items-center gap-2 text-sm text-gray-500">
        <NuxtLink to="/categories" class="hover:text-gray-800 flex items-center gap-1 transition-colors">
          <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 19l-7-7m0 0l7-7m-7 7h18" />
          </svg>
          Retour aux catégories
        </NuxtLink>
        <span class="text-gray-300">/</span>
        <span class="text-gray-700 font-medium">Catégorie #{{ category?.category?.id }}</span>
      </div>

      <div class="flex justify-between items-start mb-8">
        <div>
          <h1 class="text-2xl font-bold text-gray-900">
            {{ category?.category?.subject }}
          </h1>
          <p class="text-indigo-600 font-medium">
            {{ category?.category?.name }}
          </p>
          <p class="text-sm text-gray-500 mt-1">
            Propriétaire : {{ category?.category?.ownerName }}
          </p>
        </div>
        <span class="px-3 py-1 text-xs rounded-full font-semibold" :class="category?.category?.status === 'ACTIVE' ? 'bg-green-100 text-green-600' : 'bg-red-100 text-red-600'">
          {{ category?.category?.status }}
        </span>
      </div>

      <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">

        <div class="lg:col-span-2 space-y-6">
          <div class="bg-white rounded-xl shadow p-6">
            <h2 class="text-sm font-bold text-gray-500 uppercase tracking-widest mb-4">
              Professeurs abonnés
            </h2>

            <div v-if="category?.followers.length > 0" class="space-y-2">
              <div v-for="follower in category?.followers" :key="follower.id" class="flex justify-between items-center bg-gray-50 p-3 rounded-lg border">
                <div>
                  <p class="font-medium text-gray-900">{{ follower.name }}</p>
                </div>

                <template v-if="isOwner">
                  <button v-if="!checkTeacherIssueManager(follower.id)" class="text-red-500 text-sm hover:underline cursor-pointer" @click="removeFollower(follower.id)">
                    Retirer
                  </button>
                  <span v-else class="text-xs text-gray-400 mt-3">
                    Billet ouvert
                  </span>
                </template>
              </div>
            </div>

            <div v-else class="text-sm text-gray-400">
              Aucun professeur pour le moment
            </div>
          </div>
        </div>

        <div class="space-y-6">

          <template v-if="isOwner">
            <CategoryManagerRequests
              :manager-id="user?.id"
              :category-id="category?.category?.id"
              :followers="category?.followers"
              @request-processed="refreshData"
            />

            <div class="bg-white rounded-xl shadow p-6">
              <h2 class="text-sm font-bold text-gray-500 uppercase tracking-widest mb-4">
                Ajouter un professeur
              </h2>
              <select v-model="selectedTeacherId" class="w-full border rounded-lg p-2 mb-4 text-sm">
                <option disabled value="">Sélectionner un professeur</option>
                <option v-for="t in availableTeachers" :key="t.id" :value="t.id">
                  {{ t.name }}
                </option>
              </select>
              <button
                class="w-full bg-indigo-600 hover:bg-indigo-700 text-white py-2 rounded-lg font-medium disabled:opacity-50"
                :disabled="!selectedTeacherId"
                :class="!selectedTeacherId ? '' : 'cursor-pointer'"
                @click="addFollower(selectedTeacherId)">
                Ajouter
              </button>
            </div>

            <div class="bg-white rounded-xl shadow p-6 border border-red-100">
              <h2 class="text-sm font-bold text-red-400 uppercase tracking-widest mb-1">
                Supprimer la catégorie
              </h2>
              <p class="text-xs text-gray-400 mb-4">
                La suppression est irréversible.
              </p>
              <button
                class="w-full bg-red-500 hover:bg-red-600 text-white py-2 rounded-lg font-medium cursor-pointer"
                @click="isDeleteCategoryModalOpen = true">
                Supprimer la catégorie
              </button>
            </div>
          </template>

          <template v-else-if="isFollower">
            <CategoryLeaveRequest
              :category-id="category?.category?.id"
            />
          </template>

        </div>

      </div>
    </div>
  </div>

  <ConfirmModal
    v-model:open="isDeleteCategoryModalOpen"
    title="Supprimer la catégorie"
    description="Voulez-vous vraiment supprimer cette catégorie ?"
    @confirm="handleDelete(category?.category?.id)"
  />
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useCategories } from '~/composables/useCategories'
import { useUsers } from '~/composables/useUsers'
import { useAuth } from '~/composables/useAuth'
import { ROLE } from '~/utils/roles'
import ConfirmModal from '~/components/ui/ConfirmModal.vue'
import CategoryLeaveRequest from "~/components/categories/CategoryLeaveRequest.vue";
import CategoryManagerRequests from "~/components/categories/CategoryManagerRequests.vue";

const router = useRouter()
const route = useRoute()
const categorieId = Number(route.params.id)

const isDeleteCategoryModalOpen = ref(false)

const {
  category,
  fetchCategoryById,
  addTeacherFollower,
  deleteTeacherFollower,
  deleteCategory,
} = useCategories()

const { users, fetchUserByRoles } = useUsers()
const { user, ready } = useAuth()
const selectedTeacherId = ref('')

const isOwner = computed(() => {
  return category.value?.category?.ownerId === user.value?.id
})

const isFollower = computed(() => {
  if (!category.value?.followers || !user.value) return false
  return category.value.followers.some(f => f.id === user.value.id)
})

const availableTeachers = computed(() => {
  if (!users.value || !category?.value?.followers) return []
  return users.value.filter(u => {
    const isAlreadyFollower = category.value.followers.some(f => f.id === u.id)
    const isCurrentUser = u.id === user.value?.id
    return !isAlreadyFollower && !isCurrentUser
  })
})

const checkTeacherIssueManager = (id) => {
  const userToCheck = category.value.followers.find(f => f.id === id)
  return userToCheck?.isIssueManagerInCategory ?? false
}

const refreshData = async () => {
  await fetchCategoryById(categorieId)
}

onMounted(async () => {
  await fetchCategoryById(categorieId)
  await fetchUserByRoles([ROLE.TEACHER])
})

const removeFollower = async (id) => {
  try {
    await deleteTeacherFollower(id, category?.value?.category?.id)
    await refreshData()
  } catch (err) {
    console.error(err)
  }
}

const addFollower = async (id) => {
  try {
    await addTeacherFollower(id, category?.value?.category?.id)
    selectedTeacherId.value = ''
    await refreshData()
  } catch (err) {
    console.error(err)
  }
}

const handleDelete = async (id) => {
  if(!id) return
  try {
    await deleteCategory(id)
    router.push('/categories')
  } catch(e) {
    console.error(e)
  }
}
</script>
