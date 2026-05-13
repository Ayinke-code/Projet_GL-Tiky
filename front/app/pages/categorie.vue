<template>
  <div class="p-8 h-full bg-gray-50">
    <div class="flex justify-between items-center mb-8">
      <h1 class="text-2xl font-bold text-gray-800">Gestion des Catégories</h1>
      <button @click="isCreateModalOpen = true"
        class="bg-indigo-600 hover:bg-indigo-700 text-white px-6 py-2 rounded-lg font-medium shadow-md transition-all flex items-center">
        <i class="fa-solid fa-plus mr-2"></i> Créer une catégorie
      </button>
    </div>

    <!-- MODALE CRÉATION -->
    <div v-if="isCreateModalOpen" class="fixed inset-0 bg-black/40 flex items-center justify-center z-50 p-4"
      @click.self="isCreateModalOpen = false">
      <div
        class="bg-white rounded-xl shadow-2xl w-full max-w-2xl p-0 overflow-hidden animate-in fade-in zoom-in duration-200">
        <div class="p-6 border-b border-gray-100 flex justify-between items-center bg-gray-50">
          <h2 class="text-lg font-bold text-gray-800">Nouvelle Catégorie</h2>
          <button @click="isCreateModalOpen = false" class="text-indigo-600 hover:text-indigo-800 transition-colors">
            <i class="fa-solid fa-xmark text-xl"></i>
          </button>
        </div>
        <div class="p-6">
          <CategoryForm :loading="loading" :error="error" @submit="handleCreate" @cancel="isCreateModalOpen = false" />
        </div>
      </div>
    </div>

    <!-- Main Content -->
    <div class="max-w-7xl mx-auto">
      <!-- Section : Mes Catégories & Suivies -->
      <div v-if="ownedAndFollowed.length > 0" class="mb-12">
        <h2 class="text-xs font-bold text-indigo-600 uppercase tracking-widest mb-4 flex items-center">
          <i class="fa-solid fa-star mr-2"></i> Mes Catégories & Suivies
        </h2>
        <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          <CategoryCard v-for="cat in ownedAndFollowed" :key="cat.id" :category="cat"
            :is-followed="followedCategories.includes(cat.id)" @click="selectedCat = cat" @follow="handleFollow(cat.id)"
            @unfollow="handleUnfollow(cat.id)" />
        </div>
        <hr class="mt-10 border-gray-200" />
      </div>

      <!-- Section : Autres Catégories -->
      <div v-if="otherActive.length > 0" class="mb-12">
        <h2 class="text-xs font-bold text-gray-400 uppercase tracking-widest mb-4 flex items-center">
          <i class="fa-solid fa-globe mr-2"></i> Autres Catégories
        </h2>
        <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          <CategoryCard v-for="cat in otherActive" :key="cat.id" :category="cat" :is-followed="false"
            @click="selectedCat = cat" @follow="handleFollow(cat.id)" @unfollow="handleUnfollow(cat.id)" />
        </div>
        <hr v-if="archivedCategories.length > 0" class="mt-10 border-gray-200" />
      </div>

      <!-- Section : Archives -->
      <div v-if="archivedCategories.length > 0" class="mb-12">
        <h2 class="text-xs font-bold text-red-400 uppercase tracking-widest mb-4 flex items-center">
          <i class="fa-solid fa-box-archive mr-2"></i> Archives
        </h2>
        <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          <CategoryCard v-for="cat in archivedCategories" :key="cat.id" :category="cat"
            :is-followed="followedCategories.includes(cat.id)" @click="selectedCat = cat" @follow="handleFollow(cat.id)"
            @unfollow="handleUnfollow(cat.id)" />
        </div>
      </div>

      <!-- MODALE UNIQUE (Détail / Édition / Archivage) -->
      <div v-if="selectedCat" class="fixed inset-0 bg-black/40 flex items-center justify-center z-50 p-4"
        @click.self="closeModal">
        <div
          class="bg-white rounded-xl shadow-2xl w-full max-w-lg p-6 animate-in fade-in zoom-in duration-200 relative">

          <!-- Bouton Fermer Absolu -->
        <button @click="closeModal"
            class="absolute top-4 right-4 text-red-400 hover:bg-red-50 p-2 rounded-full transition-all z-[60] bg-white shadow-sm border border-red-100 flex items-center justify-center w-5 h-5">
            <i class="fa-solid fa-xmark text-xl"></i> x
        </button>

          <!-- MODE VUE -->
          <div v-if="!isEditing">
            <div class="flex justify-between items-start mb-6">
              <div>
                <h2 class="text-2xl font-bold text-gray-900">{{ selectedCat.subject || selectedCat.name }}</h2>
                <p class="text-indigo-600 font-medium">{{ selectedCat.name }}</p>
              </div>
            </div>

            <!-- Liste des Issues -->
            <div class="mb-6">
              <h4 class="text-xs font-bold text-gray-400 uppercase mb-3">Issues liées (2)</h4>
              <div class="space-y-2">
                <div class="p-3 bg-gray-50 rounded border border-gray-100 text-sm">Problème équation 2nd degré...
                </div>
                <div class="p-3 bg-gray-50 rounded border border-gray-100 text-sm">Questions TP Physique...</div>
              </div>
            </div>

            <!-- Propriétaire et Followers -->
            <div class="mb-6 p-4 bg-gray-50 rounded-lg border border-gray-100">
              <p class="text-sm text-gray-600 mb-4">
                <i class="fa-solid fa-user-shield mr-2 text-indigo-500"></i>
                <strong>Propriétaire :</strong> {{ selectedCat.owner }}
              </p>
              <CategoryFollowers :category="selectedCat" />
            </div>

            <!-- Actions -->
            <div class="flex gap-3 pt-6 border-t">
              <template v-if="String(selectedCat.owner) === String(user?.id)">
                <button @click="startEdit"
                  class="flex-1 px-4 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700">Modifier</button>

                <button v-if="selectedCat.status !== 'archived'" @click="handleArchive"
                  class="flex-1 px-4 py-2 bg-red-50 text-red-600 rounded-lg hover:bg-red-100 transition-colors">
                  Archiver
                </button>

                <button v-else @click="handleUnarchive"
                  class="flex-1 px-4 py-2 bg-green-50 text-green-600 rounded-lg hover:bg-green-100 transition-colors">
                  Désarchiver
                </button>

                <button v-if="selectedCat.status === 'archived'" @click="handleDeletePermanent"
                  class="flex-1 px-4 py-2 bg-red-600 text-white rounded-lg hover:bg-red-700 transition-colors">
                  Supprimer définitivement
                </button>
              </template>
              <div v-else class="flex-1 text-center py-2 text-gray-400 text-sm italic">
                Lecture seule (Propriété de {{ selectedCat.owner }})
              </div>
            </div>
          </div>

          <!-- MODE ÉDITION -->
          <div v-else>
            <div class="mb-6">
              <h2 class="text-xl font-bold text-gray-900">Modifier la catégorie</h2>
              <p class="text-sm text-gray-500">Mettez à jour les informations de la catégorie</p>
            </div>

            <CategoryForm :loading="loading" :error="error" :initial-data="selectedCat" :is-edit="true"
              @submit="handleUpdate" @cancel="closeModal" />
          </div>

        </div>
      </div>

      <!-- Toast Message de succès -->
      <div v-if="successMessage" class="fixed bottom-4 right-4 bg-green-500 text-white px-6 py-3 rounded-lg shadow-lg">
        {{ successMessage }}
      </div>

    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useCategory } from '~/composables/useCategory'
