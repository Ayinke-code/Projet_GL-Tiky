<template>
  <div class="p-2">
    <form @submit.prevent="submitForm" class="space-y-4">

      <div class="grid grid-cols-1 md:grid-cols-2 gap-4">

        <div>
          <label class="block text-xs font-medium neutral mb-2">
            Matière *
          </label>

          <input
            v-model="formData.name"
            id="categorie-name-teacher"
            type="text"
            placeholder="Ex: Mathématiques"
            class="w-full px-4 py-2 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-indigo-500 outline-none"
            required
          />
        </div>

        <div>
          <label class="block text-xs font-medium neutral mb-2">
            Sujet *
          </label>

          <input
            v-model="formData.subject"
            id="categorie-subject-teacher"
            type="text"
            placeholder="Ex: Algèbre"
            class="w-full px-4 py-2 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-indigo-500 outline-none"
            required
          />
        </div>

      </div>

      <div class="flex gap-3 pt-2">

        <button
          id="validate-categorie"
          type="submit"
          class="px-6 py-2 bg-indigo-600 hover:bg-indigo-700 text-white rounded-lg font-medium text-sm transition-all cursor-pointer"
        >
          <i class="fa-solid fa-plus mr-2"></i>
          Créer la catégorie
        </button>

      </div>

    </form>
  </div>
</template>

<script setup>
import { ref } from 'vue'

const emit = defineEmits(['submit'])

const formData = ref({
  name: '',
  subject: ''
})

const resetForm = () => {
  formData.value.name = ''
  formData.value.subject = ''
}

const submitForm = () => {
  emit('submit', {
    name: formData.value.name,
    subject: formData.value.subject
  })
  resetForm()
}
</script>
