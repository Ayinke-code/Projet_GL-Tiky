<template>
  <aside class="w-64 bg-slate-900 text-white flex flex-col h-screen border-r border-slate-800">
    <!-- Logo -->
    <div class="h-16 flex items-center px-6 border-b border-slate-800 font-bold text-xl tracking-wider">
      <span class="text-indigo-400 font-extrabold text-2xl mr-2 tracking-tighter">tk.</span> Tiky
    </div>

    <!-- Navigation Scrollable -->
    <nav class="flex-1 px-4 py-6 space-y-2 overflow-y-auto custom-scrollbar">
      <NuxtLink 
        to="/admin/categorie" 
        class="flex items-center px-4 py-2.5 rounded-lg transition-all duration-200"
        :class="isActive('/admin/categorie') ? 'bg-indigo-600 text-white shadow-lg' : 'text-slate-400 hover:bg-slate-800 hover:text-white'"
      >
        <i class="fa-solid fa-folder w-6"></i> 
        <span class="font-medium">Gestion Catégories</span>
      </NuxtLink>

      <!-- Liste dynamique des catégories -->
      <div class="pt-6 mt-6 border-t border-slate-800">
        <p class="px-4 text-[10px] font-bold text-slate-500 uppercase tracking-widest mb-3">
          Catégories actives
        </p>
        <ul class="space-y-1">
          <li v-for="category in activeCategories" :key="category.id" 
              class="flex items-center px-4 py-2 text-slate-400 hover:text-white hover:bg-slate-800 rounded-lg transition cursor-pointer">
            <span class="w-2 h-2 rounded-full inline-block mr-3" :class="getRandomColor(category.id)"></span>
            <span class="text-sm font-medium">{{ category.name }}</span>
          </li>
          
          <li v-if="activeCategories.length === 0" class="px-4 py-2 text-slate-600 italic text-xs">
            Aucune catégorie
          </li>
        </ul>
      </div>
    </nav>

    <!-- Footer Sidebar : Fixé en bas -->
    <div class="p-4 border-t border-slate-800 bg-slate-950">
      <button class="w-full flex items-center justify-center px-4 py-2 bg-slate-800 hover:bg-red-900/30 text-slate-400 hover:text-red-400 rounded-lg transition-all duration-200 text-sm font-medium">
        <i class="fa-solid fa-arrow-right-from-bracket mr-2"></i> Déconnexion
      </button>
    </div>
  </aside>
</template>

<script setup>
const route = useRoute()
// Assure-toi d'importer ton store ou composable ici
const { categories } = useCategoryMock() 

const isActive = (path) => route.path === path

const activeCategories = computed(() => {
  return categories.value.filter(c => c.status === 'active')
})

const colors = ['bg-emerald-400', 'bg-blue-400', 'bg-violet-400', 'bg-amber-400', 'bg-rose-400']
const getRandomColor = (id) => colors[id % colors.length]
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