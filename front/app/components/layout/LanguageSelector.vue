<template>
  <UDropdownMenu :items="items" :content="{ align: 'end' }">
    <UButton
      color="gray"
      variant="ghost"
      icon="i-heroicons-language"
      class="text-gray-500 hover:text-blue-600 transition-colors cursor-pointer"
    >
      <span class="text-xs font-bold">{{ locale.toUpperCase() }}</span>
    </UButton>
  </UDropdownMenu>
</template>

<script setup>
const { locale, setLocale } = useLoginI18n()

const items = [
  {
    label: 'FR',
    icon: 'i-heroicons-language',
    onSelect: () => {
      setLocale('fr')
      window.location.reload()
    }
  },
  {
    label: 'EN',
    icon: 'i-heroicons-language',
    onSelect: () => {
      setLocale('en')
      window.location.reload()
    }
  }
]
</script>
