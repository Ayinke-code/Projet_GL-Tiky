<template>
  <div v-if="isOpen" class="fixed inset-0 z-50 flex items-center justify-center">
    <div class="absolute inset-0 bg-black/50" @click="isOpen = false" />
    <div class="relative bg-white rounded-xl shadow-xl p-6 w-full max-w-md mx-4 z-10">
      <h3 class="text-lg font-bold mb-4">Suivre cette issue</h3>
      <p class="text-gray-600 mb-4">Pourquoi voulez-vous suivre cette issue ?</p>

      <div class="flex flex-col gap-3">
        <button
          v-for="option in options"
          :key="option.value"
          type="button"
          @click.stop="selected = option.value"
          :class="[
            'w-full px-4 py-3 rounded-lg border-2 text-left transition',
            selected === option.value
              ? 'border-blue-500 bg-blue-50 text-blue-700 font-semibold'
              : 'border-gray-200 bg-white text-gray-700 hover:border-blue-300'
          ]"
        >
          {{ option.label }}
        </button>
      </div>

      <div class="flex justify-end gap-2 mt-6">
        <button
          type="button"
          class="px-4 py-2 text-gray-600 hover:text-gray-800"
          @click="isOpen = false"
        >
          Annuler
        </button>
        <button
          type="button"
          :disabled="!selected || loading"
          class="px-4 py-2 bg-blue-500 text-white rounded-lg disabled:opacity-50 hover:bg-blue-600"
          @click="handleConfirm"
        >
          {{ loading ? 'Chargement...' : 'Confirmer' }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="js">
const props = defineProps({
  modelValue: Boolean,
  issueId: Number
})

const emit = defineEmits(['update:modelValue', 'followed'])

const { followIssue } = useIssueFollowers()
const loading = ref(false)
const selected = ref(null)

const isOpen = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})

const options = [
  { label: 'Intéressé', value: 'INTERESTED' },
  { label: 'Capable d\'aider', value: 'ABLE_TO_HELP' },
  { label: 'Également impacté', value: 'IMPACTED' }
]

const handleConfirm = async () => {
  if (!selected.value) return
  loading.value = true
  try {
    await followIssue(props.issueId, selected.value)
    emit('followed')
    isOpen.value = false
    selected.value = null
  } catch (err) {
    console.error(err)
  } finally {
    loading.value = false
  }
}
</script>
