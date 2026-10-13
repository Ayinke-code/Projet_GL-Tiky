<template>
  <div class="p-8 min-h-screen bg-gray-50">

    <!-- En-tête -->
    <div class="flex items-center gap-4 mb-8">
      <button
        @click="router.push('/groups?tab=PROMOTION')"
        class="text-gray-400 hover:text-gray-700 transition cursor-pointer"
        title="Retour aux groupes"
      >
        <i class="fa-solid fa-arrow-left text-lg"></i>
      </button>
      <div class="flex items-center gap-3">
        <UBadge color="primary" variant="soft" size="lg">
          <i class="fa-solid fa-users mr-1"></i>
          Promotion
        </UBadge>
        <h1 class="text-2xl font-bold text-gray-800">{{ editId ? 'Modifier' : 'Créer' }} une promotion</h1>
      </div>
    </div>

    <div class="max-w-xl space-y-6">

      <!-- Formulaire création -->
      <div class="bg-white rounded-2xl shadow-sm border border-gray-100 p-6">

        <Alert v-if="submitError" color="error" title="Erreur" :description="submitError" class="mb-4" />

        <form @submit.prevent="handleSubmit" class="space-y-5">

          <!-- Nom -->
          <div>
            <label class="block text-xs font-medium text-gray-700 mb-1">
              Nom de la promotion <span class="text-red-500">*</span>
            </label>
            <input
              v-model="form.name"
              type="text"
              placeholder="Ex : IR 2026"
              class="w-full px-4 py-2 border border-gray-300 rounded-lg text-sm
                     focus:ring-2 focus:ring-indigo-500 outline-none"
              required
            />
          </div>

          <!-- Année -->
          <div>
            <label class="block text-xs font-medium text-gray-700 mb-1">
              Année de promotion <span class="text-red-500">*</span>
            </label>
            <input
              v-model.number="form.year"
              type="number"
              placeholder="Ex : 2026"
              min="2000"
              max="2100"
              class="w-full px-4 py-2 border border-gray-300 rounded-lg text-sm
                     focus:ring-2 focus:ring-indigo-500 outline-none"
              required
            />
          </div>

          <!-- Actions -->
          <div class="flex justify-end gap-3 pt-2 border-t">
            <button
              type="button"
              @click="router.push('/groups?tab=PROMOTION')"
            >
              Annuler
            </button>
            <button
              type="submit"
              :disabled="submitting || !form.name || !form.year"
              class="px-6 py-2 bg-indigo-600 hover:bg-indigo-700 disabled:opacity-50
                     text-white rounded-lg font-medium text-sm transition cursor-pointer"
            >
              <i v-if="submitting" class="fa-solid fa-spinner animate-spin mr-2"></i>
              <i v-else :class="editId ? 'fa-solid fa-floppy-disk' : 'fa-solid fa-plus'" class="mr-2"></i>
              {{ editId ? 'Enregistrer' : 'Créer la promotion' }}
            </button>
          </div>

        </form>
      </div>

      <!-- Section import (visible après création) -->
      <div v-if="createdGroup" class="bg-white rounded-2xl shadow-sm border border-gray-100 p-6 space-y-5">
        <div class="flex items-center gap-2 text-emerald-600 font-semibold text-sm">
          <i class="fa-solid fa-circle-check"></i>
          Promotion « {{ createdGroup.name }} » créée — ajoutez les étudiants
        </div>
        <div class="flex items-center gap-3 bg-indigo-50 rounded-lg px-4 py-2 text-sm">
          <i class="fa-solid fa-users text-indigo-500"></i>
          <span class="text-gray-700">
            <span :class="totalMembers >= 50 ? 'text-emerald-600 font-bold' : 'text-orange-500 font-bold'">
              {{ totalMembers }}
            </span>
            <span class="text-gray-500"> / 50 étudiants minimum</span>
          </span>
          <i v-if="totalMembers >= 50" class="fa-solid fa-circle-check text-emerald-500 ml-auto"></i>
          <i v-else class="fa-solid fa-triangle-exclamation text-orange-400 ml-auto"></i>
        </div>

        <Alert v-if="importError" color="error" title="Erreur import" :description="importError" />
        <Alert
          v-if="importResult"
          color="success"
          title="Import terminé"
          :description="`${importResult.created} créé(s), ${importResult.alreadyExisted} déjà existant(s), ${importResult.skipped} ignoré(s)`"
        />

        <!-- Import fichier -->
        <div>
          <p class="text-xs font-medium text-gray-700 mb-2">
            Importer via CSV ou Excel (.xlsx)
            <span class="text-gray-400 font-normal ml-1">(colonnes : Nom, Email)</span>
          </p>
          <div
            class="border-2 border-dashed border-gray-200 rounded-xl p-6 text-center hover:border-indigo-300 transition"
            @dragover.prevent
            @drop.prevent="onDrop"
          >
            <i class="fa-solid fa-file-arrow-up text-3xl text-gray-300 mb-2"></i>
            <p class="text-sm text-gray-500 mb-3">Glissez un fichier ou</p>
            <label class="cursor-pointer px-4 py-2 bg-indigo-50 hover:bg-indigo-100 text-indigo-700 rounded-lg text-sm font-medium transition">
              Parcourir
              <input type="file" :accept="IMPORT_ACCEPT" class="hidden" @change="onFileChange" />
            </label>
            <p
              id="import-upload-limits"
              class="mt-3 text-xs text-gray-400"
            >
              {{ IMPORT_LIMITS_LABEL }}
            </p>
            <p v-if="selectedFile" class="mt-3 text-xs text-indigo-600 font-medium">
              <i class="fa-solid fa-file-csv mr-1"></i>{{ selectedFile.name }}
            </p>
          </div>
          <button
            v-if="selectedFile"
            @click="handleImport"
            :disabled="importing"
            class="mt-3 w-full px-4 py-2 bg-indigo-600 hover:bg-indigo-700 disabled:opacity-50
                   text-white rounded-lg font-medium text-sm transition cursor-pointer"
          >
            <i v-if="importing" class="fa-solid fa-spinner animate-spin mr-2"></i>
            <i v-else class="fa-solid fa-upload mr-2"></i>
            Importer les étudiants
          </button>
        </div>

        <!-- Ajout manuel -->
        <div>
          <p class="text-xs font-medium text-gray-700 mb-2">Ajouter un étudiant manuellement</p>
          <div class="flex gap-2">
            <input
              v-model="manualName"
              type="text"
              placeholder="Nom complet"
              class="flex-1 px-3 py-2 border border-gray-300 rounded-lg text-sm
                     focus:ring-2 focus:ring-indigo-500 outline-none"
              @keyup.enter="addManualStudent"
            />
            <input
              v-model="manualEmail"
              type="email"
              placeholder="Email"
              class="flex-1 px-3 py-2 border border-gray-300 rounded-lg text-sm
                     focus:ring-2 focus:ring-indigo-500 outline-none"
              @keyup.enter="addManualStudent"
            />
            <button
              @click="addManualStudent"
              :disabled="addingStudent || !manualName || !manualEmail"
              class="px-4 py-2 bg-gray-800 hover:bg-gray-900 disabled:opacity-40
                     text-white rounded-lg text-sm font-medium transition cursor-pointer"
            >
              <i v-if="addingStudent" class="fa-solid fa-spinner animate-spin"></i>
              <i v-else class="fa-solid fa-plus"></i>
            </button>
          </div>
          <Alert v-if="manualError" color="error" :description="manualError" class="mt-2" />
        </div>

        <!-- Bouton terminer -->
        <div class="flex justify-end pt-2 border-t">
          <button
            @click="router.push('/groups?tab=PROMOTION')"
          >
            <i class="fa-solid fa-check mr-2"></i>
            Terminer
          </button>
        </div>
      </div>

    </div>
  </div>
