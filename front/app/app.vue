<template>
  <div class="flex h-screen bg-gray-50">

    <!-- Sidebar -->
    <Sidebar v-if="ready && user" />

    <div v-if="ready && user" class="flex flex-col flex-1">

      <!-- Header -->
      <Header />

      <main class="flex-1 overflow-y-auto">
        <NuxtPage />
      </main>

    </div>

  </div>
</template>

<script setup>
import Header from '~/components/layout/Header.vue'
import Sidebar from '~/components/layout/Sidebar.vue'

const { user, ready } = useAuth()
</script>