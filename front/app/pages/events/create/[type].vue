<template>
  <div class="p-8 min-h-screen bg-gray-50">

    <!-- En-tête -->
    <div class="flex items-center gap-4 mb-8">
      <button @click="router.push('/events')" class="text-gray-400 hover:text-gray-700 transition cursor-pointer"
        title="Retour aux événements">
        <i class="fa-solid fa-arrow-left text-lg"></i>
      </button>
      <div class="flex items-center gap-3">
        <UBadge :color="eventTypeConfig.badgeColor" variant="soft" size="lg">
          <i :class="eventTypeConfig.icon" class="mr-1"></i>
          {{ eventTypeConfig.label }}
        </UBadge>
        <h1 class="text-2xl font-bold text-gray-800">
          Créer {{ eventTypeConfig.title }}
        </h1>
      </div>
    </div>

    <!-- Formulaire -->
    <div class="max-w-xl bg-white rounded-2xl shadow-sm border border-gray-100 p-6">

      <!-- Erreur de chargement des catégories -->
      <Alert v-if="categoryError" color="error" title="Impossible de charger les catégories"
        :description="categoryError" class="mb-4" />

      <!-- Erreur de création -->
      <Alert v-if="submitError" color="error" title="Erreur" :description="submitError" class="mb-4" />

      <form @submit.prevent="handleSubmit" class="space-y-5">

        <!-- Nom -->
        <div>
          <label class="block text-xs font-medium text-gray-700 mb-1">
            Nom de l'événement <span class="text-red-500">*</span>
          </label>
          <input v-model="form.name" type="text" :placeholder="eventTypeConfig.placeholder" class="w-full px-4 py-2 border border-gray-300 rounded-lg text-sm
                   focus:ring-2 focus:ring-indigo-500 outline-none" required />
        </div>

        <!-- Description -->
        <div>
          <label class="block text-xs font-medium text-gray-700 mb-1">Description</label>
          <textarea v-model="form.description" placeholder="Décrivez l'événement…" rows="3" class="w-full px-4 py-2 border border-gray-300 rounded-lg text-sm
                   focus:ring-2 focus:ring-indigo-500 outline-none resize-none" />
        </div>

        <!-- Catégorie filtrée par type (Masqué pour RDV) -->
        <div v-if="!isRdv">
          <label class="block text-xs font-medium text-gray-700 mb-1">
            Catégorie <span class="text-red-500">*</span>
          </label>
          <USkeleton v-if="loadingCategories" class="h-10 w-full rounded-lg" />
          <select v-else v-model="form.categoryId" class="w-full px-4 py-2 border border-gray-300 rounded-lg text-sm
                   focus:ring-2 focus:ring-indigo-500 outline-none" required>
            <option disabled value="">— Sélectionnez une catégorie —</option>
            <option v-for="cat in categories" :key="cat.id" :value="cat.id">
              {{ cat.name }}
            </option>
          </select>
        </div>

        <!-- Issue (Spécifique au RDV) -->
        <div v-if="isRdv">
          <label class="block text-xs font-medium text-gray-700 mb-1">
            Issue associée <span class="text-red-500">*</span>
          </label>
          <USkeleton v-if="loadingCategories" class="h-10 w-full rounded-lg" />
          <select v-else v-model="form.idIssue" class="w-full px-4 py-2 border border-gray-300 rounded-lg text-sm
                   focus:ring-2 focus:ring-indigo-500 outline-none" required>
            <option disabled value="">— Sélectionnez une issue —</option>
            <option v-for="issue in issues" :key="issue.id" :value="issue.id">
              {{ issue.title }}
            </option>
          </select>
        </div>

        <!-- Professeur (Spécifique au RDV) -->
        <div v-if="isRdv">
          <label class="block text-xs font-medium text-gray-700 mb-1">
            Professeur <span class="text-red-500">*</span>
          </label>
          <USkeleton v-if="loadingCategories" class="h-10 w-full rounded-lg" />
          <select v-else v-model="form.idTeacher" class="w-full px-4 py-2 border border-gray-300 rounded-lg text-sm
                   focus:ring-2 focus:ring-indigo-500 outline-none" required>
            <option disabled value="">— Sélectionnez un professeur —</option>
            <option v-for="teacher in teachers" :key="teacher.id" :value="teacher.id">
              {{ teacher.name }}
            </option>
          </select>
        </div>

        <!-- Plage de dates — obligatoire pour les Audits et Soutenances -->
        <div v-if="requiresDates" class="grid grid-cols-2 gap-4">
          <div>
            <label class="block text-xs font-medium text-gray-700 mb-1">
              Date de début <span class="text-red-500">*</span>
            </label>
            <input v-model="form.startDate" type="datetime-local" class="w-full px-4 py-2 border border-gray-300 rounded-lg text-sm
                     focus:ring-2 focus:ring-indigo-500 outline-none" required />
          </div>
          <div>
            <label class="block text-xs font-medium text-gray-700 mb-1">
              Date de fin <span class="text-red-500">*</span>
            </label>
            <input v-model="form.endDate" type="datetime-local" :min="form.startDate" :max="endDateMax" class="w-full px-4 py-2 border rounded-lg text-sm outline-none
                     focus:ring-2 focus:ring-indigo-500" :class="dateError ? 'border-red-400' : 'border-gray-300'"
              required />
            <p v-if="dateError" class="text-xs text-red-500 mt-1">
              {{ dateError }}
            </p>
          </div>
        </div>

        <!-- Actions -->
        <div class="flex justify-end gap-3 pt-2 border-t">
          <button type="button" @click="router.push('/events')" class="px-4 py-2 border border-gray-300 text-gray-600 rounded-lg text-sm
                   hover:bg-gray-50 transition cursor-pointer">
            Annuler
          </button>
          <button type="submit" :disabled="submitting" class="px-6 py-2 bg-indigo-600 hover:bg-indigo-700 disabled:opacity-50
                   text-white rounded-lg font-medium text-sm transition cursor-pointer">
            <i v-if="submitting" class="fa-solid fa-spinner animate-spin mr-2"></i>
            <i v-else class="fa-solid fa-plus mr-2"></i>
            Créer l'événement
          </button>
        </div>

      </form>
    </div>
  </div>