import { useAuth } from '~/composables/useAuth'
import CategoryForm from '~/components/CategoryForm.vue'
import CategoryFollowers from '~/components/CategoryFollowers.vue'

const router = useRouter()

// Composables
const { user, canAccessCategories, followedCategories, followCategory, unfollowCategory, setFollowedCategories } = useAuth()

// Composables
const {
  categories, loading, error, createCategory, updateCategory, fetchCategories,
  archiveCategory, unarchiveCategory, followCategory: apiFollow,
  unfollowCategory: apiUnfollow, getFollowedCategories, deleteCategoryPermanent
} = useCategory(user)

// State
const apiError = ref('')
const successMessage = ref('')
const selectedCat = ref(null)
const isEditing = ref(false)
const isCreateModalOpen = ref(false)
const teachers = ref([])

// Computed
const ownedAndFollowed = computed(() => {
  return categories.value.filter(cat =>
    cat.status !== 'archived' &&
    (String(cat.owner) === String(user.value?.id) || followedCategories.value.includes(cat.id))
  ).sort((a, b) => {
    if (String(a.owner) === String(user.value?.id) && String(b.owner) !== String(user.value?.id)) return -1
    if (String(a.owner) !== String(user.value?.id) && String(b.owner) === String(user.value?.id)) return 1
    return 0
  })
})

