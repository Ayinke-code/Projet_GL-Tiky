<template>
  <UContainer class="py-12">
    <ClientOnly>
      <div v-if="isTeacher" class="space-y-12">
        <section><AssignationManager :userId="user.id" /></section>

        <div class="relative py-8">
          <div class="absolute inset-0 flex items-center"><span class="w-full border-t border-gray-200"></span></div>
          <div class="relative flex justify-center"><span class="bg-white px-2 text-sm text-gray-400"></span></div>
        </div>

        <section><AssignationTeacher :userId="user.id" /></section>
      </div>

      <div v-else class="flex flex-col items-center justify-center h-64 text-center">
        <UIcon name="i-heroicons-lock-closed" class="w-12 h-12 text-red-400 mb-4" />
        <h3 class="text-xl font-bold">Accès réservé</h3>
        <p class="text-gray-500">Permissions insuffisantes.</p>
      </div>
    </ClientOnly>
  </UContainer>
</template>

<script setup>
const { user } = useAuth() // Ton composable d'auth

const isTeacher = computed(() => {
  return user.value?.roles?.includes('TEACHER') || user.value?.roles?.includes('ADMIN') || false
})

</script>
