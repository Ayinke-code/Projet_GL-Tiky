<template>
  <aside class="rounded-2xl border border-gray-200 bg-white p-5 shadow-sm">
    <h2 class="mb-4 text-lg font-semibold text-gray-900">Informations</h2>

    <dl class="space-y-3 text-sm">
      <div>
        <dt class="text-gray-500">Auteur</dt>
        <dd class="font-medium text-gray-900">{{ auteur || '—' }}</dd>
      </div>
      <div>
        <dt class="text-gray-500">Responsable</dt>
        <dd class="font-medium text-gray-900">{{ professeur || '—' }}</dd>
      </div>
      <div>
        <dt class="text-gray-500">Abonnés</dt>
        <dd class="font-medium text-gray-900">{{ abonnes?.length || 0 }}</dd>
      </div>
    </dl>
  </aside>
</template>

<script setup>
defineProps({
  auteur: { type: String, default: '' },
  professeur: { type: String, default: '' },
  abonnes: { type: Array, default: () => [] }
})
</script>