const otherActive = computed(() => {
  return categories.value.filter(cat =>
    cat.status !== 'archived' &&
    String(cat.owner) !== String(user.value?.id) &&
    !followedCategories.value.includes(cat.id)
  )
})

const archivedCategories = computed(() => {
  return categories.value.filter(cat => cat.status === 'archived')
})

// Lifecycle
onMounted(async () => {
  if (!canAccessCategories.value) {
    router.push('/login')
    return
  }
  try {
    await fetchCategories()
    const followedIds = await getFollowedCategories()
    setFollowedCategories(followedIds)

    // Charger les profs pour l'édition
    const respProfs = await fetch('http://localhost:8080/api/users/teachers')
    if (respProfs.ok) {
      teachers.value = await respProfs.json()
    }
  } catch (err) {
    apiError.value = "Impossible de charger les catégories."
  }
})

// Methods
const handleFollow = async (id) => {
  await apiFollow(id)
  followCategory(id)
  // Rafraîchir pour voir les nouveaux followers
  await fetchCategories()
}

const handleUnfollow = async (id) => {
  await apiUnfollow(id)
  unfollowCategory(id)
  // Rafraîchir pour voir les nouveaux followers
  await fetchCategories()
}

const handleCreate = async (data) => {
  try {
    await createCategory(data)
    showSuccess("Catégorie créée avec succès !")
    isCreateModalOpen.value = false
    await fetchCategories()
  } catch (err) {
    // L'erreur est gérée par useCategory et affichée via la prop 'error'
  }
}

const handleArchive = async () => {
  try {
    await archiveCategory(selectedCat.value.id)
    await fetchCategories()
    showSuccess("Catégorie archivée")
    closeModal()
  } catch (err) {
    apiError.value = "Erreur lors de l'archivage"
  }
}

const handleUnarchive = async () => {
  try {
    await unarchiveCategory(selectedCat.value.id)
    await fetchCategories()
    showSuccess("Catégorie désarchivée")
    closeModal()
  } catch (err) {
    apiError.value = "Erreur lors du désarchivage"
  }
}

const handleDeletePermanent = async () => {
  if (!confirm("ATTENTION : Cette action est irréversible. Voulez-vous vraiment supprimer cette catégorie définitivement ?")) return

  try {
    await deleteCategoryPermanent(selectedCat.value.id)
    await fetchCategories()
    showSuccess("Catégorie supprimée définitivement")
    closeModal()
  } catch (err) {
    apiError.value = "Erreur lors de la suppression"
  }
}

const startEdit = () => {
  isEditing.value = true
}

const handleUpdate = async (formData) => {
  try {
    await updateCategory(selectedCat.value.id, formData)
    await fetchCategories()
    showSuccess("Catégorie mise à jour avec succès !")
    isEditing.value = false
    // Mettre à jour selectedCat avec les nouvelles données
    selectedCat.value = categories.value.find(c => c.id === selectedCat.value.id)
  } catch (err) {
    // Erreur gérée par useCategory
  }
}

const closeModal = () => {
  selectedCat.value = null
  isEditing.value = false
}


const showSuccess = (msg) => {
  successMessage.value = msg
  setTimeout(() => {
    successMessage.value = ''
  }, 3000)
}
</script>