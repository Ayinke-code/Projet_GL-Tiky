<template>
  <div class="p-2">

    <form @submit.prevent="submitForm" class="space-y-4">
      <!-- Grid pour aligner les champs -->
      <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
        <!-- Nom de la matière -->
        <div>
          <label class="block text-xs font-medium text-gray-700 mb-2">Matière *</label>
          <input id="category-name-input" v-model="formData.name" type="text" placeholder="Ex: Mathématiques"
            class="w-full px-4 py-2 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-indigo-500 outline-none"
            required />
        </div>

        <!-- Sujet -->
        <div>
          <label class="block text-xs font-medium text-gray-700 mb-2">Sujet *</label>
          <input id="category-subject-input" v-model="formData.subject" type="text"
            placeholder="Ex: Algèbre, Analyse..."
            class="w-full px-4 py-2 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-indigo-500 outline-none"
            required />
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

      <!-- Followers Initiaux -->
      <div v-if="otherTeachers.length > 0" class="mt-4">
        <label class="block text-xs font-medium text-gray-700 mb-2">Ajouter des abonnés (Followers)</label>
        <div class="max-h-32 overflow-y-auto border border-gray-200 rounded-lg p-3 space-y-2 bg-white shadow-inner">
          <div v-for="teacher in otherTeachers" :key="teacher.id"
            class="flex items-center gap-3 p-1 hover:bg-gray-50 rounded transition-colors">
            <input type="checkbox" :id="'teacher-' + teacher.id" :value="String(teacher.id)"
              v-model="formData.initialFollowers"
              class="w-4 h-4 text-indigo-600 border-gray-300 rounded focus:ring-indigo-500 cursor-pointer" />
            <label :for="'teacher-' + teacher.id" class="text-sm text-gray-600 cursor-pointer select-none">
              {{ teacher.name }}
            </label>
          </div>
        </div>
        <p class="text-[10px] text-gray-400 mt-1 italic">Note: Le propriétaire est abonné automatiquement.</p>
      </div>
      <div v-else class="mt-4 p-3 bg-gray-50 rounded-lg border border-dashed border-gray-200 text-center">
        <p class="text-xs text-gray-500">Aucun autre enseignant disponible pour l'abonnement.</p>
      </div>

      <div v-if="fetchError || error" class="mt-4 p-3 bg-red-50 text-red-600 rounded-lg border border-red-200 text-xs">
        <span v-if="fetchError">Erreur chargement professeurs: {{ fetchError }}</span>
        <span v-if="error">Erreur: {{ error }}</span>
      </div>

      <!-- Boutons avec style Sidebar -->
      <div class="flex gap-3 pt-2">
        <button id="category-submit-button" type="submit"
          class="px-6 py-2 bg-indigo-600 hover:bg-indigo-700 text-white rounded-lg font-medium text-sm transition-all duration-200 flex items-center shadow-md hover:shadow-lg">
          <i :class="isEdit ? 'fa-solid fa-save' : 'fa-solid fa-plus'" class="mr-2"></i> 
          {{ isEdit ? 'Sauvegarder' : 'Créer la catégorie' }}
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
import { ref, onMounted, computed } from 'vue'


const emit = defineEmits(['submit', 'cancel'])

const fetchError = ref(null)
const teachers = ref([])
const props = defineProps({
  loading: Boolean,
  error: String,
  initialData: Object, // Ajouté pour l'édition
  isEdit: Boolean      // Ajouté pour changer le texte du bouton
})

// Initialisation intelligente des données
const formData = ref({
  name: props.initialData?.name || '',
  subject: props.initialData?.subject || '',
  teacher: props.initialData?.owner || '', // On utilise owner car c'est le nom dans ton objet Category
  initialFollowers: []
})

// Filtrer les profs pour ne pas proposer le propriétaire dans les followers
const otherTeachers = computed(() => {
  return teachers.value.filter(t => String(t.id) !== String(formData.value.teacher))
})

onMounted(async () => {
  try {
    const response = await fetch('http://localhost:8080/api/users/teachers')
    if (response.ok) {
      teachers.value = await response.json()
      console.log('Teachers chargés:', teachers.value.length, teachers.value)
      if (teachers.value.length > 0 && !formData.value.teacher) {
        formData.value.teacher = String(teachers.value[0].id)
      }
    } else {
      fetchError.value = `Erreur API: ${response.status}`
      console.error('Erreur API teachers:', response.status)
    }
  } catch (err) {
    fetchError.value = err.message
    console.error('Erreur chargement profs:', err)
  }
})

const submitForm = () => {
  emit('submit', {
    name: formData.value.name,
    subject: formData.value.subject,
    teacher: formData.value.teacher,
    initialFollowers: formData.value.initialFollowers
  })
  resetForm()
}

const resetForm = () => {
  formData.value = {
    name: '',
    subject: '',
    teacher: teachers.value.length > 0 ? String(teachers.value[0].id) : '',
    initialFollowers: []
  }
  emit('cancel')
}
</script>