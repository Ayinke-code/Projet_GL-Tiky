<template>
  <aside class="w-64 bg-slate-900 text-white flex flex-col h-screen border-r border-slate-800">
    <!-- Logo -->
    <div class="h-16 flex items-center px-6 border-b border-slate-800 font-bold text-xl tracking-wider">
      <span class="text-indigo-400 font-extrabold text-2xl mr-2 tracking-tighter">tk.</span> Tiky
    </div>

    <!-- Navigation Scrollable -->
    <nav class="flex-1 px-4 py-6 space-y-2 overflow-y-auto custom-scrollbar">
      <NuxtLink 
        to="/issues" 
        class="flex items-center px-4 py-2.5 rounded-lg transition-all duration-200"
        :class="isActive('/issues') ? 'bg-indigo-600 text-white shadow-lg' : 'text-slate-400 hover:bg-slate-800 hover:text-white'"
      >
        <i class="fa-solid fa-list-check w-6"></i> 
        <span class="font-medium">Mes tickets</span>
      </NuxtLink>

      <NuxtLink 
        to="/categorie" 
        class="flex items-center px-4 py-2.5 rounded-lg transition-all duration-200"
        :class="isActive('/categorie') ? 'bg-indigo-600 text-white shadow-lg' : 'text-slate-400 hover:bg-slate-800 hover:text-white'"
      >
        <i class="fa-solid fa-folder w-6"></i> 
        <span class="font-medium">Gestion Catégories</span>
      </NuxtLink>
    </nav>

    <!-- Footer Sidebar : Fixé en bas -->
    <div class="p-4 border-t border-slate-800 bg-slate-950">
      <button class="w-full flex items-center justify-center px-4 py-2 bg-slate-800 hover:bg-red-900/30 text-slate-400 hover:text-red-400 rounded-lg transition-all duration-200 text-sm font-medium">
        <i class="fa-solid fa-arrow-right-from-bracket mr-2"></i> Déconnexion
      </button>
    </div>
  </aside>
</template>

<script setup lang="ts">
import { useRoute } from 'vue-router'

const route = useRoute()

const isActive = (path) => route.path === path
</script>

<style scoped>
/* Style pour une scrollbar discrète sur la sidebar */
.custom-scrollbar::-webkit-scrollbar {
  width: 4px;
}
.custom-scrollbar::-webkit-scrollbar-thumb {
  background: #334155;
  border-radius: 4px;
}
</style>