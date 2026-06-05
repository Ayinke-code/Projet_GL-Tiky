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
          :color="isAudit ? 'primary' : 'success'"
          variant="soft" size="lg"
        >
          <i :class="isAudit ? 'fa-solid fa-magnifying-glass' : 'fa-solid fa-graduation-cap'" class="mr-1"></i>
          {{ isAudit ? 'Audit' : 'Soutenance' }}
        </UBadge>
        <h1 class="text-2xl font-bold text-gray-800">
          Créer {{ isAudit ? 'un Audit' : 'une Soutenance' }}
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
          <label for="event-name" class="block text-xs font-medium text-gray-700 mb-1">
            Nom de l'événement <span class="text-red-500">*</span>
          </label>
          <input
            id="event-name"
            v-model="form.name"
            type="text"
            :placeholder="isAudit ? 'Ex : Audit Sprint 3 — Groupe A' : 'Ex : Soutenance PFE — Promo 2025'"
            class="w-full px-4 py-2 border border-gray-300 rounded-lg text-sm
                   focus:ring-2 focus:ring-indigo-500 outline-none"
            required
          />
        </div>

        <!-- Description -->
        <div>
          <label for="event-description" class="block text-xs font-medium text-gray-700 mb-1">Description</label>
          <textarea
            id="event-description"
            v-model="form.description"
            placeholder="Décrivez l'événement…"
            rows="3"
            class="w-full px-4 py-2 border border-gray-300 rounded-lg text-sm
                   focus:ring-2 focus:ring-indigo-500 outline-none resize-none"
          />
        </div>

        <!-- Catégorie filtrée par type -->
        <div>
          <label for="event-category" class="block text-xs font-medium text-gray-700 mb-1">
            Catégorie <span class="text-red-500">*</span>
          </label>
          <USkeleton v-if="loadingCategories" class="h-10 w-full rounded-lg" />
          <select
            v-else
            id="event-category"
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

        <!-- Date unique + heures début/fin -->
        <div v-if="requiresDates" class="space-y-3">

          <!-- Date -->
          <div>
            <label for="event-date" class="block text-xs font-medium text-gray-700 mb-1">
              Date <span class="text-red-500">*</span>
            </label>
            <input
              id="event-date"
              v-model="form.eventDate"
              type="date"
              :min="todayDate"
              class="w-full px-4 py-2 border border-gray-300 rounded-lg text-sm
                     focus:ring-2 focus:ring-indigo-500 outline-none"
              required
            />
          </div>

          <!-- Heures côte à côte -->
          <div class="grid grid-cols-2 gap-4">
            <div>
              <label for="event-start-time" class="block text-xs font-medium text-gray-700 mb-1">
                Heure de début <span class="text-red-500">*</span>
              </label>
              <input
                id="event-start-time"
                v-model="form.startTime"
                type="time"
                :min="minStartTime"
                class="w-full px-4 py-2 border border-gray-300 rounded-lg text-sm
                       focus:ring-2 focus:ring-indigo-500 outline-none"
                required
              />
            </div>
            <div>
              <label for="event-end-time" class="block text-xs font-medium text-gray-700 mb-1">
                Heure de fin <span class="text-red-500">*</span>
              </label>
              <input
                id="event-end-time"
                v-model="form.endTime"
                type="time"
                class="w-full px-4 py-2 border rounded-lg text-sm outline-none
                       focus:ring-2 focus:ring-indigo-500"
                :class="dateError ? 'border-red-400' : 'border-gray-300'"
                required
              />
              <p v-if="dateError" class="text-xs text-red-500 mt-1">{{ dateError }}</p>
            </div>
          </div>

        </div>

        <!-- Groupes projet (AUDIT et DEFENSE uniquement) -->
        <div v-if="requiresDates">
          <label for="group-search" class="block text-xs font-medium text-gray-700 mb-1">
            Groupes projet
            <span class="text-gray-400 font-normal ml-1">({{ form.projectGroupIds.length }} sélectionné(s))</span>
          </label>

          <!-- Barre de recherche -->
          <input
            id="group-search"
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
import { ref, onMounted, computed } from 'vue'

const route = useRoute()
const router = useRouter()
const { fetchCategoriesByType, createEvent } = useEvents()

const type = route.params.type // 'AUDIT' ou 'DEFENSE'
const isAudit = computed(() => type === 'AUDIT')
const requiresDates = computed(() => type === 'AUDIT' || type === 'DEFENSE')

const categories        = ref([])
const loadingCategories = ref(true)
const categoryError     = ref(null)
const submitting        = ref(false)
const submitError       = ref(null)
const dateError         = ref(null)

const projectGroups      = ref([])
const loadingProjectGroups = ref(false)
const groupSearch        = ref('')

const form = ref({ name: '', description: '', categoryId: '', eventDate: '', startTime: '', endTime: '', projectGroupIds: [] })

const todayDate = computed(() => {
  const d = new Date()
  const pad = (n) => n.toString().padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
})

const minStartTime = computed(() => {
  if (!form.value.eventDate) return ''
  const d = new Date()
  const pad = (n) => n.toString().padStart(2, '0')
  const todayStr = `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
  if (form.value.eventDate !== todayStr) return ''
  // Au moins 30 minutes dans le futur
  d.setMinutes(d.getMinutes() + 30)
  return `${pad(d.getHours())}:${pad(d.getMinutes())}`
})

const filteredProjectGroups = computed(() => {
  const q = groupSearch.value.toLowerCase().trim()
  if (!q) return projectGroups.value
  return projectGroups.value.filter(g =>
    g.name.toLowerCase().includes(q) || g.categoryName?.toLowerCase().includes(q)
  )
})

onMounted(async () => {
  try {
    categories.value = await fetchCategoriesByType(type)
  } catch (e) {
    categoryError.value = 'Impossible de charger les catégories. Vérifiez que vous êtes bien connecté.'
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
    if (!form.value.eventDate || !form.value.startTime || !form.value.endTime) {
      dateError.value = 'La date et les heures de début et de fin sont obligatoires.'
      return
    }
    const startDateTime = new Date(`${form.value.eventDate}T${form.value.startTime}`)
    const minDateTime = new Date()
    minDateTime.setMinutes(minDateTime.getMinutes() + 30)
    if (startDateTime < minDateTime) {
      dateError.value = "La date de début doit être au moins 30 minutes dans le futur."
      return
    }
    if (form.value.endTime <= form.value.startTime) {
      dateError.value = "L'heure de fin doit être postérieure à l'heure de début."
      return
    }
  }

  submitting.value = true
  try {
    const startDate = `${form.value.eventDate}T${form.value.startTime}`
    const endDate   = `${form.value.eventDate}T${form.value.endTime}`
    const payload = {
      name:        form.value.name,
      description: form.value.description,
      categoryId:  form.value.categoryId,
      ...(requiresDates.value && {
        startDate,
        endDate,
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
