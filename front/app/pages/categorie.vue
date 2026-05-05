<template>
  <div class="p-8 h-full bg-gray-50">
    <h1 class="text-2xl font-bold text-gray-800 mb-8">Gestion des Catégories</h1>
    
    <CategoryForm :loading="loading" :error="apiError" @submit="handleCreate" />

    <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
      <CategoryCard 
        v-for="cat in categories" 
        :key="cat.idCat" 
        :category="cat"
        @click="selectedCat = cat" 
      />
    </div>

    <!-- MODALE UNIQUE (Détail / Édition / Archivage) -->
    <div v-if="selectedCat" class="fixed inset-0 bg-black/40 flex items-center justify-center z-50 p-4" @click.self="closeModal">
      <div class="bg-white rounded-xl shadow-2xl w-full max-w-lg p-6 animate-in fade-in zoom-in duration-200">
        
        <!-- MODE VUE -->
        <div v-if="!isEditing">
          <div class="flex justify-between items-start mb-6">
            <div>
              <h2 class="text-2xl font-bold text-gray-900">{{ selectedCat.nom }}</h2>
              <p class="text-indigo-600 font-medium">{{ selectedCat.description }}</p>
            </div>
            <button @click="closeModal" class="text-gray-400 hover:text-gray-600"><i class="fa-solid fa-times text-xl"></i></button>
          </div>

          <!-- Liste des Issues -->
          <div class="mb-6">
            <h4 class="text-xs font-bold text-gray-400 uppercase mb-3">État</h4>
            <div class="space-y-2">
              <div class="p-3 bg-gray-50 rounded border border-gray-100 text-sm">{{ selectedCat.estValide ? '✓ Validée' : '✗ Non validée' }}</div>
            </div>
          </div>

          <!-- Actions -->
          <div class="flex gap-3 pt-6 border-t">
            <button @click="startEdit" class="flex-1 px-4 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700">Modifier</button>
          </div>
        </div>

        <!-- MODE ÉDITION -->
        <div v-else>
          <div class="flex justify-between items-start mb-4">
            <h3 class="text-lg font-bold">Modifier la catégorie</h3>
            <button @click="isEditing = false" class="text-gray-400 hover:text-gray-600"><i class="fa-solid fa-times text-xl"></i></button>
          </div>
          <div class="space-y-4">
            <input v-model="editForm.nom" class="w-full border p-2 rounded" placeholder="Nom de la catégorie" />
            <textarea v-model="editForm.description" class="w-full border p-2 rounded" placeholder="Description" rows="4"></textarea>
            <label class="flex items-center gap-2">
              <input v-model="editForm.estValide" type="checkbox" class="rounded" />
              <span class="text-sm text-gray-700">Validée</span>
            </label>
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

<script setup lang="ts">
import { ref, onMounted } from 'vue' 
import { useApi } from '~/composables/useApi'

definePageMeta({
  layout: 'default'
})

const { getCategories, loading, error } = useApi()
const categories = ref([])
const apiError = ref('')
const successMessage = ref('')
const selectedCat = ref(null)
const isEditing = ref(false)
const editForm = ref({ nom: '', description: '', estValide: true })

onMounted(async () => {
  try {
    const data = await getCategories()
    categories.value = data || []
    if (categories.value.length === 0) {
      apiError.value = 'Aucune catégorie disponible. Connecté? Backend lancé?'
    }
  } catch (err) {
    apiError.value = 'Impossible de charger les catégories. Backend inaccessible?'
    console.error('Load categories error:', err)
  }
})

const handleCreate = async (data) => {
  try {
    // TODO: Implémenter createCategory dans useApi
    showSuccess("Catégorie créée avec succès !")
    const updatedCategories = await getCategories()
    categories.value = updatedCategories
  } catch (err) {
    apiError.value = "Erreur lors de la création de la catégorie."
  }
}

const closeModal = () => {
  selectedCat.value = null
  isEditing.value = false
}

const startEdit = () => {
  editForm.value = { nom: selectedCat.value.nom, description: selectedCat.value.description, estValide: selectedCat.value.estValide }
  isEditing.value = true
}

const saveEdit = () => {
  const index = categories.value.findIndex(c => c.idCat === selectedCat.value.idCat)
  if (index !== -1) {
    categories.value[index] = { ...categories.value[index], ...editForm.value }
    showSuccess("Catégorie mise à jour avec succès !")
  }
  closeModal()
}

const showSuccess = (msg) => {
  successMessage.value = msg
  setTimeout(() => {
    successMessage.value = ''
  }, 3000)
}
</script>