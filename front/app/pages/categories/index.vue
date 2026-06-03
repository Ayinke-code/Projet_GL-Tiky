<template>
  <div class="p-8 pb-8 min-h-screen bg-gray-50">

    <div class="flex flex-col gap-4 md:flex-row justify-between items-start md:items-center mb-8">
      <div>
        <h1 class="text-2xl font-bold text-gray-800">Gestion des catégories</h1>
        <p class="text-sm text-gray-600 mt-2">Mes catégories, abonnements, autres catégories et archives.</p>
      </div>

      <div class="flex flex-wrap gap-3">
        <Button id="create-categorie-teacher" v-if="ready && user?.value?.roles?.includes(ROLE.TEACHER)" @click="isCreateModalOpen = true"
          icon="i-lucide-plus" label="Créer une catégorie" color="secondary" size="lg" variant="solid" rounded />
        <Button id="create-categorie-student" v-else-if="ready && user?.value?.roles?.includes(ROLE.STUDENT)" @click="isRequestCreateModalOpen = true"
          icon="i-lucide-plus" label="Demande de catégorie" color="secondary" size="lg" variant="solid" rounded />
      </div>
    </div>

    <Alert v-if="categoriesError" color="error" title="Erreur" description="Impossible de charger les catégories.">
      {{ categoriesError }}
    </Alert>

    <div class="space-y-10">
      <section>
        <div class="mb-6">
          <div class="flex items-center justify-between gap-4 mb-3">
            <div>
              <h2 class="text-xl font-semibold text-gray-900">Mes catégories</h2>
              <p class="text-sm text-gray-600">Catégories dont vous êtes propriétaire.</p>
            </div>
          </div>
          <div v-if="ownedCategories.length > 0" class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            <CategoryCard v-for="cat in ownedCategories" :key="cat.id" :category="cat" :isFollowed="cat.followedByCurrentUser"
              @click="handleClick(cat)" @follow="handleFollow(cat.id)" @unfollow="handleUnfollow(cat.id)" />
          </div>
          <div v-else class="rounded-xl border border-dashed border-gray-200 p-8 text-center text-sm text-gray-500">
            Aucune catégorie possédée.
          </div>
        </div>
      </section>

      <section>
        <div class="mb-6">
          <div class="flex items-center justify-between gap-4 mb-3">
            <div>
              <h2 class="text-xl font-semibold text-gray-900">Abonnements</h2>
              <p class="text-sm text-gray-600">Catégories que vous suivez.</p>
            </div>
          </div>
          <div v-if="followedCategories.length > 0" class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            <CategoryCard v-for="cat in followedCategories" :key="cat.id" :category="cat" :isFollowed="cat.followedByCurrentUser"
              @click="handleClick(cat)" @follow="handleFollow(cat.id)" @unfollow="handleUnfollow(cat.id)" />
          </div>
          <div v-else class="rounded-xl border border-dashed border-gray-200 p-8 text-center text-sm text-gray-500">
            Vous ne suivez aucune catégorie pour le moment.
          </div>
        </div>
      </section>

      <section>
        <div class="mb-6">
          <div class="flex items-center justify-between gap-4 mb-3">
            <div>
              <h2 class="text-xl font-semibold text-gray-900">Autres catégories</h2>
              <p class="text-sm text-gray-600">Catégories actives disponibles à suivre.</p>
            </div>
          </div>
          <div v-if="otherCategories.length > 0" class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            <CategoryCard v-for="cat in otherCategories" :key="cat.id" :category="cat" :isFollowed="cat.followedByCurrentUser"
              @click="handleClick(cat)" @follow="handleFollow(cat.id)" @unfollow="handleUnfollow(cat.id)" />
          </div>
          <div v-else class="rounded-xl border border-dashed border-gray-200 p-8 text-center text-sm text-gray-500">
            Aucune autre catégorie active.
          </div>
        </div>
      </section>

      <section>
        <div class="mb-6">
          <div class="flex items-center justify-between gap-4 mb-3">
            <div>
              <h2 class="text-xl font-semibold text-gray-900">Archives</h2>
              <p class="text-sm text-gray-600">Catégories archivées.</p>
            </div>
          </div>
          <div v-if="archivedCategories.length > 0" class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            <CategoryCard v-for="cat in archivedCategories" :key="cat.id" :category="cat" :isFollowed="cat.followedByCurrentUser"
              @click="handleClick(cat)" @follow="handleFollow(cat.id)" @unfollow="handleUnfollow(cat.id)" />
          </div>
          <div v-else class="rounded-xl border border-dashed border-gray-200 p-8 text-center text-sm text-gray-500">
            Pas de catégories archivées.
          </div>
        </div>
      </section>
    </div>

    <div v-if="categoryRequests.length > 0 && categoryRequests.some(r => r.status === 'PENDING')">
      <Separator class="my-8" />
      <div class="flex justify-between items-center mb-8">
        <h2 class="text-2xl font-bold text-gray-800">Demandes des étudiants</h2>
      </div>
      <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        <CategoryRequestCard v-for="categoryRequest in categoryRequests.filter(r => r.status !== 'APPROVED')" :key="categoryRequest.id"
          :categoryRequest="categoryRequest" @accept="handleAcceptCategoryRequest" />
      </div>
    </div>

    <Modal v-model:open="isCreateModalOpen" title="Création de catégorie">
      <CategoryForms @submit="handleCreate" @cancel="isCreateModalOpen = false" />
    </Modal>

    <Modal v-model:open="isRequestCreateModalOpen" title="Demande de catégorie">
      <div class="flex justify-center">
        <CategoryRequestForms @submit="handleRequestCreate" @cancel="isRequestCreateModalOpen = false" />
      </div>
    </Modal>

    <Modal v-model:open="isDetailOpen" title="Détails de la catégorie">
      <div v-if="selectedCat" class="rounded-xl shadow-2xl w-full max-w-lg p-6">
        <h2 class="text-xl font-bold mb-2">{{ selectedCat.subject }}</h2>
        <p class="text-indigo-600 text-sm mb-6">{{ selectedCat.name }}</p>
        <div class="text-sm text-gray-500 mb-6">Propriétaire : {{ selectedCat.ownerName }}</div>
        <div class="flex justify-end gap-3">
          <Button @click="handleDelete(selectedCat.id)" color="error" label="Supprimer" size="lg" variant="subtle" rounded="true" />
        </div>
      </div>
    </Modal>
  </div>
