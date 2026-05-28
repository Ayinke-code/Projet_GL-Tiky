<template>
  <Modal v-model:open="isOpen" title="Suivre cette issue">

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
      <UButton
        color="neutral"
        variant="ghost"
        @click="isOpen = false"
      >
        Annuler
      </UButton>
      <UButton
        color="primary"
        :disabled="!selected || loading"
        :loading="loading"
        @click="handleConfirm"
      >
        Confirmer
      </UButton>
    </div>

  </Modal>
</template>

<script setup lang="js">
/**
 * Modale de suivi d'une issue.
 * - propose trois raisons exclusives : INTERESTED, ABLE_TO_HELP, IMPACTED
 * - appelle POST /issues/{id}/follow avec la raison choisie
 * - émet l'événement 'followed' après succès
 */

import Modal from '~/components/ui/Modal.vue'

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
