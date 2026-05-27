<template>
  <Modal v-model:open="openModel" title="Refuser l'invitation">
    <div class="flex flex-col gap-4 p-2">
      <p class="text-sm text-gray-700">Souhaitez-vous indiquer un motif d'absence ? (Optionnel)</p>
      <textarea
        :value="reason"
        @input="$emit('update:reason', $event.target.value)"
        class="w-full border border-gray-300 rounded-lg p-3 text-sm focus:ring-2 focus:ring-indigo-500 outline-none"
        rows="3"
        placeholder="Raison de votre indisponibilité...">
      </textarea>
      <div class="flex justify-end gap-3 mt-4">
        <Button @click="$emit('update:open', false)" color="secondary" label="Annuler" variant="subtle" rounded />
        <Button @click="$emit('confirm')" color="error" label="Confirmer le refus" variant="solid" rounded />
      </div>
    </div>
  </Modal>
</template>

<script setup>
import { computed } from 'vue'
import Button from '~/components/ui/Button.vue'
import Modal from '~/components/ui/Modal.vue'

const props = defineProps({
  open: {
    type: Boolean,
    required: true
  },
  reason: {
    type: String,
    default: ''
  }
})

const emit = defineEmits(['update:open', 'update:reason', 'confirm'])

const openModel = computed({
  get: () => props.open,
  set: (val) => emit('update:open', val)
})
</script>
