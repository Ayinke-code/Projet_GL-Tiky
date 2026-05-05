<template>
  <UAlert
    :color="color"
    :title="title"
    :description="description"
    :icon="icon"
  />
</template>
<script setup>
defineProps({
  color: {
    default: 'primary'
  },
  title: {
    required: true
  },
  description: {
    default: ''
  },
  icon: {
    default: 'i-lucide-info'
  }
})
</script>