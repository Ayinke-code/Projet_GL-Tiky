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
          {{ editId ? 'Modifier' : 'Créer' }} un groupe {{ isOption ? 'd\'option' : 'de projet' }}
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

        <!-- Nom auto-généré (OPTION) — aperçu read-only -->
        <div v-if="isOption">
          <label class="block text-xs font-medium text-gray-700 mb-1 flex items-center gap-2">
            Nom du groupe
            <span class="inline-flex items-center gap-1 text-indigo-500 font-medium text-xs">
              <i class="fa-solid fa-wand-magic-sparkles"></i>
              Auto-généré
            </span>
          </label>
          <div
            class="w-full px-4 py-2 border rounded-lg text-sm"
            :class="form.name
              ? 'border-indigo-200 bg-indigo-50 text-indigo-800 font-medium'
              : 'border-gray-200 bg-gray-50 text-gray-400 italic'"
          >
            {{ form.name || 'Sélectionnez une option et une promotion…' }}
          </div>
        </div>

        <!-- Catégorie filtrée par type -->
        <div>
          <div class="flex items-center justify-between mb-1">
            <label class="block text-xs font-medium text-gray-700">
              {{ isOption ? 'Option' : 'Nom du groupe' }} <span class="text-red-500">*</span>
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
            :placeholder="isOption ? 'Ex : Génie Logiciel' : 'Ex : Horizon Alpha'"
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
              <option disabled value="">— Sélectionnez {{ isOption ? 'une option' : 'un nom de groupe' }} —</option>
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

          <!-- Enseignants assignés au groupe d'option sélectionné -->
          <div v-if="parentGroupTeachers.length > 0" class="mt-2 flex flex-wrap gap-1 items-center">
            <span class="text-xs text-gray-500 mr-1">Enseignants :</span>
            <span
              v-for="teacher in parentGroupTeachers"
              :key="teacher.id"
              class="bg-violet-50 text-violet-700 text-xs px-2 py-0.5 rounded-full"
            >
              {{ teacher.name }}
            </span>
          </div>
          <p v-else-if="form.parentGroupId" class="mt-1 text-xs text-gray-400 italic">
            Aucun enseignant assigné à ce groupe d'option.
          </p>
        </div>

        <!-- Promotion associée (OPTION uniquement) -->
        <div v-if="isOption && promotions.length > 0">
          <label class="block text-xs font-medium text-gray-700 mb-1">
            Promotion associée
            <span class="text-gray-400 font-normal ml-1">(optionnel)</span>
          </label>
          <select
            v-model="form.promotionId"
            class="w-full px-4 py-2 border border-gray-300 rounded-lg text-sm
                   focus:ring-2 focus:ring-indigo-500 outline-none"
          >
            <option value="">— Aucune promotion —</option>
            <option v-for="p in promotions" :key="p.id" :value="p.id">
              {{ p.name }}<template v-if="p.year"> · {{ p.year }}</template>
            </option>
          </select>
        </div>

        <!-- Filtrer les étudiants par promotion (PROJECT uniquement) -->
        <div v-if="false"><!-- filtre promo masqué : la liste est déjà filtrée par le groupe d'option --></div>

        <!-- Sélection des étudiants -->
        <div>
          <label class="block text-xs font-medium text-gray-700 mb-1">
            Étudiants
            <span class="text-gray-400 font-normal ml-1">({{ form.memberIds.length }} sélectionné(s))</span>
            <span :class="form.memberIds.length < minMembers ? 'text-red-500' : 'text-emerald-600'" class="font-semibold ml-1">
              · minimum {{ minMembers }}
            </span>
          </label>

          <!-- Hint PROJECT sans option sélectionnée -->
          <div
            v-if="!isOption && !form.parentGroupId"
            class="flex items-center gap-2 bg-amber-50 border border-amber-200 rounded-lg px-4 py-3 text-xs text-amber-700"
          >
            <i class="fa-solid fa-triangle-exclamation"></i>
            Sélectionnez d'abord un groupe d'option pour afficher ses étudiants.
          </div>

          <template v-else>
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
          </template>
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
            :disabled="submitting || form.memberIds.length < minMembers || (!isOption && !form.parentGroupId) || (isOption && !form.name)"
            class="px-6 py-2 bg-indigo-600 hover:bg-indigo-700 disabled:opacity-50
                   text-white rounded-lg font-medium text-sm transition cursor-pointer"
          >
            <i v-if="submitting" class="fa-solid fa-spinner animate-spin mr-2"></i>
            <i v-else :class="editId ? 'fa-solid fa-floppy-disk' : 'fa-solid fa-plus'" class="mr-2"></i>
            {{ editId ? 'Enregistrer' : 'Créer le groupe' }}
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
const editId = computed(() => route.query.id ? Number(route.query.id) : null)
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
const promotions = ref([])
const promotionFilter = ref('')
const sameTypeGroups = ref([])

const form = reactive({
  name: '',
  categoryId: '',
  customCategoryName: '',
  parentGroupId: '',
  promotionId: '',
  memberIds: [],
})

const promotionMemberIds = computed(() => {
  if (!promotionFilter.value) return null
  const found = promotions.value.find(p => p.id === promotionFilter.value)
  return found?.members ? new Set(found.members.map(m => m.id)) : null
})

// --- Auto-génération du nom OPTION ---
const nameAutoFilled = ref(false)

