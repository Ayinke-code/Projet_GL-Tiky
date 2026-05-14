<template>
  <UApp>

    <div class="flex h-screen bg-gray-50">

      <Sidebar v-if="!isAuthPage" />

      <div class="flex flex-col flex-1">

        <Header v-if="!isAuthPage" />

        <main class="flex-1 overflow-y-auto">
          <NuxtPage />

        </main>

      </div>

    </div>

  </UApp>

</template>

<script setup>
import Header from '~/components/layout/Header.vue'
import Sidebar from '~/components/layout/Sidebar.vue'

const route = useRoute()
const isAuthPage = computed(() => route.path === '/login')
</script>
