<template>
  <UDropdown :items="items" :popper="{ placement: 'bottom-start' }">
    <UButton color="white" label="FR" trailing-icon="i-heroicons-chevron-down-20-solid" />
  </UDropdown>
</template>

<script setup>
const items = [
  [{
    label: 'Français',
    icon: 'i-heroicons-language',
    click: () => {
      console.log('Changement de langue : FR')
    }
  }, {
    label: 'English',
    icon: 'i-heroicons-language',
    click: () => {
      console.log('Changement de langue : EN')
    }
  }]
]
</script>
