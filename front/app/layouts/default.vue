<template>
  <div class="flex h-screen bg-gray-50 overflow-hidden relative">
    <!-- Overlay sombre pour mobile -->
    <div 
      v-if="isSidebarOpen" 
      @click="isSidebarOpen = false"
      class="fixed inset-0 bg-gray-900 bg-opacity-50 z-20 md:hidden transition-opacity"
    ></div>

    <!-- Sidebar : cachée sur mobile par défaut, affichée en absolu si ouverte -->
    <Sidebar 
      :class="[
        isSidebarOpen ? 'translate-x-0' : '-translate-x-full',
        'md:translate-x-0 fixed md:relative z-30 transition-transform duration-300 ease-in-out h-full'
      ]" 
    />
    
    <div class="flex-1 flex flex-col min-w-0">
      <Header @toggle-sidebar="isSidebarOpen = !isSidebarOpen" />
      <main class="flex-1 p-4 md:p-8 overflow-y-auto">
        <slot />
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'

const isSidebarOpen = ref(false)
</script>