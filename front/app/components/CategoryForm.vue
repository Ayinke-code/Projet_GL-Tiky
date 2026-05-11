<template>
  <div class="p-2">
    
    <form @submit.prevent="submitForm" class="space-y-4">
      <!-- Grid pour aligner les champs -->
      <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
        <!-- Nom de la matière -->
        <div>
          <label class="block text-xs font-medium text-gray-700 mb-2">Matière *</label>
          <input id="category-name-input" v-model="formData.name" type="text" placeholder="Ex: Mathématiques" 
                 class="w-full px-4 py-2 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-indigo-500 outline-none" required />
        </div>

        <!-- Sujet -->
        <div>
          <label class="block text-xs font-medium text-gray-700 mb-2">Sujet *</label>
          <input id="category-subject-input" v-model="formData.subject" type="text" placeholder="Ex: Algèbre, Analyse..." 
                 class="w-full px-4 py-2 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-indigo-500 outline-none" required />
        </div>
      </div>

      <!-- Professeur -->
      <div>
        <label class="block text-xs font-medium text-gray-700 mb-2">Enseignant responsable *</label>
        <select id="category-teacher-select" v-model="formData.teacher" 
                class="w-full px-4 py-2 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-indigo-500 outline-none bg-white">
          <option v-for="teacher in teachers" :key="teacher.id" :value="String(teacher.id)">
            {{ teacher.name }}
          </option>
        </select>
      </div>

      <!-- Boutons avec style Sidebar -->
      <div class="flex gap-3 pt-2">
        <button id="category-submit-button" type="submit" 
                class="px-6 py-2 bg-indigo-600 hover:bg-indigo-700 text-white rounded-lg font-medium text-sm transition-all duration-200 flex items-center shadow-md hover:shadow-lg">
          <i class="fa-solid fa-plus mr-2"></i> Créer la catégorie
        </button>
        <button type="button" @click="resetForm"
                class="px-6 py-2 bg-slate-100 hover:bg-slate-200 text-slate-700 rounded-lg font-medium text-sm transition-all duration-200">
          Annuler
        </button>
      </div>
    </form>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useCategory } from '~/composables/useCategory' 

// Initialisation du service 
const { createCategory, loading, error } = useCategory()

const teachers = ref([])
const formData = ref({
  name: '',
  subject: '',
  teacher: '' 
})

onMounted(async () => {
  try {
    const response = await fetch('http://localhost:8080/api/users/teachers')
    if (response.ok) {
      teachers.value = await response.json()
      if (teachers.value.length > 0) {
        formData.value.teacher = String(teachers.value[0].id)
      }
    }
  } catch (err) {
    console.error('Erreur chargement profs:', err)
  }
})

const emit = defineEmits(['submit'])
 
const submitForm = () => {
  emit('submit', {
    name: formData.value.name,
    subject: formData.value.subject,
    teacher: formData.value.teacher
  })
  resetForm()
}

const resetForm = () => {
  formData.value = { 
    name: '', 
    subject: '', 
    teacher: teachers.value.length > 0 ? String(teachers.value[0].id) : '' 
  }
}
</script>