<template>
  <div class="p-8">
    <UCard class="max-w-2xl">
      <template #header>
        <div class="flex items-center gap-2">
          <UButton
            to="/issues"
            variant="ghost"
            icon="i-lucide-arrow-left"
            size="sm"
          />
          <h1 class="text-2xl font-bold">Créer un ticket</h1>
        </div>
      </template>

      <form @submit.prevent="handleCreateIssue" class="space-y-4">
        <!-- Titre -->
        <div class="space-y-2">
          <label class="block text-sm font-medium text-gray-700">Titre *</label>
          <input
            v-model="form.titre"
            type="text"
            placeholder="Entrez le titre du ticket"
            class="w-full px-4 py-3 text-lg border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-transparent outline-none transition"
            required
          />
        </div>

        <!-- Description -->
        <div class="space-y-2">
          <label class="block text-sm font-medium text-gray-700">Description *</label>
          <UTextarea
            v-model="form.description"
            placeholder="Décrivez le ticket en détail..."
            :rows="6"
            maxlength="4000"
            class="w-full"
            required
          />
          <div class="text-right text-xs text-gray-400">
            {{ form.description.length }}/4000
          </div>
        </div>

        <!-- Catégorie -->
        <div class="space-y-2">
          <label class="block text-sm font-medium text-gray-700">Catégorie *</label>
          <select
            v-model="form.categorieId"
            class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-transparent outline-none transition"
            required
          >
            <option disabled value="">Sélectionnez une catégorie</option>
            <option v-for="cat in categories" :key="cat.id" :value="cat.id">
              {{ cat.name }}
            </option>
          </select>
        </div>

        <!-- Erreur -->
        <UAlert
          v-if="error"
          color="error"
          class="mt-4"
          @close="error = null"
        >
          <template #title>Erreur</template>
          {{ error }}
        </UAlert>

        <!-- Boutons -->
        <div class="flex gap-2 justify-end pt-4 border-t">
          <UButton
            to="/issues"
            variant="outline"
            icon="i-lucide-x"
          >
            Annuler
          </UButton>
          <UButton
            type="submit"
            icon="i-lucide-check"
            :loading="submitting"
          >
            Créer
          </UButton>
        </div>
      </form>
    </UCard>
  </div>
</template>

<script setup lang="ts">
definePageMeta({
  layout: 'default'
})

const router = useRouter()
const { isAuthenticated, addIssueRole } = useAuth()
const { createIssue, getCategories } = useApi()
const submitting = ref(false)
const loadingCategories = ref(true)
const error = ref<string | null>(null)
const categories = ref<any[]>([])

const form = reactive({
  titre: '',
  description: '',
  categorieId: null
})

const handleCreateIssue = async () => {
  if (!form.titre.trim()) {
    error.value = 'Le titre est obligatoire'
    return
  }

  if (!form.description.trim()) {
    error.value = 'La description est obligatoire'
    return
  }

  if (form.description.length > 4000) {
    error.value = 'La description ne doit pas dépasser 4000 caractères'
    return
  }

  if (!form.categorieId) {
    error.value = 'Veuillez sélectionner une catégorie'
    return
  }

  submitting.value = true
  error.value = null

  try {
    await createIssue({
      titre: form.titre,
      description: form.description,
      categorieId: form.categorieId
    })

    addIssueRole('CREATOR')

    await router.push('/issues')
  } catch (err: any) {
    console.error('Create error:', err)
    error.value = err.data?.message || err.message || 'Erreur lors de la création de l\'issue'
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  if (!isAuthenticated.value) {
    await router.push('/login')
    return
  }

  try {
    const data = await getCategories()
    categories.value = (data || []) as any[]
    if (categories.value.length === 0) {
      error.value = 'Aucune catégorie disponible. Connecté? Backend lancé?'
    }
  } catch (err: any) {
    console.error('Load categories error:', err)
    error.value = 'Impossible de charger les catégories. Backend inaccessible?'
  } finally {
    loadingCategories.value = false
  }
})
</script>
