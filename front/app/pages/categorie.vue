<template>
  <div class="p-8 h-full bg-gray-50">
    <div class="flex justify-between items-center mb-8">
      <h1 class="text-2xl font-bold text-gray-800">Gestion des Catégories</h1>
      <button 
        @click="isCreateModalOpen = true" 
        class="bg-indigo-600 hover:bg-indigo-700 text-white px-6 py-2 rounded-lg font-medium shadow-md transition-all flex items-center"
      >
        <i class="fa-solid fa-plus mr-2"></i> Créer une catégorie
      </button>
    </div>
    
    <!-- MODALE CRÉATION -->
    <div v-if="isCreateModalOpen" class="fixed inset-0 bg-black/40 flex items-center justify-center z-50 p-4" @click.self="isCreateModalOpen = false">
      <div class="bg-white rounded-xl shadow-2xl w-full max-w-2xl p-0 overflow-hidden animate-in fade-in zoom-in duration-200">
        <div class="p-6 border-b border-gray-100 flex justify-between items-center bg-gray-50">
          <h2 class="text-lg font-bold text-gray-800">Nouvelle Catégorie</h2>
          <button @click="isCreateModalOpen = false" class="text-gray-400 hover:text-gray-600">
            <i class="fa-solid fa-xmark text-xl"></i>
          </button>
        </div>
        <div class="p-6">
          <CategoryForm :loading="loading" :error="apiError" @submit="handleCreate" @cancel="isCreateModalOpen = false" />
        </div>
      </div>
    </div>

    <!-- Section : Mes Catégories & Suivies -->
    <div v-if="ownedAndFollowed.length > 0" class="mb-12">
      <h2 class="text-xs font-bold text-indigo-600 uppercase tracking-widest mb-4 flex items-center">
        <i class="fa-solid fa-star mr-2"></i> Mes Catégories & Suivies
      </h2>
      <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        <CategoryCard 
          v-for="cat in ownedAndFollowed" 
          :key="cat.id" 
          :category="cat"
          :is-followed="followedCategories.includes(cat.id)"
          @click="selectedCat = cat" 
          @follow="handleFollow(cat.id)"
          @unfollow="handleUnfollow(cat.id)"
        />
      </div>
      <hr class="mt-10 border-gray-200" />
    </div>

    <!-- Section : Autres Catégories -->
    <div v-if="otherActive.length > 0" class="mb-12">
      <h2 class="text-xs font-bold text-gray-400 uppercase tracking-widest mb-4 flex items-center">
        <i class="fa-solid fa-globe mr-2"></i> Autres Catégories
      </h2>
      <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        <CategoryCard 
          v-for="cat in otherActive" 
          :key="cat.id" 
          :category="cat"
          :is-followed="false"
          @click="selectedCat = cat" 
          @follow="handleFollow(cat.id)"
          @unfollow="handleUnfollow(cat.id)"
        />
      </div>
      <hr v-if="archivedCategories.length > 0" class="mt-10 border-gray-200" />
    </div>

    <!-- Section : Archives -->
    <div v-if="archivedCategories.length > 0" class="mb-12">
      <h2 class="text-xs font-bold text-red-400 uppercase tracking-widest mb-4 flex items-center">
        <i class="fa-solid fa-box-archive mr-2"></i> Archives
      </h2>
      <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        <CategoryCard 
          v-for="cat in archivedCategories" 
          :key="cat.id" 
          :category="cat"
          :is-followed="followedCategories.includes(cat.id)"
          @click="selectedCat = cat" 
          @follow="handleFollow(cat.id)"
          @unfollow="handleUnfollow(cat.id)"
        />
      </div>
    </div>

    <!-- MODALE UNIQUE (Détail / Édition / Archivage) -->
    <div v-if="selectedCat" class="fixed inset-0 bg-black/40 flex items-center justify-center z-50 p-4" @click.self="closeModal">
      <div class="bg-white rounded-xl shadow-2xl w-full max-w-lg p-6 animate-in fade-in zoom-in duration-200 relative">
        
        <!-- Bouton Fermer Absolu -->
        <button 
          @click="closeModal" 
          class="absolute top-4 right-4 text-gray-400 hover:text-gray-600 hover:bg-gray-100 p-2 rounded-full transition-colors z-10"
        >
          <i class="fa-solid fa-xmark text-xl"></i>
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
              <div class="p-3 bg-gray-50 rounded border border-gray-100 text-sm">Problème équation 2nd degré...</div>
              <div class="p-3 bg-gray-50 rounded border border-gray-100 text-sm">Questions TP Physique...</div>
            </div>
          </div>

          <!-- Actions -->
          <div class="flex gap-3 pt-6 border-t">
            <template v-if="selectedCat.owner === user?.id">
              <button @click="startEdit" class="flex-1 px-4 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700">Modifier</button>
              
              <button 
                v-if="selectedCat.status !== 'archived'" 
                @click="handleArchive" 
                class="flex-1 px-4 py-2 bg-red-50 text-red-600 rounded-lg hover:bg-red-100 transition-colors"
              >
                Archiver
              </button>
              
              <button 
                v-else 
                @click="handleUnarchive" 
                class="flex-1 px-4 py-2 bg-green-50 text-green-600 rounded-lg hover:bg-green-100 transition-colors"
              >
                Désarchiver
              </button>

              <button 
                v-if="selectedCat.status === 'archived'" 
                @click="handleDeletePermanent" 
                class="flex-1 px-4 py-2 bg-red-600 text-white rounded-lg hover:bg-red-700 transition-colors"
              >
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
          <div class="flex justify-between items-start mb-4">
            <h3 class="text-lg font-bold">Modifier la catégorie</h3>
          </div>
          <div class="space-y-4">
            <input v-model="editForm.name" class="w-full border p-2 rounded" placeholder="Matière" />
            <input v-model="editForm.subject" class="w-full border p-2 rounded" placeholder="Sujet" />
            <label class="text-[10px] font-bold text-gray-400 uppercase">Propriétaire (Transférer)</label>
            <select v-model="editForm.teacher" class="w-full border p-2 rounded">
              <option value="teacher-default">Prof. Martin (Moi)</option>
              <option value="teacher-002">Dr. Smith</option>
              <option value="teacher-003">Mme. Durand</option>
            </select>
            <div class="flex gap-3 mt-6 pt-4 border-t">
              <button @click="saveEdit" class="flex-1 bg-green-600 hover:bg-green-700 text-white py-2 rounded-lg">Sauvegarder</button>
              <button @click="isEditing = false" class="flex-1 bg-gray-200 hover:bg-gray-300 text-gray-800 py-2 rounded-lg">Annuler</button>
            </div>
          </div>
        </div>

      </div>
    </div>
    
    <!-- Toast Message de succès -->
    <div v-if="successMessage" class="fixed bottom-4 right-4 bg-green-500 text-white px-6 py-3 rounded-lg shadow-lg">
      {{ successMessage }}
    </div>

  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useCategory } from '~/composables/useCategory' 
 
