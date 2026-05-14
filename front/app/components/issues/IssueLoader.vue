<template>
  <div class="flex flex-col items-center justify-center gap-3 rounded-2xl border border-dashed border-gray-200 bg-white p-8 text-gray-500">
    <div class="h-8 w-8 animate-spin rounded-full border-4 border-gray-200 border-t-primary" />
    <p class="text-sm font-medium">
      {{ text }}
    </p>
  </div>
</template>

<script setup>
defineProps({
  text: {
    type: String,
    default: 'Chargement...'
  }
})
</script>