function normalizePart(str) {
  return str
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .replace(/\s+/g, '-')
    .replace(/[^a-zA-Z0-9-_]/g, '')
}

function buildOptionName() {
  const catName = useCustomCategory.value
    ? form.customCategoryName
    : categories.value.find(c => c.id === form.categoryId)?.name
  const promo = promotions.value.find(p => p.id === form.promotionId)
  if (!catName || !promo) return null
  const cat = normalizePart(catName)
  const promoName = normalizePart(promo.name)
  return promo.year ? `${cat}-${promoName}_${promo.year}` : `${cat}-${promoName}`
}

function regenerateName() {
  const generated = buildOptionName()
  if (generated) { form.name = generated; nameAutoFilled.value = true }
}

watch(
  [() => form.categoryId, () => form.customCategoryName, () => form.promotionId],
  () => {
    if (editId.value) return
    if (isOption.value) {
      const generated = buildOptionName()
      if (generated) { form.name = generated; nameAutoFilled.value = true }
    } else {
      // PROJECT : le nom = la catégorie sélectionnée
      if (useCustomCategory.value) {
        form.name = form.customCategoryName
      } else {
        const cat = categories.value.find(c => c.id === form.categoryId)
        if (cat) form.name = cat.name
      }
    }
  }
)

// Pour OPTION : quand une promo est sélectionnée, auto-sélectionner tous ses étudiants
watch(() => form.promotionId, (newPromoId) => {
  if (!isOption.value) return
  if (!newPromoId) {
    form.memberIds = []
    return
  }
  const promo = promotions.value.find(p => p.id === newPromoId)
  form.memberIds = promo?.members?.map(m => m.id) ?? []
})

// Pour PROJECT : vider la sélection quand le groupe d'option change
watch(() => form.parentGroupId, () => {
  if (!isOption.value) form.memberIds = []
})

// Enseignants du groupe d'option parent (PROJECT uniquement)
const parentGroupTeachers = computed(() => {
  if (isOption.value || !form.parentGroupId) return []
  const og = optionGroups.value.find(g => g.id === form.parentGroupId)
  return og?.teachers ?? []
})

// Étudiants déjà dans un groupe du même type (exclus de la sélection)
const busyStudentIds = computed(() => {
  const ids = new Set()
  for (const g of sameTypeGroups.value) {
    if (editId.value && g.id === editId.value) continue
    if (g.members) g.members.forEach(m => ids.add(m.id))
  }
  return ids
})

const filteredStudents = computed(() => {
  const q = studentSearch.value.toLowerCase().trim()
  let list = allStudents.value

  // Pour OPTION avec promo : afficher UNIQUEMENT les étudiants de la promo
  if (isOption.value && form.promotionId) {
    const promo = promotions.value.find(p => p.id === form.promotionId)
    const promoIds = promo?.members ? new Set(promo.members.map(m => m.id)) : null
    if (promoIds) list = list.filter(s => promoIds.has(s.id))
  }

  // Pour PROJECT avec groupe d'option : afficher UNIQUEMENT les membres de ce groupe
  if (!isOption.value && form.parentGroupId) {
    const optGrp = optionGroups.value.find(og => og.id === form.parentGroupId)
    const optIds = optGrp?.members ? new Set(optGrp.members.map(m => m.id)) : null
    if (optIds) list = list.filter(s => optIds.has(s.id))
  }

  // Exclure les étudiants déjà dans un groupe du même type
  // Exception : PROMOTION — un étudiant peut redoubler et rejoindre une nouvelle promo
  if (type !== 'PROMOTION') {
    list = list.filter(s => !busyStudentIds.value.has(s.id))
  }

  if (!q) return list
  return list.filter(s => s.name.toLowerCase().includes(q))
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

  // Charger les promotions (pour OPTION : lier, pour PROJECT : filtrer)
  try {
    promotions.value = await api('/groups/type/PROMOTION')
  } catch {
    // liste vide en cas d'erreur
  }

  try {
    sameTypeGroups.value = await api(`/groups/type/${type}`)
  } catch {
    // liste vide en cas d'erreur
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

  // Mode édition : pré-remplir le formulaire
  if (editId.value) {
    try {
      const group = await api(`/groups/${editId.value}`)
      form.name = group.name
      form.categoryId = group.categoryId ?? ''
      form.parentGroupId = group.parentGroupId ?? ''
      form.promotionId = group.promotionId ?? ''
      form.memberIds = group.members?.map(m => m.id) ?? []
    } catch {
      submitError.value = 'Impossible de charger les données du groupe.'
    }
  }
})

async function handleSubmit() {
  submitting.value = true
  submitError.value = null
  try {
    const body = {
      name: form.name,
      type: type,
      categoryId: useCustomCategory.value ? null : form.categoryId,
      customCategoryName: useCustomCategory.value ? form.customCategoryName : null,
      parentGroupId: (!isOption.value && form.parentGroupId) ? form.parentGroupId : null,
      promotionId: (isOption.value && form.promotionId) ? form.promotionId : null,
      memberIds: form.memberIds,
    }
    if (editId.value) {
      await api(`/groups/${editId.value}`, { method: 'PUT', body })
    } else {
      await api('/groups', { method: 'POST', body })
    }
    router.push('/groups?tab=' + type)
  } catch (err) {
    submitError.value = err?.data?.error ?? err?.data?.message ?? 'Une erreur est survenue lors de la création du groupe.'
  } finally {
    submitting.value = false
  }
}
</script>
