<template>
  <header class="rounded-2xl border border-gray-200 bg-white p-5 shadow-sm">
    <div class="mb-4 flex flex-wrap items-start justify-between gap-3">
      <div>
        <p class="text-sm text-gray-500">
          Ticket #{{ issueId }}
        </p>
        <h1 class="text-2xl font-bold text-gray-900">
          {{ title }}
        </h1>
        <p class="mt-1 text-sm text-gray-600">
          Catégorie : {{ category || '—' }}
        </p>
      </div>
      <div class="flex flex-wrap gap-2">
        <span class="rounded-full bg-gray-100 px-3 py-1 text-xs font-semibold text-gray-700">{{ status }}</span>
        <span class="rounded-full bg-blue-50 px-3 py-1 text-xs font-semibold text-blue-700">{{ visibility }}</span>
      </div>
    </div>

    <div class="flex flex-wrap gap-2">
      <button class="rounded-xl bg-green-600 px-4 py-2 text-sm font-semibold text-white" @click="$emit('resolveClick')">
        Marquer comme résolu
      </button>
      <button class="rounded-xl bg-gray-900 px-4 py-2 text-sm font-semibold text-white" @click="$emit('visibilityClick')">
        Modifier la visibilité
      </button>
    </div>
  </header>
</template>

<script setup>
defineProps({
  issueId: { type: [String, Number], default: '' },
  title: { type: String, default: '' },
  status: { type: String, default: '' },
  visibility: { type: String, default: '' },
  category: { type: String, default: '' },
  isResolved: { type: Boolean, default: false }
})

defineEmits(['resolveClick', 'visibilityClick'])
</script>
