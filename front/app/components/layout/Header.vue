<template>
  <header class="h-16 bg-white shadow-sm flex items-center justify-between px-4 md:px-8">
    <div class="flex items-center">
      <button
        @click="$emit('toggle-sidebar')"
        class="md:hidden mr-4 text-gray-500 hover:text-gray-700 focus:outline-none"
      >
        <i class="fa-solid fa-bars text-xl"></i>
      </button>
      <h1 class="text-xl md:text-2xl font-bold text-gray-700">Tiky</h1>
    </div>

    <div class="flex items-center space-x-4">
      <ClientOnly>
        <div class="h-9 w-9 rounded-full bg-blue-100 flex items-center justify-center font-bold text-blue-600">
          {{ userInitial }}
        </div>
        <template #fallback>
          <div class="h-9 w-9 rounded-full bg-slate-200 animate-pulse"></div>
        </template>
      </ClientOnly>
    </div>
  </header>
</template>

<script setup>

defineEmits(['toggle-sidebar'])
const { user, ready } = useAuth()
const userInitial = computed(() => {
  if (ready && user?.value?.name) return user.value.name.charAt(0).toUpperCase()
  else return '?'
})
</script>
