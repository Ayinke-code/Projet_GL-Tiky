<template>
  <UButton
    :icon="icon"
    :size="size"
    :color="color"
    :variant="variant"
    :label="label"
    v-bind="$attrs"
    :class="buttonClass"
  />
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  icon: { type: String, default: '' },
  size: { type: String, default: 'md' },
  color: { type: String, default: 'primary' },
  variant: { type: String, default: 'solid' },
  label: { type: String, default: 'Button' },
  rounded: { type: Boolean, default: false }
})

const buttonClass = computed(() => [
  'cursor-pointer transition-all',

  props.rounded
    ? 'font-bold rounded-full'
    : ''
])
</script>