const { 
  categories, loading, createCategory, updateCategory, fetchCategories, 
  archiveCategory, unarchiveCategory, followCategory: apiFollow, 
  unfollowCategory: apiUnfollow, getFollowedCategories, deleteCategoryPermanent 
} = useCategory()

const { user, canAccessCategories, followedCategories, followCategory, unfollowCategory, setFollowedCategories } = useAuth()
 
const ownedAndFollowed = computed(() => {
  return categories.value.filter(cat => 
    cat.status !== 'archived' && 
    (cat.owner === user.value?.id || followedCategories.value.includes(cat.id))
  ).sort((a, b) => {
    // On met les "Owned" avant les simples "Followed"
    if (a.owner === user.value?.id && b.owner !== user.value?.id) return -1
    if (a.owner !== user.value?.id && b.owner === user.value?.id) return 1
    return 0
  })
})

const otherActive = computed(() => {
  return categories.value.filter(cat => 
    cat.status !== 'archived' && 
    cat.owner !== user.value?.id && 
    !followedCategories.value.includes(cat.id)
  )
})

const archivedCategories = computed(() => {
  return categories.value.filter(cat => cat.status === 'archived')
})

onMounted(async () => {
  if (!canAccessCategories.value) {
    router.push('/login')
    return
  }
  await fetchCategories()
  const followedIds = await getFollowedCategories()
  setFollowedCategories(followedIds)
})

const handleFollow = async (id) => {
  await apiFollow(id)
  followCategory(id)
}

const handleUnfollow = async (id) => {
  await apiUnfollow(id)
  unfollowCategory(id)
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

const apiError = ref('')
const successMessage = ref('')
const selectedCat = ref(null)
const isEditing = ref(false)
const isCreateModalOpen = ref(false)
const editForm = ref({ name: '', subject: '', teacher: 'Teacher_default' })
 
 
import { useRouter } from 'vue-router'
const router = useRouter()
 
onMounted(async () => {
  await fetchCategories()
})

const handleCreate = async (data) => {
  try {
    await createCategory(data)
    showSuccess("Catégorie créée avec succès !")
    isCreateModalOpen.value = false // Fermer la modale après succès
    // Le composables recharge automatiquement la liste via fetchCategories
  } catch (err) {
    apiError.value = "Erreur lors de la création."
  }
}

const startEdit = () => {
  editForm.value = { 
    name: selectedCat.value.name, 
    subject: selectedCat.value.subject,
    teacher: selectedCat.value.owner || 'Teacher_default'
  }
  isEditing.value = true
}

const closeModal = () => {
  selectedCat.value = null
  isEditing.value = false
}

const saveEdit = async () => {
  try {
    await updateCategory(selectedCat.value.id, editForm.value)
    
    await fetchCategories() 
    
    showSuccess("Catégorie mise à jour avec succès !")
    closeModal()
  } catch (err) {
    apiError.value = "Erreur lors de la mise à jour."
  }
}

/* const archiveCategory = async () => {
  if (confirm(`Êtes-vous sûr de vouloir archiver la catégorie ${selectedCat.value.name} ?`)) {
    try {
      await deleteCategory(selectedCat.value.id)
      showSuccess("Catégorie archivée avec succès !")
      closeModal()
    } catch (err) {
      alert("Erreur lors de l'archivage")
    }
  }
} */

const showSuccess = (msg) => {
  successMessage.value = msg
  setTimeout(() => {
    successMessage.value = ''
  }, 3000)
}
</script>