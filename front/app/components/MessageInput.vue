<template>
  <div>
    <!-- Bandeau ticket terminé -->
    <div
      v-if="disabled"
      class="rounded-2xl border border-gray-200 bg-gray-50 p-4 shadow-sm text-center text-sm text-gray-500 flex items-center justify-center gap-2"
    >
      <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4 text-gray-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z" />
      </svg>
      Ce ticket est terminé — la messagerie est désactivée.
    </div>

    <!-- Formulaire normal -->
    <div v-else class="rounded-2xl border border-gray-200 bg-white p-4 shadow-sm">
      <label class="mb-2 block text-sm font-medium text-gray-700">
        Ajouter un commentaire ou une réponse
      </label>
      <div
        v-if="replyingTo"
        class="mb-3 rounded-xl border border-indigo-200 bg-indigo-50 px-3 py-2 text-xs text-indigo-700"
      >
        <div class="flex items-start justify-between gap-3">
          <div>
            <p class="font-semibold">
              Réponse à {{ replyingTo.auteur }}
            </p>
            <p class="mt-1 line-clamp-2 whitespace-pre-line text-indigo-600">
              {{ replyingTo.contenu }}
            </p>
          </div>
          <button
            type="button"
            class="text-indigo-500 hover:text-indigo-800"
            @click="emit('cancel-reply')"
          >
            ✕
          </button>
        </div>
      </div>
      <textarea
        id="input-message"
        v-model="value"
        rows="4"
        maxlength="4000"
        class="w-full rounded-xl border border-gray-200 p-3 text-sm outline-none transition focus:border-indigo-600"
        placeholder="Écrivez votre message..."
        @keydown.ctrl.enter.prevent="emitSend"
        style="overflow-wrap: anywhere; word-break: break-word;"
      />

      <!-- Fichiers optionnel -->
      <div class="space-y-2">
        <label class="block text-sm font-medium text-gray-400">Fichiers <span class="italic">(optionnel)</span></label>
        <UFileUpload
          v-model="messageFiles"
          multiple
          size="sm"
          accept="application/pdf,image/png,image/jpeg,.docx,.xlsx"
          icon="i-lucide-paperclip"
          layout="list"
          class="w-fit cursor-pointer"
          button-variant="ghost"
          :ui="{
            root: 'w-fit',
            wrapper: 'w-fit p-0',
            base: 'w-fit p-0',
            label: 'hidden',
            description: 'hidden',
            button: 'size-8 p-0 flex items-center justify-center rounded-lg text-gray-400 hover:text-indigo-600 hover:bg-gray-100',
            buttonLabel: 'hidden',
            buttonLeadingIcon: 'size-5',
          }"
        />
      </div>

      <!-- Erreur taille ou format de fichier -->
      <UAlert v-if="error"
        color="error"
        class="mt-4"
        @close="error = null"
        title="Erreur : "
        :description="error"
        icon="material-symbols:error"
        variant="subtle"
      />

      <div class="mt-1 text-right text-xs text-gray-400">
        {{ value.length }}/4000
      </div>
      <div class="mt-3 flex justify-end">
        <button
          id="btn_send-message"
          class="rounded-xl bg-indigo-600 hover:bg-indigo-700 px-4 py-2 text-sm font-semibold text-white disabled:cursor-not-allowed disabled:opacity-50 transition-colors"
          :disabled="sending || !value.trim()"
          @click="handleSend"
        >
          {{ sending ? 'Envoi...' : 'Envoyer' }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { validateFiles } from '../utils/validateFiles'

defineOptions({ inheritAttrs: false })

const error = ref(null)

const props = defineProps({
  modelValue: { type: String, default: '' },
  messageFiles: { type: Array, default: () => [] },
  sending: { type: Boolean, default: false },
  replyingTo: { type: Object, default: null },
  disabled: { type: Boolean, default: false }  // ← nouveau
})

const emit = defineEmits([
  'update:modelValue',
  'update:messageFiles',
  'send',
  'cancel-reply'
])

const value = computed({
  get: () => props.modelValue,
  set: val => emit('update:modelValue', val)
})

const messageFiles = computed({
  get: () => props.messageFiles,
  set: (val) => emit('update:messageFiles', val)
})

const handleSend = () => {
  const res = validateFiles(messageFiles.value)
  if (!res.valid) {
    error.value = res.error
    return
  }
  error.value = null
  emitSend()
}

const emitSend = () => emit('send')
</script>