</template>

<script setup>
definePageMeta({
  ssr: false
})

import Alert from '~/components/ui/Alert.vue'
import { ref, onMounted, computed, watch } from 'vue'

const route = useRoute()
const router = useRouter()
const { fetchCategoriesByType, createEvent } = useEvents()
const { issues, fetchIssues } = useIssues()
const { users: teachers, fetchUserByRoles } = useUsers()

const type = route.params.type // 'AUDIT', 'DEFENSE' ou 'RDV'
const isRdv = computed(() => type === 'RDV')

const eventTypeConfig = computed(() => {
  switch (type) {
    case 'AUDIT':
      return {
        badgeColor: 'primary',
        icon: 'fa-solid fa-magnifying-glass',
        label: 'Audit',
        title: 'un Audit',
        placeholder: 'Ex : Audit Sprint 3 — Groupe A'
      }
    case 'RDV':
      return {
        badgeColor: 'amber',
        icon: 'fa-solid fa-calendar-days',
        label: 'Rendez-vous',
        title: 'un Rendez-vous',
        placeholder: 'Ex : Point de synchronisation Issue #42'
      }
    case 'DEFENSE':
    default:
      return {
        badgeColor: 'success',
        icon: 'fa-solid fa-graduation-cap',
        label: 'Soutenance',
        title: 'une Soutenance',
        placeholder: 'Ex : Soutenance PFE — Promo 2025'
      }
  }
})

const requiresDates = computed(() => ['AUDIT', 'DEFENSE', 'RDV'].includes(type))

const categories = ref([])
const loadingCategories = ref(true)
const categoryError = ref(null)
const submitting = ref(false)
const submitError = ref(null)
const dateError = ref(null)

