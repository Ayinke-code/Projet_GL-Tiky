<template>
  <div class="p-8 h-full bg-gray-50">
    <h1 class="text-2xl font-bold text-gray-800 mb-8">Gestion des Catégories</h1>
    
    <CategoryForm :loading="loading" :error="apiError" @submit="handleCreate" />

    <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
      <CategoryCard 
        v-for="cat in categories" 
        :key="cat.id" 
        :category="cat"
        @click="selectedCat = cat" 
      />
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
            <button v-if="selectedCat.owner === 'teacher-default' || selectedCat.owner === 'Teacher_default'" @click="startEdit" class="flex-1 px-4 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700">Modifier</button>
            <button class="flex-1 px-4 py-2 bg-red-50 text-red-600 rounded-lg opacity-50 cursor-not-allowed" title="Fonctionnalité désactivée">Archiver</button>
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
            <select v-model="editForm.teacher" class="w-full border p-2 rounded">
              <option value="Teacher_default">Teacher_default</option>
              <option value="Prof. Martin">Prof. Martin</option>
              <option value="Dr. Smith">Dr. Smith</option>
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
import { ref, onMounted } from 'vue'
import { useCategory } from '~/composables/useCategory' 

const { categories, loading, createCategory, updateCategory, fetchCategories } = useCategory()

const apiError = ref('')
const successMessage = ref('')
const selectedCat = ref(null)
const isEditing = ref(false)
const editForm = ref({ name: '', subject: '', teacher: 'Teacher_default' })


onMounted(async () => {
  await fetchCategories()
})

const handleCreate = async (data) => {
  try {
    await createCategory(data)
    showSuccess("Catégorie créée avec succès !")
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