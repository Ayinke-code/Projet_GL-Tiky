<template>
  <UDropdown :items="items" :popper="{ placement: 'bottom-end' }">
    <UButton
      color="gray"
      variant="ghost"
      icon="i-heroicons-language"
      class="text-gray-500 hover:text-blue-600 transition-colors"
    >
      <span class="text-xs font-bold">{{ currentLang }}</span>
    </UButton>
  </UDropdown>
</template>

<script setup>
const currentLang = ref('FR')

const items = [
  [{
    label: 'Français',
    icon: 'i-heroicons-language',
    click: () => {
      currentLang.value = 'FR'
      console.log('Changement de langue : FR')
    }
  }, {
    label: 'English',
    icon: 'i-heroicons-language',
    click: () => {
      currentLang.value = 'EN'
      console.log('Changement de langue : EN')
    }
  }]
]
</script>
