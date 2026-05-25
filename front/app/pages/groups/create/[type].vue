<template>
  <div class="p-8 min-h-screen bg-gray-50">

    <!-- En-tête -->
    <div class="flex items-center gap-4 mb-8">
      <button
        @click="router.push('/groups')"
        class="text-gray-400 hover:text-gray-700 transition cursor-pointer"
        title="Retour aux groupes"
      >
        <i class="fa-solid fa-arrow-left text-lg"></i>
      </button>
      <div class="flex items-center gap-3">
        <UBadge :color="isOption ? 'warning' : 'success'" variant="soft" size="lg">
          <i :class="isOption ? 'fa-solid fa-code-branch' : 'fa-solid fa-rocket'" class="mr-1"></i>
          {{ isOption ? 'Option' : 'Projet' }}
        </UBadge>
        <h1 class="text-2xl font-bold text-gray-800">
          Créer un groupe {{ isOption ? 'd\'option' : 'de projet' }}
        </h1>
      </div>
    </div>

    <!-- Formulaire -->
    <div class="max-w-xl bg-white rounded-2xl shadow-sm border border-gray-100 p-6">

      <Alert
        v-if="categoryError"
        color="error"
        title="Impossible de charger les catégories"
        :description="categoryError"
        class="mb-4"
      />

      <Alert
        v-if="submitError"
        color="error"
        title="Erreur"
        :description="submitError"
        class="mb-4"
      />

      <form @submit.prevent="handleSubmit" class="space-y-5">

        <!-- Nom du groupe -->
        <div>
          <label class="block text-xs font-medium text-gray-700 mb-1">
            Nom du groupe <span class="text-red-500">*</span>
          </label>
          <input
            v-model="form.name"
            type="text"
            :placeholder="isOption ? 'Ex : LD — Promo 2026' : 'Ex : Groupe Horizon A'"
            class="w-full px-4 py-2 border border-gray-300 rounded-lg text-sm
                   focus:ring-2 focus:ring-indigo-500 outline-none"
            required
          />
        </div>

        <!-- Catégorie filtrée par type -->
        <div>
          <div class="flex items-center justify-between mb-1">
            <label class="block text-xs font-medium text-gray-700">
              {{ isOption ? 'Option' : 'Projet' }} <span class="text-red-500">*</span>
            </label>
            <button
              type="button"
              @click="useCustomCategory = !useCustomCategory; form.categoryId = ''; form.customCategoryName = ''"
              class="text-xs text-indigo-600 hover:underline cursor-pointer"
            >
              {{ useCustomCategory ? '← Choisir dans la liste' : '+ Saisir un nom personnalisé' }}
            </button>
          </div>

          <!-- Saisie libre -->
          <input
            v-if="useCustomCategory"
            v-model="form.customCategoryName"
            type="text"
            :placeholder="isOption ? 'Ex : Génie Logiciel' : 'Ex : Projet Phoenix'"
            class="w-full px-4 py-2 border border-indigo-300 rounded-lg text-sm
                   focus:ring-2 focus:ring-indigo-500 outline-none"
            required
          />

          <!-- Liste déroulante -->
          <template v-else>
            <USkeleton v-if="loadingCategories" class="h-10 w-full rounded-lg" />
            <select
              v-else
              v-model="form.categoryId"
              class="w-full px-4 py-2 border border-gray-300 rounded-lg text-sm
                     focus:ring-2 focus:ring-indigo-500 outline-none"
              required
            >
              <option disabled value="">— Sélectionnez {{ isOption ? 'une option' : 'un projet' }} —</option>
              <option v-for="cat in categories" :key="cat.id" :value="cat.id">
                {{ cat.name }}
              </option>
            </select>
          </template>
        </div>

        <!-- Groupe d'option parent (PROJECT uniquement) -->
        <div v-if="!isOption">
          <label class="block text-xs font-medium text-gray-700 mb-1">
            Groupe d'option rattaché <span class="text-red-500">*</span>
          </label>
          <USkeleton v-if="loadingOptionGroups" class="h-10 w-full rounded-lg" />
          <select
            v-else
            v-model="form.parentGroupId"
            class="w-full px-4 py-2 border border-gray-300 rounded-lg text-sm
                   focus:ring-2 focus:ring-indigo-500 outline-none"
            required
          >
            <option disabled value="">— Sélectionnez un groupe d'option —</option>
            <option v-for="og in optionGroups" :key="og.id" :value="og.id">
              {{ og.categoryName ? `${og.name} · ${og.categoryName}` : og.name }}
            </option>
          </select>
        </div>

        <!-- Sélection des étudiants -->
        <div>
          <label class="block text-xs font-medium text-gray-700 mb-1">
            Étudiants
            <span class="text-gray-400 font-normal ml-1">({{ form.memberIds.length }} sélectionné(s))</span>
            <span :class="form.memberIds.length < minMembers ? 'text-red-500' : 'text-emerald-600'" class="font-semibold ml-1">
              · minimum {{ minMembers }}
            </span>
          </label>

          <!-- Barre de recherche -->
          <input
            v-model="studentSearch"
            type="text"
            placeholder="Rechercher un étudiant…"
            class="w-full px-4 py-2 border border-gray-300 rounded-lg text-sm mb-2
                   focus:ring-2 focus:ring-indigo-500 outline-none"
          />

          <USkeleton v-if="loadingStudents" class="h-40 w-full rounded-lg" />
          <div
            v-else
            class="max-h-52 overflow-y-auto border border-gray-200 rounded-lg divide-y divide-gray-100"
          >
            <label
              v-for="student in filteredStudents"
              :key="student.id"
              class="flex items-center gap-3 px-4 py-2.5 cursor-pointer hover:bg-indigo-50 transition-colors"
            >
              <input
                type="checkbox"
                :value="student.id"
                v-model="form.memberIds"
                class="accent-indigo-600"
              />
              <span class="text-sm text-gray-800">{{ student.name }}</span>
            </label>
            <div v-if="filteredStudents.length === 0" class="px-4 py-4 text-sm text-gray-400 text-center">
              Aucun étudiant trouvé
            </div>
          </div>
        </div>

        <!-- Actions -->
        <div class="flex justify-end gap-3 pt-2 border-t">
          <button
            type="button"
            @click="router.push('/groups')"
            class="px-4 py-2 border border-gray-300 text-gray-600 rounded-lg text-sm
                   hover:bg-gray-50 transition cursor-pointer"
          >
            Annuler
          </button>
          <button
            type="submit"
            :disabled="submitting || form.memberIds.length < minMembers || (!isOption && !form.parentGroupId)"
            class="px-6 py-2 bg-indigo-600 hover:bg-indigo-700 disabled:opacity-50
                   text-white rounded-lg font-medium text-sm transition cursor-pointer"
          >
            <i v-if="submitting" class="fa-solid fa-spinner animate-spin mr-2"></i>
            <i v-else class="fa-solid fa-plus mr-2"></i>
            Créer le groupe
          </button>
        </div>

      </form>
    </div>
  </div>
