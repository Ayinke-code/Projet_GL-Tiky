<template>
  <div class="p-8 min-h-screen bg-gray-50">

    <!-- En-tête -->
    <div class="flex items-center gap-4 mb-8">
      <button
        @click="router.push('/events')"
        class="text-gray-400 hover:text-gray-700 transition cursor-pointer"
        title="Retour aux événements"
      >
        <i class="fa-solid fa-arrow-left text-lg"></i>
      </button>
      <div class="flex items-center gap-3">
        <UBadge
          :color="eventTypeConfig.badgeColor"
          variant="soft" size="lg"
        >
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
      <Alert
        v-if="categoryError"
        color="error"
        title="Impossible de charger les catégories"
        :description="categoryError"
        class="mb-4"
      />

      <!-- Erreur de création -->
      <Alert
        v-if="submitError"
        color="error"
        title="Erreur"
        :description="submitError"
        class="mb-4"
      />

      <form @submit.prevent="handleSubmit" class="space-y-5">

        <!-- Nom -->
        <div>
          <label class="block text-xs font-medium text-gray-700 mb-1">
            Nom de l'événement <span class="text-red-500">*</span>
          </label>
          <input
            v-model="form.name"
            type="text"
            :placeholder="eventTypeConfig.placeholder"
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

        <!-- Catégorie filtrée par type (Masqué pour RDV) -->
        <div v-if="!isRdv">
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
          <select
            v-else
            v-model="form.idIssue"
            class="w-full px-4 py-2 border border-gray-300 rounded-lg text-sm
                   focus:ring-2 focus:ring-indigo-500 outline-none"
            required
          >
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
          <select
            v-else
            v-model="form.idTeacher"
            class="w-full px-4 py-2 border border-gray-300 rounded-lg text-sm
                   focus:ring-2 focus:ring-indigo-500 outline-none"
            required
          >
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
            <input
              v-model="form.startDate"
              type="datetime-local"
              class="w-full px-4 py-2 border border-gray-300 rounded-lg text-sm
                     focus:ring-2 focus:ring-indigo-500 outline-none"
              required
            />
          </div>
          <div>
            <label class="block text-xs font-medium text-gray-700 mb-1">
              Date de fin <span class="text-red-500">*</span>
            </label>
            <input
              v-model="form.endDate"
              type="datetime-local"
              :min="form.startDate"
              :max="endDateMax"
              class="w-full px-4 py-2 border rounded-lg text-sm outline-none
                     focus:ring-2 focus:ring-indigo-500"
              :class="dateError ? 'border-red-400' : 'border-gray-300'"
              required
            />
            <p v-if="dateError" class="text-xs text-red-500 mt-1">
              {{ dateError }}
            </p>
          </div>
        </div>

        <!-- Groupes projet (AUDIT et DEFENSE uniquement) -->
        <div v-if="requiresDates">
          <label class="block text-xs font-medium text-gray-700 mb-1">
            Groupes projet
            <span class="text-gray-400 font-normal ml-1">({{ form.projectGroupIds.length }} sélectionné(s))</span>
          </label>

          <!-- Barre de recherche -->
          <input
            v-model="groupSearch"
            type="text"
            placeholder="Rechercher un groupe projet…"
            class="w-full px-4 py-2 border border-gray-300 rounded-lg text-sm mb-2
                   focus:ring-2 focus:ring-indigo-500 outline-none"
          />

          <USkeleton v-if="loadingProjectGroups" class="h-32 w-full rounded-lg" />
          <div
            v-else
            class="max-h-44 overflow-y-auto border border-gray-200 rounded-lg divide-y divide-gray-100"
          >
            <label
              v-for="g in filteredProjectGroups"
              :key="g.id"
              class="flex items-center gap-3 px-4 py-2.5 cursor-pointer hover:bg-indigo-50 transition-colors"
            >
              <input
                type="checkbox"
                :value="g.id"
                v-model="form.projectGroupIds"
                class="accent-indigo-600"
              />
              <span class="text-sm text-gray-800">
                {{ g.name }}
                <span v-if="g.categoryName" class="text-xs text-gray-400 ml-1">· {{ g.categoryName }}</span>
              </span>
            </label>
            <div v-if="filteredProjectGroups.length === 0" class="px-4 py-4 text-sm text-gray-400 text-center">
              Aucun groupe projet trouvé
            </div>
          </div>
        </div>

        <!-- Actions -->
        <div class="flex justify-end gap-3 pt-2 border-t">
          <button
            type="button"
            @click="router.push('/events')"
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
  </div>
</template>

<script setup>
definePageMeta({ ssr: false })

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

const categories        = ref([])
const loadingCategories = ref(true)
const categoryError     = ref(null)
const submitting        = ref(false)
const submitError       = ref(null)
const dateError         = ref(null)

const projectGroups      = ref([])
const loadingProjectGroups = ref(false)
const groupSearch        = ref('')

const form = ref({ name: '', description: '', categoryId: '', startDate: '', endDate: '', projectGroupIds: [] })

const filteredProjectGroups = computed(() => {
  const q = groupSearch.value.toLowerCase().trim()
  if (!q) return projectGroups.value
  return projectGroups.value.filter(g =>
    g.name.toLowerCase().includes(q) || g.categoryName?.toLowerCase().includes(q)
  )
})
const form = ref({ name: '', description: '', categoryId: '', startDate: '', endDate: '', idTeacher: '', idIssue: '' })

// Force la date de fin sur le même jour que la date de début
const endDateMax = computed(() => {
  if (!form.value.startDate) return ''
  return form.value.startDate.split('T')[0] + 'T23:59'
})

watch(() => form.value.startDate, (newStart) => {
  if (!newStart) return
  const datePart = newStart.split('T')[0]
  const timePart = form.value.endDate?.split('T')[1] ?? ''
  form.value.endDate = timePart ? `${datePart}T${timePart}` : ''
})

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

  if (requiresDates.value) {
    loadingProjectGroups.value = true
    try {
      const api = useApi()
      projectGroups.value = await api('/groups/type/PROJECT')
    } catch {
      // liste vide en cas d'erreur
    } finally {
      loadingProjectGroups.value = false
    }
  }
})

const handleSubmit = async () => {
  submitError.value = null
  dateError.value   = null

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
      name:        form.value.name,
      description: form.value.description,
      categoryId:  finalCategoryId,
      ...(isRdv.value && {
        idTeacher: form.value.idTeacher,
        idIssue: form.value.idIssue
      }),
      ...(requiresDates.value && {
        startDate: form.value.startDate,
        endDate:   form.value.endDate,
        projectGroupIds: form.value.projectGroupIds,
      })
    }
    await createEvent(payload)
    router.push('/events')
  } catch (e) {
    submitError.value = e?.data?.message || 'Une erreur est survenue lors de la création.'
  } finally {
    submitting.value = false
  }
}
</script>
