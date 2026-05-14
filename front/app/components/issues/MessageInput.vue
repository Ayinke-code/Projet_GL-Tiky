<template>
  <div v-bind="$attrs" class="rounded-2xl border border-gray-200 bg-white p-4 shadow-sm">
    <label class="mb-2 block text-sm font-medium text-gray-700">Ajouter un message</label>
    <textarea
      v-model="value"
      rows="4"
      class="w-full rounded-xl border border-gray-200 p-3 text-sm outline-none transition focus:border-primary"
      placeholder="Écrire votre message..."
      @keydown.ctrl.enter.prevent="emitSend"
    />
    <div class="mt-3 flex justify-end">
      <button
        class="rounded-xl bg-primary px-4 py-2 text-sm font-semibold text-white disabled:cursor-not-allowed disabled:opacity-50"
        :disabled="sending || !value.trim()"
        @click="emitSend"
      >
        {{ sending ? 'Envoi...' : 'Envoyer' }}
      </button>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

defineOptions({ inheritAttrs: false })

const props = defineProps({
  modelValue: { type: String, default: '' },
  sending: { type: Boolean, default: false }
})

const emit = defineEmits(['update:modelValue', 'send'])

const value = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})

const emitSend = () => emit('send')
</script>
