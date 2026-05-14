<template>
  <div v-if="isOpen" class="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4">
    <div class="w-full max-w-md rounded-2xl bg-white p-6 shadow-xl">
      <h3 class="text-lg font-semibold text-gray-900">
        Modifier la visibilité
      </h3>
      <p class="mt-2 text-sm text-gray-600">
        Visibilité actuelle :<strong>
          {{ visibility }}
        </strong>
      </p>
      <div class="mt-6 flex justify-end gap-3">
        <button class="rounded-xl border border-gray-200 px-4 py-2 text-sm" @click="$emit('update:isOpen', false)">
          Annuler
        </button>
        <button class="rounded-xl bg-gray-900 px-4 py-2 text-sm font-semibold text-white" @click="confirm">
          Confirmer
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
defineProps({
  isOpen: { type: Boolean, default: false },
  visibility: { type: String, default: 'Public' }
})

const emit = defineEmits(['update:isOpen', 'confirm'])

const confirm = () => {
  emit('confirm')
  emit('update:isOpen', false)
}
</script>