// Initialisation de l'état du formulaire
const form = ref({ name: '', description: '', categoryId: '', startDate: '', endDate: '', idTeacher: '', idIssue: '' })

/**
 * Calcule dynamiquement la date de fin maximale autorisée.
 * Force la sélection de la date de fin sur le même jour que la date de début.
 * @returns {string} La date maximale au format ISO pour l'input type datetime-local
 */
const endDateMax = computed(() => {
  if (!form.value.startDate) return ''
  return form.value.startDate.split('T')[0] + 'T23:59'
})

/**
 * Surveille les changements de la date de début pour réajuster automatiquement
 * la date de fin, afin de s'assurer qu'elles restent sur la même journée.
 */
watch(() => form.value.startDate, (newStart) => {
  if (!newStart) return
  const datePart = newStart.split('T')[0]
  const timePart = form.value.endDate?.split('T')[1] ?? ''
  form.value.endDate = timePart ? `${datePart}T${timePart}` : ''
})

/**
 * Lifecycle hook déclenché au montage du composant.
 * Gère le chargement initial des données requises selon le type d'événement :
 * - Pour un RDV : récupère la liste des Issues et la liste des professeurs.
 * - Pour les autres (Audit/Soutenance) : récupère les catégories correspondantes au type.
 */
onMounted(async () => {
  try {
    if (isRdv.value) {
      await fetchIssues()
      await fetchUserByRoles(['TEACHER'])
    } else {
      categories.value = await fetchCategoriesByType(type)
    }
  } catch (e) {
    categoryError.value = isRdv.value ? 'Impossible de charger les données.' : 'Impossible de charger les catégories. Vérifiez que vous êtes bien connecté.'
  } finally {
    loadingCategories.value = false
  }
})

/**
 * Gère la soumission du formulaire de création d'événement.
 *
 * 1. Valide les champs obligatoires (notamment les dates si le type le requiert).
 * 2. Prépare le payload dynamiquement en fonction du type (ex: ajoute idTeacher et idIssue pour RDV,
 *    et extrait la categoryId de l'issue sélectionnée).
 * 3. Envoie la requête via le composable useEvents et redirige vers la liste des événements en cas de succès.
 */
// Ajoutez defineEmits dans vos imports si ce n'est pas déjà fait
const emit = defineEmits(['update:open', 'created'])

const handleSubmit = async () => {
   console.log("Tentative de soumission du formulaire...");
  submitError.value = null
  dateError.value = null

  if (requiresDates.value) {
    if (!form.value.startDate || !form.value.endDate) {
      dateError.value = 'Les dates de début et de fin sont obligatoires.'
      return
    }
    if (new Date(form.value.endDate) <= new Date(form.value.startDate)) {
      dateError.value = 'La date de fin doit être postérieure à la date de début.'
      return
    }
  }

  submitting.value = true
  try {
    let finalCategoryId = form.value.categoryId
    if (isRdv.value) {
      const selectedIssue = issues.value.find(i => i.id === form.value.idIssue)
      if (selectedIssue) {
        finalCategoryId = selectedIssue.category?.id || selectedIssue.categoryId
      }
    }

    const payload = {
      name: form.value.name,
      description: form.value.description,
      categoryId: finalCategoryId,
      ...(isRdv.value && {
        idTeacher: form.value.idTeacher,
        idIssue: form.value.idIssue
      }),
      categoryType: type,
      ...(requiresDates.value && {
        startDate: form.value.startDate,
        endDate: form.value.endDate
      })
    }
    await createEvent(payload)
    emit('created')

    emit('update:open', false)
    console.log("Payload envoyé :", JSON.stringify(payload, null, 2));

    router.push('/events') 
    
    // Toast de succès si tu as Nuxt UI
    useToast().add({ title: 'Événement créé !', color: 'green' })

  } catch (e) {
    submitError.value = e?.data?.message || 'Une erreur est survenue lors de la création.'
  } finally {
    submitting.value = false
  }
}
</script>
