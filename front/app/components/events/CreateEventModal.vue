<template>
  <!--
    Modal Nuxt UI wrappé dans notre composant Modal.vue.
    v-model:open est lié à la prop du parent (events/index.vue).
  -->
  <Modal v-model:open="open" title="Créer un événement">

    <!--
      ÉTAPE 1 — Choix du type
      selectedType est null → on affiche les deux cards.
      Au clic, on charge les catégories filtrées puis on passe à l'étape 2.
    -->
    <div v-if="!selectedType" class="space-y-4">
      <p class="text-sm text-gray-500">Choisissez le type d'événement :</p>

      <div class="grid grid-cols-2 gap-4">

        <!-- Card AUDIT -->
        <button
          @click="selectType('AUDIT')"
          class="flex flex-col items-center gap-3 p-6 border-2 rounded-xl transition-all cursor-pointer
                 border-indigo-200 hover:border-indigo-500 hover:bg-indigo-50"
        >
          <i class="fa-solid fa-magnifying-glass text-3xl text-indigo-500"></i>
          <span class="font-bold text-gray-800">Audit</span>
          <span class="text-xs text-gray-400 text-center">Revue de code, CI/CD, qualité…</span>
        </button>

        <!-- Card SOUTENANCE -->
        <button
          @click="selectType('DEFENSE')"
          class="flex flex-col items-center gap-3 p-6 border-2 rounded-xl transition-all cursor-pointer
                 border-emerald-200 hover:border-emerald-500 hover:bg-emerald-50"
        >
          <i class="fa-solid fa-graduation-cap text-3xl text-emerald-500"></i>
          <span class="font-bold text-gray-800">Soutenance</span>
          <span class="text-xs text-gray-400 text-center">PFE, Sprint, Stage…</span>
        </button>

      </div>
    </div>

    <!--
      ÉTAPE 2 — Formulaire
      selectedType est défini → on affiche le formulaire
      avec les catégories filtrées chargées dans selectType().
    -->
    <div v-else class="space-y-4">

      <!-- Badge type + bouton retour étape 1 -->
      <div class="flex items-center gap-3">
        <button
          @click="selectedType = null"
          class="text-gray-400 hover:text-gray-700 transition cursor-pointer"
          title="Retour au choix du type"
        >
          <i class="fa-solid fa-arrow-left"></i>
        </button>
        <UBadge
          :color="selectedType === 'AUDIT' ? 'primary' : 'success'"
          variant="soft" size="lg"
        >
          <i
            :class="selectedType === 'AUDIT' ? 'fa-solid fa-magnifying-glass' : 'fa-solid fa-graduation-cap'"
            class="mr-1"
          ></i>
          {{ selectedType === 'AUDIT' ? 'Audit' : 'Soutenance' }}
        </UBadge>
      </div>

      <!-- Alerte erreur de création -->
      <Alert
        v-if="submitError"
        color="error"
        title="Erreur"
        :description="submitError"
      />

      <form @submit.prevent="handleSubmit" class="space-y-4">

        <!-- Nom -->
        <div>
          <label class="block text-xs font-medium text-gray-700 mb-1">
            Nom de l'événement <span class="text-red-500">*</span>
          </label>
          <input
            v-model="form.name"
            type="text"
            placeholder="Ex : Audit Sprint 3 — Groupe A"
            class="w-full px-4 py-2 border border-gray-300 rounded-lg text-sm
                   focus:ring-2 focus:ring-indigo-500 outline-none"
            required
          />
        </div>

        <!-- Description -->
        <div>
          <label class="block text-xs font-medium text-gray-700 mb-1">Description</label>
          <textarea
            v-model="form.description"
            placeholder="Décrivez l'événement…"
            rows="3"
            class="w-full px-4 py-2 border border-gray-300 rounded-lg text-sm
                   focus:ring-2 focus:ring-indigo-500 outline-none resize-none"
          />
        </div>

        <!--
          Catégorie filtrée par type.
          Alimentée par fetchCategoriesByType() dans selectType().
          USkeleton pendant le chargement.
        -->
        <div>
          <label class="block text-xs font-medium text-gray-700 mb-1">
            Catégorie <span class="text-red-500">*</span>
          </label>
          <USkeleton v-if="loadingCategories" class="h-10 w-full rounded-lg" />
          <select
            v-else
            v-model="form.categoryId"
            class="w-full px-4 py-2 border border-gray-300 rounded-lg text-sm
                   focus:ring-2 focus:ring-indigo-500 outline-none"
            required
          >
            <option disabled value="">— Sélectionnez une catégorie —</option>
            <option v-for="cat in filteredCategories" :key="cat.id" :value="cat.id">
              {{ cat.name }}
            </option>
          </select>
        </div>

        <!-- Actions -->
        <div class="flex justify-end gap-3 pt-2 border-t">
          <button
            type="button"
            @click="resetAndClose"
            class="px-4 py-2 border border-gray-300 text-gray-600 rounded-lg text-sm
                   hover:bg-gray-50 transition cursor-pointer"
          >
            Annuler
          </button>
          <button
            type="submit"
            :disabled="submitting"
            class="px-6 py-2 bg-indigo-600 hover:bg-indigo-700 disabled:opacity-50
                   text-white rounded-lg font-medium text-sm transition cursor-pointer"
          >
            <i v-if="submitting" class="fa-solid fa-spinner animate-spin mr-2"></i>
            <i v-else class="fa-solid fa-plus mr-2"></i>
            Créer l'événement
          </button>
        </div>

      </form>
    </div>

  </Modal>
