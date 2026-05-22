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
          <label class="block text-xs font-medium text-gray-700 mb-1">
            Nom de l'événement <span class="text-red-500">*</span>
          </label>
          <input
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
          <label class="block text-xs font-medium text-gray-700 mb-1">Description</label>
          <textarea
            v-model="form.description"
            placeholder="Décrivez l'événement…"
            rows="3"
            class="w-full px-4 py-2 border border-gray-300 rounded-lg text-sm
                   focus:ring-2 focus:ring-indigo-500 outline-none resize-none"
          />
        </div>

        <!-- Catégorie filtrée par type -->
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
            <option v-for="cat in categories" :key="cat.id" :value="cat.id">
              {{ cat.name }}
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

const form = ref({ name: '', description: '', categoryId: '', startDate: '', endDate: '' })

onMounted(async () => {
  try {
    categories.value = await fetchCategoriesByType(type)
  } catch (e) {
    categoryError.value = 'Impossible de charger les catégories. Vérifiez que vous êtes bien connecté.'
  } finally {
    loadingCategories.value = false
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
    const payload = {
      name:        form.value.name,
      description: form.value.description,
      categoryId:  form.value.categoryId,
      ...(requiresDates.value && {
        startDate: form.value.startDate,
        endDate:   form.value.endDate
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
