<template>
  <aside class="w-64 bg-slate-900 text-white flex flex-col h-screen border-r border-slate-800">
    <!-- Logo -->
    <div class="h-16 flex items-center px-6 border-b border-slate-800 font-bold text-xl tracking-wider">
      <span class="text-indigo-400 font-extrabold text-2xl mr-2 tracking-tighter">tk.</span> Tiky
    </div>

    <!-- Navigation Scrollable -->
    <nav class="flex-1 px-4 py-6 space-y-2 overflow-y-auto custom-scrollbar">
      <div v-if="user" class="mb-4 rounded-xl border border-slate-800 bg-slate-950/60 p-4">
        <p class="text-[10px] font-bold text-slate-500 uppercase tracking-widest mb-2">Mon espace</p>
        <p class="text-sm font-semibold text-white">{{ user?.name || 'Utilisateur' }}</p>
        <div class="mt-3 flex flex-wrap gap-2 text-[10px] font-bold uppercase tracking-wide">
          <span v-for="role in user.roles" class="rounded-full bg-slate-800 px-2.5 py-1 text-slate-300">{{ role
            }}</span>
          <span v-if="user?.contributor" class="rounded-full bg-emerald-500/20 px-2.5 py-1 text-emerald-300">Issue
            Creator</span>
          <!-- TODO: Faire les routes une fois disponible -->
          <!-- <span v-if="isIssueFollower" class="rounded-full bg-blue-500/20 px-2.5 py-1 text-blue-300">Issue
            Follower</span>
          <span v-if="isParticipant" class="rounded-full bg-amber-500/20 px-2.5 py-1 text-amber-300">Participant</span> -->
        </div>
      </div>

      <p v-if="ready && user?.value?.id"
        class="px-4 text-[10px] font-bold text-slate-500 uppercase tracking-widest mb-3">
        Activités
      </p>

      <NuxtLink v-if="ready && user?.id" to="/issues"
        class="flex items-center px-4 py-2.5 rounded-lg transition-all duration-200"
        :class="isActive('/issues') ? 'bg-indigo-600 text-white shadow-lg' : 'text-slate-400 hover:bg-slate-800 hover:text-white'">
        <i class="fa-solid fa-circle-exclamation w-6"></i>
        <span class="font-medium">Mes issues</span>
      </NuxtLink>

      <NuxtLink v-if="user" to="/categories"
        class="flex items-center px-4 py-2.5 rounded-lg transition-all duration-200"
        :class="isActive('/categories') ? 'bg-indigo-600 text-white shadow-lg' : 'text-slate-400 hover:bg-slate-800 hover:text-white'">
        <i class="fa-solid fa-folder w-6"></i>
        <span class="font-medium">Mes catégories</span>
      </NuxtLink>

      <NuxtLink v-if="user" to="/calendar" class="flex items-center px-4 py-2.5 rounded-lg transition-all duration-200"
        :class="isActive('/calendar') ? 'bg-indigo-600 text-white shadow-lg' : 'text-slate-400 hover:bg-slate-800 hover:text-white'">
        <i class="fa-solid fa-calendar w-6"></i>
        <span class="font-medium">Mon calendrier</span>
      </NuxtLink>


    </nav>

    <!-- Footer Sidebar : Fixé en bas -->
    <div class="p-4 border-t border-slate-800 bg-slate-950">
      <button v-if="!user"
        class="w-full flex items-center justify-center px-4 py-2 bg-slate-800 hover:bg-blue-900/30 text-slate-400 hover:text-blue-400 rounded-lg transition-all duration-200 text-sm font-medium cursor-pointer transition-all"
        @click="goToLogin">
        Se connecter
      </button>
      <button v-else @click="logout"
        class="w-full flex items-center justify-center px-4 py-2 bg-slate-800 hover:bg-red-900/30 text-slate-400 hover:text-red-400 rounded-lg transition-all duration-200 text-sm font-medium cursor-pointer transition-all">
        <i class="fa-solid fa-arrow-right-from-bracket mr-2"></i> Déconnexion
      </button>
    </div>
  </aside>
</template>

<script setup>
const route = useRoute()
const { user, ready, logout } = useAuth()
const isActive = (path) => route.path.startsWith(path)

// Redirection vers la page de connexion
const goToLogin = () => navigateTo('/login')

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
