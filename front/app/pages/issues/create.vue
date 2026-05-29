<template>
  <div class="p-8">
    <UCard class="max-w-2xl">
      <template #header>
        <div class="flex items-center gap-2">
          <UButton to="/issues" variant="ghost" icon="i-lucide-arrow-left" size="sm" />
          <h1 class="text-2xl font-bold">Créer un ticket</h1>
        </div>
      </template>

      <form @submit.prevent="handleCreateIssue" class="space-y-4">
        <!-- Titre -->
        <div class="space-y-2">
          <label class="block text-sm font-medium text-gray-700">Titre *</label>
          <input v-model="form.title" type="text" placeholder="Entrez le titre du ticket"
                 class="w-full px-4 py-3 text-lg border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-transparent outline-none transition"
                 required />
        </div>

        <!-- Description -->
        <div class="space-y-2">
          <label class="block text-sm font-medium text-gray-700">Description *</label>
          <UTextarea v-model="form.description" placeholder="Décrivez le ticket en détail..." :rows="6"
                     class="w-full" required />
        </div>

        <!-- Catégorie -->
        <div class="space-y-2">
          <label class="block text-sm font-medium text-gray-700">Catégorie *</label>
          <select v-model="form.categoryId"
                  class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-transparent outline-none transition"
                  required>
            <option disabled value="">Sélectionnez une catégorie</option>
            <option v-for="cat in categories" :key="cat.id" :value="cat.id">
              {{ cat.name }}
            </option>
          </select>
        </div>

        <!-- Option de mise en attente -->
        <div class="pt-2">
          <UCheckbox v-model="form.isPending" label="Marquer ce ticket incomplet, vous pourrez le compléter plus tard" />
        </div>

        <!-- Erreur -->
        <UAlert v-if="error" color="error" class="mt-4" @close="error = null">
          <template #title>Erreur</template>
          {{ error }}
        </UAlert>

        <!-- Boutons -->
        <div class="flex gap-2 justify-end pt-4 border-t">
          <UButton to="/issues" variant="outline" icon="i-lucide-x">
            Annuler
          </UButton>
          <UButton type="submit" icon="i-lucide-check" :loading="submitting" class="cursor-pointer">
            Créer
          </UButton>
        </div>
      </form>
    </UCard>
  </div>
</template>

<script setup lang="js">
/**
 * Page création d'issue
 * - charge catégories
 * - valide formulaire
 * - crée une issue
 */

const { ready, user } = useAuth()
const { createIssue } = useIssues()
const { fetchCategoriesByType, categories } = useCategories()

/**
 * État UI
 */
const submitting = ref(false)
const loadingCategories = ref(true)
const error = ref(null)

/**
 * Formulaire local
 */
const form = reactive({
  title: '',
  description: '',
  categoryId: null,
  isPending: false
})

/**
 * Création d'une issue
 */
const handleCreateIssue = async () => {
  error.value = null

  /**
   * Validation minimale UI (UX)
   */
  if (!form.title.trim()) {
    error.value = 'Le titre est obligatoire'
    return
  }

  if (!form.description.trim()) {
    error.value = 'La description est obligatoire'
    return
  }

  if (!form.categoryId) {
    error.value = 'Veuillez sélectionner une catégorie'
    return
  }

  submitting.value = true
  try {
    // Envoi du DTO
    const result = await createIssue({
      title: form.title,
      description: form.description,
      categoryId: form.categoryId,
      pendingCase: form.isPending
    })

    console.log('valeur formulaire' + form.isPending)
    console.log('valeur requete' + result.pendingCase)



    if (!result) return

    await navigateTo('/issues')

  } catch (err) {
    error.value = err?.data?.message || err?.message || 'Erreur création ticket'
  } finally {
    submitting.value = false
  }
}

/**
 * Chargement initial des catégories
 */
onMounted(async () => {
  if (ready && !user?.value?.id) {
    await navigateTo('/login')
    return
  }

  try {
    await fetchCategoriesByType('ISSUE')

    if (!categories.value.length) {
      error.value = 'Aucune catégorie disponible'
    }
  } catch (err) {
    error.value = 'Erreur chargement catégories'
  } finally {
    loadingCategories.value = false
  }
})
</script>
