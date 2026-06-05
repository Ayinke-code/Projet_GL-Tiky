<template>
  <aside class="w-64 bg-slate-900 text-white flex flex-col h-screen border-r border-slate-800" :aria-label="$t('sidebar.main_label')">
    <div class="h-16 flex items-center px-6 border-b border-slate-800 font-bold text-xl tracking-wider">
      <span class="text-indigo-400 font-extrabold text-2xl mr-2 tracking-tighter">tk.</span> Tiky
    </div>

    <ClientOnly>
      <nav class="flex-1 px-4 py-6 space-y-2 overflow-y-auto custom-scrollbar" :aria-label="$t('sidebar.nav_label')">
        <div v-if="user" class="mb-4 rounded-xl border border-slate-800 bg-slate-950/60 p-4">
          <p class="text-[10px] font-bold text-slate-500 uppercase tracking-widest mb-2">
            {{ $t('sidebar.my_space') }}
          </p>
          <p class="text-sm font-semibold text-white">
            {{ user?.name || $t('profile') }}
          </p>
          <div class="mt-3 flex flex-wrap gap-2 text-[10px] font-bold uppercase tracking-wide">
            <span v-for="role in user.roles" :key="role" class="rounded-full bg-slate-800 px-2.5 py-1 text-slate-300">{{ translateRole(role) }}</span>
          </div>
        </div>

        <p v-if="user?.id" class="px-4 text-[10px] font-bold text-slate-500 uppercase tracking-widest mb-3">
          {{ $t('sidebar.activities') }}
        </p>

        <UCollapsible class="w-full">
          <UButton
            class="group justify-between cursor-pointer w-full px-4 py-2.5 rounded-lg transition-all duration-200"
            :class="isActive('/issues') || isActive('/issues/followed') ? 'bg-indigo-600 text-white shadow-lg' : 'text-slate-400 hover:bg-slate-800 hover:text-white'"
            variant="ghost"
            trailing-icon="i-lucide-chevron-down"
            :ui="{
              trailingIcon: 'group-data-[state=open]:rotate-180 transition-transform duration-200 ml-auto'
            }"
            block
          >
            <div class="flex items-center">
              <i class="fa-solid fa-circle-exclamation w-6"></i>
              <span class="font-medium">{{ $t('sidebar.tickets') }}</span>
            </div>
          </UButton>

          <template #content>
            <div class="mt-1 flex flex-col gap-1">
              <NuxtLink v-if="user?.id" to="/issues"
                        class="flex items-center pl-10 pr-4 py-2 rounded-lg transition-all duration-200"
                        :class="isActive('/issues') && !isActive('/issues/followed') ? 'text-indigo-400 bg-slate-800/50' : 'text-slate-400 hover:bg-slate-800 hover:text-white'">
                <i class="fa-solid fa-list-check w-6"></i>
                <span class="font-medium text-sm">{{ $t('sidebar.all_tickets') }}</span>
              </NuxtLink>

              <NuxtLink v-if="user" to="/issues/followed"
                        class="flex items-center pl-10 pr-4 py-2 rounded-lg transition-all duration-200"
                        :class="isActive('/issues/followed') ? 'text-indigo-400 bg-slate-800/50' : 'text-slate-400 hover:bg-slate-800 hover:text-white'">
                <i class="fa-solid fa-bell w-6"></i>
                <span class="font-medium text-sm">{{ $t('sidebar.followed_tickets') }}</span>
              </NuxtLink>
            </div>
          </template>
        </UCollapsible>

        <NuxtLink
          v-if="user" to="/categories"
          class="flex items-center px-4 py-2.5 rounded-lg transition-all duration-200"
          :class="isActive('/categories') ? 'bg-indigo-600 text-white shadow-lg' : 'text-slate-400 hover:bg-slate-800 hover:text-white'">
          <i class="fa-solid fa-folder w-6" aria-hidden="true"></i>
          <span class="font-medium">{{ $t('sidebar.categories') }}</span>
        </NuxtLink>

        <NuxtLink
          v-if="user"
          to="/groups"
          class="flex items-center px-4 py-2.5 rounded-lg transition-all duration-200"
          :class="isActive('/groups') ? 'bg-indigo-600 text-white shadow-lg' : 'text-slate-400 hover:bg-slate-800 hover:text-white'">
          <i class="fa-solid fa-users w-6" aria-hidden="true"></i>
          <span class="font-medium">{{ $t('sidebar.groups') }}</span>
        </NuxtLink>

        <NuxtLink
          v-if="user"
          to="/calendar"
          class="flex items-center px-4 py-2.5 rounded-lg transition-all duration-200"
          :class="isActive('/calendar') ? 'bg-indigo-600 text-white shadow-lg' : 'text-slate-400 hover:bg-slate-800 hover:text-white'">
          <i class="fa-solid fa-calendar w-6" aria-hidden="true"></i>
          <span class="font-medium">{{ $t('sidebar.calendar') }}</span>
        </NuxtLink>

        <NuxtLink
          v-if="user?.roles?.includes('TEACHER') || user?.roles?.includes('ADMIN')"
          to="/events"
          class="flex items-center px-4 py-2.5 rounded-lg transition-all duration-200"
          :class="isActive('/events') ? 'bg-indigo-600 text-white shadow-lg' : 'text-slate-400 hover:bg-slate-800 hover:text-white'">
          <i class="fa-solid fa-calendar-days w-6" aria-hidden="true"></i>
          <span class="font-medium">{{ $t('sidebar.events') }}</span>
        </NuxtLink>

        <NuxtLink v-if="user" to="/invitations"
                  class="flex items-center px-4 py-2.5 rounded-lg transition-all duration-200"
                  :class="isActive('/invitations') ? 'bg-indigo-600 text-white shadow-lg' : 'text-slate-400 hover:bg-slate-800 hover:text-white'">
          <i class="fa-solid fa-folder w-6"></i>
          <span class="font-medium">{{ $t('sidebar.manage_planning') }}</span>
        </NuxtLink>
      </nav>

      <div class="p-4 border-t border-slate-800 bg-slate-950">
        <button v-if="!user"
                class="w-full flex items-center justify-center px-4 py-2 bg-slate-800 hover:bg-blue-900/30 text-slate-400 hover:text-blue-400 rounded-lg transition-all duration-200 text-sm font-medium cursor-pointer transition-all"
                @click="goToLogin">
          {{ $t('login') }}
        </button>
        <button v-else @click="logout"
                class="w-full flex items-center justify-center px-4 py-2 bg-slate-800 hover:bg-red-900/30 text-slate-400 hover:text-red-400 rounded-lg transition-all duration-200 text-sm font-medium cursor-pointer transition-all">
          <i class="fa-solid fa-arrow-right-from-bracket mr-2" aria-hidden="true"></i> {{ $t('logout') }}
        </button>
      </div>

      <template #fallback>
        <nav class="flex-1 px-4 py-6 space-y-2" :aria-label="$t('sidebar.nav_label')">
          <div v-for="n in 3" :key="n" class="h-10 w-full rounded-lg bg-slate-800 animate-pulse"></div>
        </nav>
        <div class="p-4 border-t border-slate-800 bg-slate-950 h-14"></div>
      </template>
    </ClientOnly>
  </aside>
</template>

<script setup>
import { translateRole } from '@/utils/roles'

const route = useRoute()
const { user, logout } = useAuth()
const isActive = path => route.path.startsWith(path)

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
