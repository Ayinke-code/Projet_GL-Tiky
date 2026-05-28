<template>
  <Modal v-model:open="openModel" title="Confirmer l'annulation">
    <div class="p-2 flex flex-col gap-4">
      <div class="flex items-center gap-3 text-red-600 bg-red-50 p-3 rounded-xl border border-red-100">
        <span class="i-lucide-alert-triangle w-6 h-6 flex-shrink-0"></span>
        <p class="text-sm font-semibold">Cette action est irréversible.</p>
      </div>

      <p class="text-sm text-gray-600">
        Êtes-vous sûr de vouloir annuler {{ confirmCancelText }} ?
        Cette annulation sera répercutée dans l'agenda de tous les participants.
      </p>

      <div class="flex justify-end gap-3 mt-4 border-t pt-4">
        <Button @click="$emit('update:open', false)" color="secondary" label="Retour" variant="subtle" rounded />
        <Button @click="$emit('confirm')" color="error" label="Oui, annuler définitivement" variant="solid" rounded />
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
  confirmCancelText: {
    type: String,
    required: true
  }
})

const emit = defineEmits(['update:open', 'confirm'])

const openModel = computed({
  get: () => props.open,
  set: (val) => emit('update:open', val)
})
</script>
