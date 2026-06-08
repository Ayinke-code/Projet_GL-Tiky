<template>
  <div class="space-y-6">
    <!-- En-tête de la section -->
    <div class="flex items-center justify-between">
      <h2 class="text-lg font-semibold text-gray-900">
        {{ $t('incomplete_issues.title') }}
      </h2>
      <UBadge v-if="issues.length > 0" color="orange" variant="soft" size="sm">
        {{ $t('incomplete_issues.actions_required', { count: issues.length }) }}
      </UBadge>
    </div>

    <!-- État de chargement -->
    <div v-if="loading" class="flex justify-center py-8">
      <UIcon name="i-heroicons-arrow-path" class="animate-spin text-gray-400 text-3xl" />
    </div>

    <!-- État vide : Aucune issue incomplète -->
    <div v-else-if="issues.length === 0" class="text-center py-8 text-gray-500 italic bg-gray-50/50 rounded-xl border border-dashed border-gray-200">
      {{ $t('incomplete_issues.empty_state') }}
    </div>

    <!-- Liste des issues incomplètes (Limitée) -->
    <div v-else class="space-y-4">
      <UCard
        v-for="issue in displayedIssues"
        :key="issue.id"
        class="shadow-sm"
        :class="issue.rejectReason ? 'border-orange-200 ring-orange-500/10' : 'border-blue-200 ring-blue-500/10'"
      >
        <template #header>
          <div class="flex items-center justify-between">
            <h3 class="font-bold text-gray-900">
              {{ issue.title }}
            </h3>
            <span class="text-xs text-gray-500">{{ $t('incomplete_issues.category') }} : {{ issue.categoryName }}</span>
          </div>
        </template>

        <div class="space-y-4">
          <!-- Cas 1 : Retour du superviseur (Raison + Commentaire présents) -->
          <UAlert
            v-if="issue.rejectReason"
            icon="i-heroicons-exclamation-triangle"
            color="orange"
            variant="soft"
            :title="`${$t('incomplete_issues.refused_for')} : ${issue.rejectReason}`"
            :description="issue.rejectComment"
          />

          <!-- Cas 2 : Mis en attente à la création (Pas de raison de refus) -->
          <UAlert
            v-else
            icon="i-heroicons-clock"
            color="blue"
            variant="soft"
            :title="$t('incomplete_issues.draft')"
          />

          <!-- Aperçu de la description actuelle -->
          <div>
            <p class="text-xs font-semibold text-gray-400 uppercase tracking-wide mb-1">
              {{ $t('incomplete_issues.current_description') }}
            </p>
            <p class="text-sm text-gray-600 line-clamp-3 bg-gray-50 p-3 rounded border border-gray-100">
              {{ issue.description }}
            </p>
          </div>
        </div>

        <template #footer>
          <div class="flex justify-end">
            <UButton
              :label="issue.rejectReason ? $t('incomplete_issues.correct_request') : $t('incomplete_issues.finalize_request')"
              icon="i-heroicons-pencil-square"
              :color="issue.rejectReason ? 'primary' : 'blue'"
              @click="openEditModal(issue)"
            />
          </div>
        </template>
      </UCard>

      <!-- Indicateur d'éléments supplémentaires -->
      <div v-if="remainingIssuesCount > 0" class="text-center pt-2">
        <p class="text-sm text-gray-500 italic bg-gray-50 py-2 rounded-lg border border-gray-100">
          {{ $t('incomplete_issues.remaining_issues', { count: remainingIssuesCount }) }}
        </p>
      </div>
    </div>

    <UModal
      v-model:open="isModalOpen"
      :title="editingIssue?.rejectReason ? $t('incomplete_issues.modal_update_title') : $t('incomplete_issues.modal_finalize_title')"
    >
      <template #body>
        <div v-if="editingIssue" class="space-y-4">
          <!-- Cas 1 : Rappel de ce qu'il faut corriger (Superviseur) -->
          <div v-if="editingIssue.rejectReason" class="p-3 bg-orange-50 text-orange-800 rounded text-sm border border-orange-100 mb-4">
            <span class="font-semibold block mb-1">
              {{ $t('incomplete_issues.to_correct') }} : {{ editingIssue.rejectReason }}
            </span>
            <span class="text-orange-700">
              {{ editingIssue.rejectComment }}
            </span>
          </div>

          <!-- Cas 2 : Message informatif de finalisation (Créateur) -->
          <div v-else class="p-3 bg-blue-50 text-blue-800 rounded text-sm border border-blue-100 mb-4">
            <span class="font-semibold block mb-1">{{ $t('incomplete_issues.incomplete_ticket') }}</span>
            <span class="text-blue-700">{{ $t('incomplete_issues.finalize_info') }}</span>
          </div>

          <!-- Formulaire -->
          <UFormField :label="$t('incomplete_issues.form.title')" required>
            <UInput v-model="formData.title" :placeholder="$t('incomplete_issues.form.title_placeholder')" class="w-full" />
          </UFormField>

          <UFormField :label="$t('incomplete_issues.form.description')" required>
            <UTextarea v-model="formData.description" :rows="6" :placeholder="$t('incomplete_issues.form.description_placeholder')" class="w-full" />
          </UFormField>

          <!-- Correction de la catégorie -->
          <UFormField :label="$t('incomplete_issues.form.category')" required>
            <select v-model="formData.categoryId"
                    class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-transparent outline-none transition"
                    required>
              <option disabled value="">
                {{ $t('incomplete_issues.form.category_placeholder') }}
              </option>
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
            :label="$t('incomplete_issues.form.cancel')"
            @click="closeEditModal"
          />
          <UButton
            color="primary"
            :label="submitting ? $t('incomplete_issues.form.updating') : $t('incomplete_issues.form.submit')"
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
import { computed } from 'vue'

