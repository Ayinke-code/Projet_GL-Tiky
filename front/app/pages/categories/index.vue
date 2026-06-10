<template>
  <div class="p-8 pb-8 min-h-screen bg-gray-50">

    <!-- Header page -->
    <div class="flex justify-between items-center mb-8">
      <h1 class="text-2xl font-bold text-gray-800">Gestion des Catégories</h1>

      <!-- Bouton ouverture modal création pour les professeur -->
      <Button id="create-categorie-teacher" v-if="ready && user?.roles?.includes(ROLE.TEACHER)" @click="isCreateModalOpen = true"
              icon="i-lucide-plus" label="Créer une catégorie" color="secondary" size="lg" variant="solid" rounded />
      <!-- Bouton ouverture modal demande de création pour les étudiants -->
      <Button id="create-categorie-student" v-else-if="ready && user?.roles?.includes(ROLE.STUDENT)" @click="isRequestCreateModalOpen = true"
              icon="i-lucide-plus" label="Demande de catégorie" color="secondary" size="lg" variant="solid" rounded />
    </div>

    <!-- MODALE CREATION -->
    <Modal v-model:open="isCreateModalOpen" title="Création de catégorie">

      <!-- Affichage erreur API -->
      <Alert v-if="errorMessage" color="error" title="Erreur"
             description="La catégorie renseigné existe déja !">
        {{ errorMessage }}
      </Alert>

      <!-- Formulaire création -->
      <CategoryForms @submit="handleCreate" @cancel="isCreateModalOpen = false" />
    </Modal>

    <!-- MODALE DEMANDE DE CREATION -->
    <Modal v-model:open="isRequestCreateModalOpen" title="Demande de catégorie">

      <!-- Formulaire de demande de création -->
      <div class="flex justify-center">
        <CategoryRequestForms @submit="handleRequestCreate" @cancel="isRequestCreateModalOpen = false" />
      </div>
    </Modal>

    <!-- LISTE DES CATEGORIES -->
    <div v-if="categories.length > 0" class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">

      <CategoryCard
        v-for="cat in categories"
        :key="cat.id"
        :category="cat"
        :is-followed="checkIfUserIsFollower(cat)"
        @click="handleClick(cat)"
        @follow="handleFollow(cat?.id, user?.id)"
      />

    </div>

    <!-- LISTE DES DEMANDES DES ETUDIANTS -->
    <div v-if="categoryRequests.length > 0 && categoryRequests.some(r => r.status === 'PENDING')">
      <Separator class="my-8" />

      <!-- Header page -->
      <div class="flex justify-between items-center mb-8">
        <h1 class="text-2xl font-bold text-gray-800">Demande des étudiants</h1>
      </div>
      <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">

        <!-- Affichage des catégories request si pas encore validé -->
        <CategoryRequestCard v-for="categoryRequest in categoryRequests.filter(r => r.status !== 'APPROVED')" :key="categoryRequest.id" :categoryRequest="categoryRequest"
                             @accept="handleAcceptCategoryRequest" />

      </div>
    </div>

    <!-- MODALE DETAIL -->
    <Modal v-model:open="isDetailOpen" title="Détails de la catégorie">

      <!-- Protection rendu -->
      <div v-if="selectedCat" class="rounded-xl shadow-2xl w-full max-w-lg p-6">

        <!-- Titre -->
        <h2 class="text-xl font-bold mb-2">
          {{ selectedCat.subject }}
        </h2>

        <!-- Nom -->
        <p class="text-indigo-600 text-sm mb-6">
          {{ selectedCat.name }}
        </p>

        <!-- Owner -->
        <div class="text-sm text-gray-500 mb-6">
          Propriétaire : {{ selectedCat.ownerName }}
        </div>

        <!-- Actions -->
        <div class="flex justify-end gap-3">
          <Button @click="handleDelete(selectedCat.id)" color="error" label="Supprimer" size="lg"
                  variant="subtle" rounded="true">
            Supprimer
          </Button>
        </div>

      </div>
    </Modal>

  </div>
</template>

