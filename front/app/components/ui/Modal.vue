<template>
  <UModal
    v-model:open="open"
    :title="title"
    :close="close"
  >
    <template #body>
      <slot />
    </template>
  </UModal>
</template>

<script setup>

const open = defineModel('open')

defineProps({
  title: {
    type: String,
    default: ''
  },

  close: {
    type: [Boolean, Object],
    default: () => ({
      color: 'neutral',
      variant: 'outline',
      class: 'rounded-full cursor-pointer'
    })
  }
})

</script>