</template>

<script setup>
definePageMeta({ ssr: false })

import Alert from '~/components/ui/Alert.vue'
import { validateImportFile, IMPORT_ACCEPT, IMPORT_LIMITS_LABEL } from '~/utils/validateFiles'

const route = useRoute()
const router = useRouter()
const api = useApi()

const editId = computed(() => route.query.id ? Number(route.query.id) : null)

const form = reactive({ name: '', year: new Date().getFullYear() })
const submitting = ref(false)
const submitError = ref(null)
const createdGroup = ref(null)

const selectedFile = ref(null)
const importing = ref(false)
const importResult = ref(null)
const importError = ref(null)
const totalMembers = ref(0)

const manualName = ref('')
const manualEmail = ref('')
const addingStudent = ref(false)
const manualError = ref(null)

async function handleSubmit() {
  submitting.value = true
  submitError.value = null
  try {
    if (editId.value) {
      const updated = await api(`/groups/${editId.value}`, {
        method: 'PUT',
        body: { name: form.name, year: form.year, type: 'PROMOTION', memberIds: [] }
      })
      createdGroup.value = updated
    } else {
      createdGroup.value = await api('/groups', {
        method: 'POST',
        body: { name: form.name, year: form.year, type: 'PROMOTION', memberIds: [] }
      })
    }
  } catch (err) {
    submitError.value = err?.data?.error ?? err?.data?.message ?? 'Une erreur est survenue.'
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  if (editId.value) {
    try {
      const group = await api(`/groups/${editId.value}`)
      form.name = group.name
      form.year = group.year ?? new Date().getFullYear()
      totalMembers.value = group.members?.length ?? 0
      createdGroup.value = group
    } catch {
      submitError.value = 'Impossible de charger la promotion.'
    }
  }
})