<script setup>
import CategoryForms from '~/components/forms/CategoryForms.vue'
import CategoryCard from '~/components/categories/CategoryCard.vue'
import Modal from '~/components/ui/Modal.vue'
import Alert from '~/components/ui/Alert.vue'
import CategoryRequestForms from '~/components/forms/CategoryRequestForms.vue'
import Separator from '~/components/ui/Separator.vue'
import CategoryRequestCard from '~/components/categories/CategoryRequestCard.vue'

import { ref, onMounted, onUnmounted } from 'vue'
import { useCategories } from '~/composables/useCategories'
import Button from '~/components/ui/Button.vue'
import { ROLE } from '~/utils/roles'
import { useAuth } from '~/composables/useAuth'

const {
  categories,
  fetchCategoriesByType,
  createCategory,
  deleteCategory,
  addTeacherFollower,
  error: categoriesError
} = useCategories()

const {
  user,
  ready
} = useAuth()

const {
  categoryRequests,
  fetchCategoryRequests,
  createCategoryRequest,
  acceptCategoryRequest,
  deleteCategoryRequest,
  error: categoryRequestsError
} = useCategoryRequest()

const selectedCat = ref(null)
const isCreateModalOpen = ref(false)
const isRequestCreateModalOpen = ref(false)
const isDetailOpen = ref(false)
const errorMessage = ref(null)

const CATEGORY_LIST_NOTIFICATION_TYPES = ['DELETING_CATEGORY', 'MANAGER_ADDING_TEACHER_CATEGORY']

const onNotificationReceived = async (e) => {
  if (CATEGORY_LIST_NOTIFICATION_TYPES.includes(e.detail?.type)) {
    await fetchCategoriesByType('ISSUE')
  }
}

onMounted(async () => {
  await fetchCategoriesByType('ISSUE')
  await fetchCategoryRequests()
  window.addEventListener('notification:received', onNotificationReceived)
})

onUnmounted(() => {
  window.removeEventListener('notification:received', onNotificationReceived)
})

/**
 * Vérifie si l'utilisateur courant est follower de la catégorie
 * (S'adapte selon la donnée renvoyée par votre backend dans le DTO liste)
 */
const checkIfUserIsFollower = (cat) => {
  const currentUserId = user?.value?.id || user?.id;

  if (!currentUserId) return false

  if (cat.followerIds && Array.isArray(cat.followerIds)) {
    return cat.followerIds.includes(currentUserId)
  }

  return cat.isFollowed === true
}

/**
 * Gestion du clic sur une catégorie.
 * Ouvre la page de détail si l'utilisateur est PROPRIÉTAIRE ou FOLLOWER.
 */
const handleClick = (cat) => {
  if (!ready || !user?.value) return

  const isTeacher = user.value.roles?.includes(ROLE.TEACHER)
  const isOwner = user.value.name === cat.ownerName
  const isFollower = checkIfUserIsFollower(cat)

  if (!isOwner && !isFollower) return

  if (isTeacher && cat?.id) {
    navigateTo(`/categories/${cat.id}`)
  }
}

const handleCreate = async (data) => {
  const res = await createCategory(data)

  if(res.error) {
    errorMessage.value = res.error;
    return
  }

  await fetchCategoriesByType('ISSUE')
  isCreateModalOpen.value = false
}

const handleDelete = async (id) => {
  isDetailOpen.value = false
  await deleteCategory(id)
  selectedCat.value = null
  await fetchCategoriesByType('ISSUE');
}

const handleRequestCreate = async (data) => {
  const res = await createCategoryRequest(data)
  if (!res) return
  isRequestCreateModalOpen.value = false
  await navigateTo('/categories')
}

const handleAcceptCategoryRequest = async (data) => {
  const res = await acceptCategoryRequest(data.id)
  if (!res) return
  await fetchCategoriesByType('ISSUE')
  await fetchCategoryRequests()
}

/**
 * Ajoute un utilisateur en tant que suiveur de la catégorie
 */
const handleFollow = async (idCat, idUser) => {
  if(!(idCat && idUser)) return
  const res = await addTeacherFollower(idUser, idCat)
  if (!res) return
}
</script>
