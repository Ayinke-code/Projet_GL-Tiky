<template>
  <div class="bg-white rounded-lg shadow-sm border border-gray-200 p-6 mb-8">
    <h2 class="text-sm font-medium uppercase tracking-wider text-blue-800 mb-6">Créer une nouvelle catégorie</h2>
    
    <form @submit.prevent="submitForm" class="space-y-4">
      <!-- Grid pour aligner les champs -->
      <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
        <!-- Nom de la matière -->
        <div>
          <label class="block text-xs font-medium text-gray-700 mb-2">Matière *</label>
          <input v-model="formData.name" type="text" placeholder="Ex: Mathématiques" 
                 class="w-full px-4 py-2 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-indigo-500 outline-none" required />
        </div>

        <!-- Sujet -->
        <div>
          <label class="block text-xs font-medium text-gray-700 mb-2">Sujet *</label>
          <input v-model="formData.subject" type="text" placeholder="Ex: Algèbre, Analyse..." 
                 class="w-full px-4 py-2 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-indigo-500 outline-none" required />
        </div>
      </div>

      <!-- Professeur -->
      <div>
        <label class="block text-xs font-medium text-gray-700 mb-2">Enseignant responsable *</label>
        <select v-model="formData.teacher" 
                class="w-full px-4 py-2 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-indigo-500 outline-none bg-white">
          <option value="Teacher_default">Teacher_default</option>
          <option value="Dr. Smith">Dr. Smith</option>
          <option value="Prof. Martin">Prof. Martin</option>
        </select>
      </div>

      <!-- Boutons avec style Sidebar -->
      <div class="flex gap-3 pt-2">
        <button type="submit" 
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
const emit = defineEmits(['submit'])

const formData = ref({
  name: '',
  subject: '',
  teacher: 'Teacher_default'
})

const submitForm = () => {
  if (formData.value.name && formData.value.subject) {
    emit('submit', { ...formData.value })
  }
}

const resetForm = () => {
  formData.value = { name: '', subject: '', teacher: 'Teacher_default' }
}
</script>