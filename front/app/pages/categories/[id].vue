<template>
  <!-- Affichage uniquement si la catégorie est chargée -->
  <div v-if="category?.category?.id !== null" class="min-h-screen bg-gray-50 p-8">

    <div class="max-w-7xl mx-auto px-4 py-6 space-y-4">

      <!-- NAVIGATION / RETOUR   -->
      <div class="flex items-center gap-2 text-sm text-gray-500">
        <NuxtLink to="/categories" class="hover:text-gray-800 flex items-center gap-1 transition-colors">
          <!-- Icône retour -->
          <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 19l-7-7m0 0l7-7m-7 7h18" />
          </svg>
          Retour aux catégories
        </NuxtLink>
        <span class="text-gray-300">/</span>

        <!-- ID catégorie -->
        <span class="text-gray-700 font-medium">Catégorie #{{ category?.category?.id }}</span>
      </div>

      <!-- HEADER -->
      <div class="flex justify-between items-start mb-8">

        <div>
          <!-- Sujet principal -->
          <h1 class="text-2xl font-bold text-gray-900">
            {{ category?.category?.subject }}
          </h1>

          <!-- Nom de la catégorie -->
          <p class="text-indigo-600 font-medium">
            {{ category?.category?.name }}
          </p>

          <!-- Propriétaire -->
          <p class="text-sm text-gray-500 mt-1">
            Propriétaire : {{ category?.category?.ownerName }}
          </p>
        </div>

        <!-- Statut de la catégorie -->
        <span class="px-3 py-1 text-xs rounded-full font-semibold" :class="category?.category?.status === 'ACTIVE'
          ? 'bg-green-100 text-green-600'
          : 'bg-red-100 text-red-600'">
          {{ category?.category?.status }}
        </span>

      </div>

      <!-- GRID -->
      <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <div class="lg:col-span-2 space-y-6">

          <!-- Followers -->
          <div class="bg-white rounded-xl shadow p-6">

            <div class="flex justify-between items-center mb-4">
              <h2 class="text-sm font-bold text-gray-500 uppercase tracking-widest">
                Professeurs abonnés
              </h2>
            </div>

            <!-- Liste des followers -->
            <div v-if="category?.followers.length > 0" class="space-y-2">
              <div v-for="user in category?.followers" :key="user.id"
                class="flex justify-between items-center bg-gray-50 p-3 rounded-lg border">
                <div>
                  <!-- Nom utilisateur -->
                  <p class="font-medium text-gray-900">
                    {{ user.name }}
                  </p>
                </div>

                <!-- Action suppression -->
                <button v-if="!checkTeacherIssueManager(user.id)" class="text-red-500 text-sm hover:underline cursor-pointer" @click="removeFollower(user.id)">
                  Retirer
                </button>

                <span v-else class="text-xs text-gray-400 mt-3">
                  L'utilisateur à un ticket ouvert
                </span>
              </div>
            </div>

            <!-- État vide -->
            <div v-else class="text-sm text-gray-400">
              Aucun professeur pour le moment
            </div>

          </div>

        </div>

        <!-- Ajout professeur -->
        <div class="bg-white rounded-xl shadow p-6">

          <h2 class="text-sm font-bold text-gray-500 uppercase tracking-widest mb-4">
            Ajouter un professeur
          </h2>

          <!-- Sélection professeur -->
          <select v-model="selectedTeacherId" class="w-full border rounded-lg p-2 mb-4">
            <option disabled value="">Sélectionner un professeur</option>

            <option v-for="t in availableTeachers" :key="t.id" :value="t.id" @click="selectedTeacherId = t.id">
              {{ t.name }}
            </option>
          </select>

          <!-- Action ajout -->
          <button
            class="w-full bg-indigo-600 hover:bg-indigo-700 text-white py-2 rounded-lg font-medium disabled:opacity-50"
            :disabled="!selectedTeacherId" :class="!selectedTeacherId ? '' : 'cursor-pointer'"
            @click="addFollower(selectedTeacherId)">
            Ajouter
          </button>

          <p class="text-xs text-gray-400 mt-3">
            Seuls les enseignants non déjà abonnés sont affichés
          </p>

        </div>

      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { useCategories } from '~/composables/useCategories'
import { useUsers } from '~/composables/useUsers'
import { useAuth } from '~/composables/useAuth'
import { ROLE } from '~/utils/roles'

/**
 * ID de la catégorie récupéré depuis l’URL
 */
const route = useRoute()
const categorieId = Number(route.params.id)

/**
 * Composable de gestion des catégories.
 */
const {
  category,
  fetchCategoryById,
  addTeacherFollower,
  deleteTeacherFollower,
  error: categoriesError
} = useCategories()

/**
 * Composable utilisateurs
 */
const { users, fetchUserByRoles } = useUsers()

/**
 * Etat utilisateur authentifié.
 */
const { user, ready } = useAuth()

/**
 * ID du professeur sélectionné dans le select
 */
const selectedTeacherId = ref(null)

/**
 * Liste des professeurs disponibles (non déjà abonnés + pas soi-même)
 */
const availableTeachers = computed(() => {

  if (!users.value || !category?.value?.followers) {
    return []
  }

  return users.value.filter(u => {

    const isAlreadyFollower =
      category.value.followers.some(
        follower => follower.id === u.id
      )

    const isCurrentUser =
      u.id === user.value?.id

    return !isAlreadyFollower && !isCurrentUser
  })

})

/**
 * Vérifie si l'enseignant est manager d'au moins une issue ouverte.
 *
 * @param {number} id Identifiant de l'enseignant.
 * @returns {boolean} true si l'enseignant est manager d'une issue, sinon false.
 */
const checkTeacherIssueManager = (id) => {
  const userToCheck = category.value.followers.find(
    follower => follower.id === id
  )

  return userToCheck?.issueManagerInCategory ?? false
}

/**
 * Chargement initial des catégories au montage de la page.
 */
onMounted(async () => {
  await fetchCategoryById(categorieId)
  await fetchUserByRoles([ROLE.TEACHER])
});

/**
 * Retirer un professeur d'une catégorie
 */
const removeFollower = async (id) => {
  try {
    await deleteTeacherFollower(id, category?.value?.category?.id)
  } catch (err) {
    console.error(err)
  }
}

/**
 * Ajouter un professeur à une catégorie
 */
const addFollower = async (id) => {
  try {
    await addTeacherFollower(id, category?.value?.category?.id)
  } catch (err) {
    console.error(err)
  }
}
</script>