</template>

<script setup>
import { computed, ref, onMounted } from 'vue'
import CategoryForms from '~/components/forms/CategoryForms.vue'
import CategoryCard from '~/components/categories/CategoryCard.vue'
import Modal from '~/components/ui/Modal.vue'
import Alert from '~/components/ui/Alert.vue'
import CategoryRequestForms from '~/components/forms/CategoryRequestForms.vue'
import Separator from '~/components/ui/Separator.vue'
import CategoryRequestCard from '~/components/categories/CategoryRequestCard.vue'
import Button from '~/components/ui/Button.vue'
import { ROLE } from '~/utils/roles'
import { useCategories } from '~/composables/useCategories'
import { useCategoryRequest } from '~/composables/useCategoryRequests'
import { useAuth } from '~/composables/useAuth'
import { useRouter } from 'vue-router'

const router = useRouter()

const {
  categories,
  fetchCategoriesByType,
  createCategory,
  deleteCategory,
  followCategory,
  unfollowCategory,
  error: categoriesError
} = useCategories()

const {
  categoryRequests,
  fetchCategoryRequests,
  createCategoryRequest,
  acceptCategoryRequest,
  error: categoryRequestsError
} = useCategoryRequest()

const { user, ready } = useAuth()

const selectedCat = ref(null)
const isCreateModalOpen = ref(false)
const isRequestCreateModalOpen = ref(false)
const isDetailOpen = ref(false)

const ownedCategories = computed(() => categories.value.filter(cat => cat.status !== 'ARCHIVED' && cat.ownerId === user?.value?.id))
const followedCategories = computed(() => categories.value.filter(cat => cat.status !== 'ARCHIVED' && cat.ownerId !== user?.value?.id && cat.followedByCurrentUser))
const otherCategories = computed(() => categories.value.filter(cat => cat.status !== 'ARCHIVED' && cat.ownerId !== user?.value?.id && !cat.followedByCurrentUser))
const archivedCategories = computed(() => categories.value.filter(cat => cat.status === 'ARCHIVED'))

const fetchData = async () => {
  await fetchCategoriesByType('ISSUE')
  await fetchCategoryRequests()
}

onMounted(fetchData)

const handleClick = (cat) => {
  if (!ready || !user?.value) return
  const isOwner = user.value.id === cat.ownerId
  const isTeacherRole = user.value.roles?.includes(ROLE.TEACHER)
  if (!isOwner) return
  if (isTeacherRole && cat?.id) {
    router.push(`/categories/${cat.id}`)
  }
}

const handleCreate = async (data) => {
  const res = await createCategory(data)
  if (!res) return
  isCreateModalOpen.value = false
  await fetchCategoriesByType('ISSUE')
}

const handleDelete = async (id) => {
  isDetailOpen.value = false
  await deleteCategory(id)
  selectedCat.value = null
  await fetchCategoriesByType('ISSUE')
}

const handleRequestCreate = async (data) => {
  const res = await createCategoryRequest(data)
  if (!res) return
  isRequestCreateModalOpen.value = false
  await router.push('/categories')
}

const handleAcceptCategoryRequest = async (data) => {
  const res = await acceptCategoryRequest(data.id)
  if (!res) return
  await fetchData()
}

const handleFollow = async (categoryId) => {
  if (!categoryId) return
  await followCategory(categoryId)
  await fetchCategoriesByType('ISSUE')
}

const handleUnfollow = async (categoryId) => {
  if (!categoryId) return
  await unfollowCategory(categoryId)
  await fetchCategoriesByType('ISSUE')
}
</script>
