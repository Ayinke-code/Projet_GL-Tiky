<template>
  <div class="p-2 space-y-6">

    <!--
      ÉTAPE 1 — Choix du type d'événement
      Affiché tant que selectedType est null.
      Trois cards cliquables : AUDIT ou DEFENSE ou RDV.
      Au clic → selectType() charge les catégories correspondantes.
    -->
    <div v-if="!selectedType">
      <p class="text-sm font-medium text-gray-600 mb-4">
        Quel type d'événement souhaitez-vous créer ?
      </p>
      <div class="grid grid-cols-2 gap-4">

        <button @click="selectType('AUDIT')" class="flex flex-col items-center gap-3 p-6 border-2 border-indigo-200 rounded-xl
                 hover:border-indigo-500 hover:bg-indigo-50 transition-all cursor-pointer">
          <i class="fa-solid fa-magnifying-glass text-3xl text-indigo-500"></i>
          <span class="font-bold text-gray-800">Audit</span>
          <span class="text-xs text-gray-500 text-center">Revue de code, qualité, CI/CD…</span>
        </button>

        <button @click="selectType('DEFENSE')" class="flex flex-col items-center gap-3 p-6 border-2 border-emerald-200 rounded-xl
                 hover:border-emerald-500 hover:bg-emerald-50 transition-all cursor-pointer">
          <i class="fa-solid fa-graduation-cap text-3xl text-emerald-500"></i>
          <span class="font-bold text-gray-800">Soutenance</span>
          <span class="text-xs text-gray-500 text-center">PFE, Sprint, Stage…</span>
        </button>
        <!-- Bouton RDV -->
        <button @click="selectType('RDV')" class="flex flex-col items-center gap-3 p-6 border-2 border-amber-200 rounded-xl
         hover:border-amber-500 hover:bg-amber-50 transition-all cursor-pointer">
          <i class="fa-solid fa-calendar-days text-3xl text-amber-500"></i>
          <span class="font-bold text-gray-800">Rendez-vous</span>
          <span class="text-xs text-gray-500 text-center">Lié à une issue…</span>
        </button>

      </div>
    </div>

    <!--
      ÉTAPE 2 — Formulaire de création
      Affiché une fois le type sélectionné.
      Le select "Catégorie" est alimenté par fetchCategoriesByType()
      appelé dans selectType() → aucun appel API redondant.
    -->

    <div v-else>

      <!-- Indicateur de type + bouton retour vers étape 1 -->
      <div class="flex items-center gap-3 mb-4">
        <button @click="selectedType = null" class="text-gray-400 hover:text-gray-700 transition cursor-pointer">
          <i class="fa-solid fa-arrow-left"></i>
        </button>
        <!-- Remplacez votre UBadge actuel par ceci -->
        <UBadge :color="selectedType === 'AUDIT' ? 'primary' : selectedType === 'DEFENSE' ? 'success' : 'amber'"
          variant="soft" size="lg">
          <i :class="{
            'fa-solid fa-magnifying-glass': selectedType === 'AUDIT',
            'fa-solid fa-graduation-cap': selectedType === 'DEFENSE',
            'fa-solid fa-calendar-days': selectedType === 'RDV'
          }" class="mr-1"></i>
          {{ selectedType === 'AUDIT' ? 'Audit' : (selectedType === 'DEFENSE' ? 'Soutenance' : 'Rendez-vous') }}
        </UBadge>
      </div>

      <form @submit.prevent="submitForm" class="space-y-4">

        <!-- Nom de l'événement -->
        <div>
          <label class="block text-xs font-medium text-gray-700 mb-2">Nom de l'événement *</label>
          <input v-model="formData.name" type="text" placeholder="Ex: Audit Sprint 3 - Groupe A" class="w-full px-4 py-2 border border-gray-300 rounded-lg text-sm
                   focus:ring-2 focus:ring-indigo-500 outline-none" required />
        </div>

        <!-- Description optionnelle -->
        <div>
          <label class="block text-xs font-medium text-gray-700 mb-2">Description</label>
          <textarea v-model="formData.description" placeholder="Décrivez l'événement…" rows="3" class="w-full px-4 py-2 border border-gray-300 rounded-lg text-sm
                   focus:ring-2 focus:ring-indigo-500 outline-none resize-none" />
        </div>

        <!--
          Catégorie filtrée par type.
          filteredCategories est chargé via fetchCategoriesByType dans selectType().
          Le skeleton s'affiche le temps de la réponse API.
        -->
        <div>
          <label class="block text-xs font-medium text-gray-700 mb-2">Catégorie *</label>
          <USkeleton v-if="loadingCategories" class="h-10 w-full" />
          <select v-else v-model="formData.categoryId" class="w-full px-4 py-2 border border-gray-300 rounded-lg text-sm
                   focus:ring-2 focus:ring-indigo-500 outline-none" required>
            <option disabled value="">Sélectionnez une catégorie</option>
            <option v-for="cat in filteredCategories" :key="cat.id" :value="cat.id">
              {{ cat.name }}
            </option>
          </select>
        </div>

        <div class="flex gap-3 pt-2">
          <button type="button" @click="$emit('cancel')" class="px-4 py-2 border border-gray-300 text-gray-600 rounded-lg text-sm
                   hover:bg-gray-50 transition cursor-pointer">
            Annuler
          </button>
          <button type="submit" class="px-6 py-2 bg-indigo-600 hover:bg-indigo-700 text-white rounded-lg
                   font-medium text-sm transition cursor-pointer">
            <i class="fa-solid fa-plus mr-2"></i>Créer l'événement
          </button>
        </div>
      </form>
    </div>
  </div>
</template>

<script setup>
/**
 * Formulaire de création d'événement en 2 étapes :
 *   1. Choix du type (AUDIT / DEFENSE) → déclenche le chargement des catégories filtrées
 *   2. Saisie du nom, description, et choix de la catégorie
 *
 * Émet :
 *   - submit(payload) : { name, description, categoryId } → géré par la page parente
 *   - cancel : ferme le modal sans action
 */
const emit = defineEmits(['submit', 'cancel'])

const { fetchCategoriesByType } = useEvents()

const selectedType = ref(null)          // null = étape 1, 'AUDIT'|'DEFENSE' = étape 2
const filteredCategories = ref([])      // catégories chargées dynamiquement
const loadingCategories = ref(false)

const formData = ref({ name: '', description: '', categoryId: '' })

/**
 * Sélectionne le type et charge immédiatement les catégories correspondantes.
 * Réinitialise le champ catégorie pour éviter un ID invalide d'un type précédent.
 */
const selectType = async (type) => {
  selectedType.value = type
  formData.value.categoryId = ''
  loadingCategories.value = true
  filteredCategories.value = await fetchCategoriesByType(type)
  loadingCategories.value = false
}

/**
 * Émet les données du formulaire vers le parent (events/index.vue)
 * et réinitialise le formulaire pour une prochaine utilisation.
 */
const submitForm = () => {
  emit('submit', { // On inclut le type dans l'objet envoyé au parent
    ...formData.value,
    categoryType: selectedType.value
  })

  selectedType.value = null
  formData.value = { name: '', description: '', categoryId: '' }
}
</script>