</template>

<script setup>
/**
 * CreateEventModal — Modal de création d'événement en 2 étapes.
 *
 * Étape 1 : L'utilisateur choisit le type (AUDIT ou DEFENSE).
 *           Au clic, on appelle fetchCategoriesByType() pour charger
 *           uniquement les catégories du bon type.
 *
 * Étape 2 : Le formulaire s'affiche avec :
 *           - nom (obligatoire)
 *           - description (optionnelle)
 *           - catégorie (select filtré, obligatoire)
 *
 * Émet "created" vers le parent (events/index.vue) quand la création réussit.
 * Le parent se charge de fermer le modal et rafraîchir la liste.
 */

const emit = defineEmits(['created'])

// Prop liée au v-model:open du parent
const open = defineModel('open')

const { fetchCategoriesByType, createEvent } = useEvents()

// État interne du workflow
const selectedType       = ref(null)   // null = étape 1, 'AUDIT'|'DEFENSE' = étape 2
const filteredCategories = ref([])     // catégories chargées selon le type choisi
const loadingCategories  = ref(false)
const submitting         = ref(false)
const submitError        = ref(null)

const form = ref({ name: '', description: '', categoryId: '' })

/**
 * Sélectionne le type et charge les catégories correspondantes.
 * Réinitialise categoryId pour éviter un ID d'un type précédent.
 */
const selectType = async (type) => {
  selectedType.value       = type
  form.value.categoryId    = ''
  loadingCategories.value  = true
  try {
    filteredCategories.value = await fetchCategoriesByType(type)
  } finally {
    loadingCategories.value = false
  }
}

/**
 * Soumet le formulaire.
 * Le backend dérive automatiquement eventType depuis la catégorie.
 */
const handleSubmit = async () => {
  submitError.value = null
  submitting.value  = true
  try {
    await createEvent({
      name:        form.value.name,
      description: form.value.description,
      categoryId:  form.value.categoryId
    })
    emit('created') // notifie le parent → fermeture + refresh liste
    resetForm()
  } catch (e) {
    submitError.value = e?.data?.message || 'Une erreur est survenue lors de la création.'
  } finally {
    submitting.value = false
  }
}

/** Ferme le modal et remet tout à zéro. */
const resetAndClose = () => {
  open.value = false
  resetForm()
}

const resetForm = () => {
  selectedType.value       = null
  filteredCategories.value = []
  submitError.value        = null
  form.value               = { name: '', description: '', categoryId: '' }
}
</script>