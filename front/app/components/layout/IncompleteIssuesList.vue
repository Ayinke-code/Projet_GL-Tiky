<template>
  <div class="space-y-6">
    <!-- En-tête de la section -->
    <div class="flex items-center justify-between">
      <h2 class="text-lg font-semibold text-gray-900">Demandes à compléter</h2>
      <UBadge v-if="issues.length > 0" color="orange" variant="soft" size="sm">
        {{ issues.length }} action(s) requise(s)
      </UBadge>
    </div>

    <!-- État de chargement -->
    <div v-if="loading" class="flex justify-center py-8">
      <UIcon name="i-heroicons-arrow-path" class="animate-spin text-gray-400 text-3xl" />
    </div>

    <!-- État vide : Aucune issue incomplète -->
    <div v-else-if="issues.length === 0" class="text-center py-8 text-gray-500 italic bg-gray-50/50 rounded-xl border border-dashed border-gray-200">
      Vous n'avez aucune demande nécessitant des corrections.
    </div>

    <!-- Liste des issues incomplètes -->
    <div v-else class="space-y-4">
      <UCard
        v-for="issue in issues"
        :key="issue.id"
        class="border-orange-200 ring-orange-500/10 shadow-sm"
      >
        <template #header>
          <div class="flex items-center justify-between">
            <h3 class="font-bold text-gray-900">{{ issue.title }}</h3>
            <span class="text-xs text-gray-500">Catégorie : {{ issue.categoryName }}</span>
          </div>
        </template>

        <div class="space-y-4">
          <!-- Bloc de retour du professeur (Raison + Commentaire) -->
          <UAlert
            icon="i-heroicons-exclamation-triangle"
            color="orange"
            variant="soft"
            :title="`Refusé pour : ${issue.rejectReason}`"
            :description="issue.rejectComment"
          />

          <!-- Aperçu de la description actuelle -->
          <div>
            <p class="text-xs font-semibold text-gray-400 uppercase tracking-wide mb-1">Votre description initiale</p>
            <p class="text-sm text-gray-600 line-clamp-3 bg-gray-50 p-3 rounded border border-gray-100">
              {{ issue.description }}
            </p>
          </div>
        </div>

        <template #footer>
          <div class="flex justify-end">
            <UButton
              label="Corriger la demande"
              icon="i-heroicons-pencil-square"
              color="primary"
              @click="openEditModal(issue)"
            />
          </div>
        </template>
      </UCard>
    </div>

    <!-- Modale de correction -->
    <UModal v-model:open="isModalOpen" title="Mettre à jour la demande">
      <template #body>
        <div v-if="editingIssue" class="space-y-4">
          <!-- Rappel de ce qu'il faut corriger -->
          <div class="p-3 bg-orange-50 text-orange-800 rounded text-sm border border-orange-100 mb-4">
            <span class="font-semibold block mb-1">À corriger : {{ editingIssue.rejectReason }}</span>
            <span class="text-orange-700">{{ editingIssue.rejectComment }}</span>
          </div>

          <!-- Formulaire -->
          <UFormField label="Titre" required>
            <UInput v-model="formData.title" placeholder="Titre de l'issue" class="w-full" />
          </UFormField>

          <UFormField label="Description" required>
            <UTextarea v-model="formData.description" :rows="6" placeholder="Détaillez votre problème..." class="w-full" />
          </UFormField>

          <!-- Correction de la catégorie -->
          <UFormField label="Catégorie" required>
            <select v-model="formData.categoryId"
                    class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-transparent outline-none transition"
                    required>
              <option disabled value="">Sélectionnez une catégorie</option>
              <option v-for="cat in categories" :key="cat.id" :value="cat.id">
                {{ cat.name }}
              </option>
            </select>
          </UFormField>
        </div>
      </template>

      <template #footer>
        <div class="flex justify-end gap-3">
          <UButton
            color="neutral"
            variant="ghost"
            label="Annuler"
            @click="closeEditModal"
          />
          <UButton
            color="primary"
            label="Enregistrer et renvoyer"
            icon="i-heroicons-paper-airplane"
            :loading="submitting"
            :disabled="!isFormValid"
            @click="submitCorrections"
          />
        </div>
      </template>
    </UModal>
  </div>
</template>

<script setup>
const emit = defineEmits(['has-issues'])

const props = defineProps({
  userId: {
    type: [Number, String],
    required: true
  }
})

// Dépendances API
const { getIncompleteIssues, updateIssueWhenIncomplete } = useAssignation()
const { fetchCategories, categories } = useCategories() // Ajout de useCategories

// États
const issues = ref([])
const loading = ref(true)

// Gestion de la modale et de l'édition
const isModalOpen = ref(false)
const editingIssue = ref(null)
const submitting = ref(false)

const formData = ref({
  title: '',
  description: '',
  categoryId: null
})

// Validation simple
const isFormValid = computed(() => {
  return formData.value.title.trim() !== '' &&
    formData.value.description.trim() !== '' &&
    formData.value.categoryId !== null
})

// Récupération des données
const fetchIssues = async () => {
  loading.value = true
  try {
    const data = await getIncompleteIssues(props.userId)
    issues.value = Array.isArray(data) ? data : []

    emit('has-issues', issues.value.length > 0)
  } catch (error) {
    console.error("Erreur lors de la récupération des issues incomplètes :", error)
    emit('has-issues', false)
  } finally {
    loading.value = false
  }
}

// Ouvrir la modale et pré-remplir les champs
const openEditModal = (issue) => {

  console.log("Données de l'issue reçues du backend :", issue)

  editingIssue.value = issue
  formData.value = {
    title: issue.title,
    description: issue.description,
    categoryId: issue.categoryId
  }
  isModalOpen.value = true
}

// Fermer et réinitialiser
const closeEditModal = () => {
  isModalOpen.value = false
  setTimeout(() => {
    editingIssue.value = null
    formData.value = { title: '', description: '', categoryId: null }
  }, 200)
}

// Soumettre les changements
const submitCorrections = async () => {
  if (!isFormValid.value || !editingIssue.value) return

  submitting.value = true
  try {
    await updateIssueWhenIncomplete(editingIssue.value.id, formData.value)

    closeEditModal()
    await fetchIssues()
  } catch (error) {
    console.error("Erreur lors de la mise à jour de l'issue :", error)
  } finally {
    submitting.value = false
  }
}

// Chargement des données au montage
onMounted(async () => {
  if (props.userId) {
    // On charge les catégories pour le menu déroulant
    await fetchCategories()
    // On charge les issues incomplètes
    await fetchIssues()
  }
})
</script>
