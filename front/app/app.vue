<template>
  <div class="flex h-screen bg-gray-50">

    <!-- Sidebar -->
    <Sidebar v-if="!isAuthPage" />

    <div class="flex flex-col flex-1 relative">

      <!-- Header -->
      <Header v-if="!isAuthPage" />
      <div v-else-if="route.path === '/login'" class="absolute top-4 right-4 z-50">
        <LanguageSelector />
      </div>

      <main class="flex-1 overflow-y-auto">
        <NuxtPage />
      </main>

    </div>

  </div>
</template>

<script setup>
import Header from '~/components/layout/Header.vue'
import Sidebar from '~/components/layout/Sidebar.vue'
import LanguageSelector from '~/components/layout/LanguageSelector.vue'

const route = useRoute()
const { startSync, stopSync } = useIssueSync()

onMounted(() => {
  startSync()
})

onUnmounted(() => {
  stopSync()
})

const isAuthPage = computed(() => route.path === '/login' || route.path === '/')
</script>
