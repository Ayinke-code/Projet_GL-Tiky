<template>
  <div v-bind="$attrs" class="rounded-2xl border border-gray-200 bg-white p-4 shadow-sm">
    <label class="mb-2 block text-sm font-medium text-gray-700">Ajouter un message</label>
    <div
      v-if="replyingTo"
      class="mb-3 rounded-xl bg-gray-100 p-3 text-sm"
    >
      <div class="flex justify-between">
    <span>
      Réponse à <strong>{{ replyingTo.auteur }}</strong>
    </span>

        <button @click="$emit('cancelReply')">
          ✕
        </button>
      </div>

      <p class="truncate text-gray-500">
        {{ replyingTo.contenu }}
      </p>
    </div>
    <textarea
      v-model="value"
      rows="4"
      maxlength="4000"
      class="w-full rounded-xl border border-gray-200 p-3 text-sm outline-none transition focus:border-primary"
      placeholder="Écrire votre message..."
      @keydown.ctrl.enter.prevent="emitSend"
    />
    <div class="mt-1 text-right text-xs text-gray-400">
      {{ value.length }}/4000
    </div>
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
  sending: { type: Boolean, default: false },
  replyingTo: { type: Object, default: null }
})

const emit = defineEmits([
  'update:modelValue',
  'send',
  'cancelReply'
])

const value = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})

const emitSend = () => emit('send')
</script>