const emit = defineEmits(['has-issues', 'issue-updated'])

const props = defineProps({
  userId: {
    type: [Number, String],
    required: true
  }
})

// Dépendances API
const { getIncompleteIssues, updateIssueWhenIncomplete } = useAssignation()
const { fetchCategories, categories } = useCategories()

// États
const issues = ref([])
const loading = ref(true)

// ---  LOGIQUE DE LIMITATION ---
const MAX_DISPLAYED = 3

// Calcule les 3 premières issues à afficher
const displayedIssues = computed(() => {
  return issues.value.slice(0, MAX_DISPLAYED)
})

// Calcule combien d'issues ne sont pas affichées
const remainingIssuesCount = computed(() => {
  return Math.max(0, issues.value.length - MAX_DISPLAYED)
})

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
  return formData.value.title.trim() !== ''
    && formData.value.description.trim() !== ''
    && formData.value.categoryId !== null
})

// Récupération des données
const fetchIssues = async () => {
  loading.value = true
  try {
    const data = await getIncompleteIssues(props.userId)
    issues.value = Array.isArray(data) ? data : []

    emit('has-issues', issues.value.length > 0)
  } catch (error) {
    console.error('Erreur lors de la récupération des issues incomplètes :', error)
    emit('has-issues', false)
  } finally {
    loading.value = false
  }
}

// Ouvrir la modale et pré-remplir les champs
const openEditModal = (issue) => {
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
    await fetchIssues() // Rechargera la liste, et si on était à >3, le 4ème élément prendra la place du 3ème !

    emit('issue-updated')
  } catch (error) {
    console.error('Erreur lors de la mise à jour de l\'issue :', error)
  } finally {
    submitting.value = false
  }
}

const onNotificationReceived = async (e) => {
  if (e.detail?.type === 'INCOMPLETE_ISSUE') {
    await fetchIssues()
  }
}

// Chargement des données au montage
onMounted(async () => {
  window.addEventListener('notification:received', onNotificationReceived)
  if (props.userId) {
    try {
      await fetchCategories()
    } catch (e) {
      console.error('Erreur lors du chargement des catégories', e)
    }
    await fetchIssues()
  }
})

onUnmounted(() => {
  window.removeEventListener('notification:received', onNotificationReceived)
})
</script>
