<template>
  <UDropdown :items="items" :popper="{ placement: 'bottom-end' }">
    <UButton
      color="gray"
      variant="ghost"
      icon="i-heroicons-language"
      class="text-gray-500 hover:text-blue-600 transition-colors"
    >
      <span class="text-xs font-bold">{{ locale.toUpperCase() }}</span>
    </UButton>
  </UDropdown>
</template>

<script setup>
const { locale, setLocale } = useI18n()

const items = [
  [{
    label: 'FR',
    icon: 'i-heroicons-language',
    click: () => {
      setLocale('fr')
      window.location.reload()
    }
  }, {
    label: 'EN',
    icon: 'i-heroicons-language',
    click: () => {
      setLocale('en')
      window.location.reload()
    }
  }]
]
</script>