/**
 * Keeps the picked file only if it respects the import limits, so a wrong
 * format or an oversized file is refused before any transfer.
 *
 * @param {File|null} file - File picked through the input or dropped on the zone
 */
function selectImportFile(file) {
  importResult.value = null
  const check = validateImportFile(file)
  importError.value = check.valid ? null : check.error
  selectedFile.value = check.valid ? file : null
}

function onFileChange(e) {
  selectImportFile(e.target.files[0] ?? null)
  // Reset the input so picking the same file again after a rejection fires "change" again
  e.target.value = ''
}

function onDrop(e) {
  selectImportFile(e.dataTransfer.files[0] ?? null)
}

async function handleImport() {
  if (!selectedFile.value || !createdGroup.value) return
  importing.value = true
  importError.value = null
  importResult.value = null
  try {
    const formData = new FormData()
    formData.append('file', selectedFile.value)
    importResult.value = await api(`/groups/${createdGroup.value.id}/import-students`, {
      method: 'POST',
      body: formData
    })
    totalMembers.value += (importResult.value.created + importResult.value.alreadyExisted)
    selectedFile.value = null
  } catch (err) {
    importError.value = err?.data?.error ?? 'Erreur lors de l\'import.'
  } finally {
    importing.value = false
  }
}

async function addManualStudent() {
  if (!manualName.value || !manualEmail.value || !createdGroup.value) return
  addingStudent.value = true
  manualError.value = null
  try {
    // Crée un mini-CSV en mémoire et l'envoie
    const csvContent = `${manualName.value},${manualEmail.value}`
    const blob = new Blob([csvContent], { type: 'text/csv' })
    const file = new File([blob], 'student.csv', { type: 'text/csv' })
    const formData = new FormData()
    formData.append('file', file)
    const result = await api(`/groups/${createdGroup.value.id}/import-students`, {
      method: 'POST',
      body: formData
    })
    if (result.created > 0 || result.alreadyExisted > 0) {
      manualName.value = ''
      manualEmail.value = ''
      importResult.value = result
      totalMembers.value += (result.created + result.alreadyExisted)
    } else {
      manualError.value = 'Étudiant ignoré (données invalides ou déjà présent).'
    }
  } catch (err) {
    manualError.value = err?.data?.error ?? 'Erreur lors de l\'ajout.'
  } finally {
    addingStudent.value = false
  }
}
</script>