</template>

<script setup>
definePageMeta({ ssr: false })

import Alert from '~/components/ui/Alert.vue'

const route = useRoute()
const router = useRouter()
const api = useApi()

const type = route.params.type // 'OPTION' ou 'PROJECT'
const isOption = computed(() => type === 'OPTION')
const minMembers = computed(() => type === 'OPTION' ? 24 : 2)

const categories = ref([])
const loadingCategories = ref(true)
const categoryError = ref(null)

const loadingStudents = ref(true)
const studentSearch = ref('')
const allStudents = ref([])

const submitting = ref(false)
const submitError = ref(null)
const useCustomCategory = ref(false)

const optionGroups = ref([])
const loadingOptionGroups = ref(false)

const form = reactive({
  name: '',
  categoryId: '',
  customCategoryName: '',
  parentGroupId: '',
  memberIds: [],
})

const filteredStudents = computed(() => {
  const q = studentSearch.value.toLowerCase().trim()
  if (!q) return allStudents.value
  return allStudents.value.filter(s => s.name.toLowerCase().includes(q))
})

onMounted(async () => {
  try {
    categories.value = await api(`/categories/type/${type}`)
  } catch {
    categoryError.value = 'Impossible de charger les catégories.'
  } finally {
    loadingCategories.value = false
  }

  if (type === 'PROJECT') {
    loadingOptionGroups.value = true
    try {
      optionGroups.value = await api('/groups/type/OPTION')
    } catch {
      // liste vide en cas d'erreur
    } finally {
      loadingOptionGroups.value = false
    }
  }

  try {
    allStudents.value = await api('/users/byRoles', {
      method: 'POST',
      body: ['STUDENT'],
    })
  } catch {
    // liste vide en cas d'erreur
  } finally {
    loadingStudents.value = false
  }
})

async function handleSubmit() {
  submitting.value = true
  submitError.value = null
  try {
    await api('/groups', {
      method: 'POST',
      body: {
        name: form.name,
        type: type,
        categoryId: useCustomCategory.value ? null : form.categoryId,
        customCategoryName: useCustomCategory.value ? form.customCategoryName : null,
        parentGroupId: (!isOption.value && form.parentGroupId) ? form.parentGroupId : null,
        memberIds: form.memberIds,
      },
    })
    router.push('/groups')
  } catch (err) {
    submitError.value = err?.data?.error ?? 'Une erreur est survenue lors de la création du groupe.'
  } finally {
    submitting.value = false
  }
}
</script>
