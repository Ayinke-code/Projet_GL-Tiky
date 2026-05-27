<template>
  <div class="p-8 min-h-screen bg-gray-50">

    <!-- En-tête -->
    <div class="flex justify-between items-center mb-8">
      <div>
        <h1 class="text-2xl font-bold text-gray-800">Mes sessions</h1>
        <p class="text-sm text-gray-400 mt-1">Vos rencontres planifiées avec vos enseignants</p>
      </div>

      <!-- Filtre passé / à venir -->
      <div class="flex gap-2">
        <button
          v-for="f in filters"
          :key="f.value"
          @click="activeFilter = f.value"
          class="px-4 py-2 rounded-full text-sm font-semibold transition-all cursor-pointer"
          :class="activeFilter === f.value
            ? 'bg-indigo-600 text-white shadow'
            : 'bg-white text-gray-500 border border-gray-200 hover:bg-gray-50'"
        >
          <i :class="f.icon" class="mr-1.5"></i>{{ f.label }}
        </button>
      </div>
    </div>

    <!-- Chargement -->
    <div v-if="loading" class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
      <USkeleton v-for="n in 6" :key="n" class="h-48 rounded-2xl" />
    </div>

    <!-- Erreur -->
    <div v-else-if="error" class="text-center py-16 text-red-400">
      <i class="fa-solid fa-circle-exclamation text-4xl mb-3 block"></i>
      <p>{{ error }}</p>
    </div>

    <!-- Aucune session -->
    <div v-else-if="filteredSessions.length === 0" class="text-center py-20">
      <div class="text-gray-200 text-6xl mb-4">
        <i class="fa-solid fa-calendar-xmark"></i>
      </div>
      <p class="text-gray-400 text-lg">
        {{ activeFilter === 'upcoming' ? 'Aucune session à venir' : 'Aucune session passée' }}
      </p>
    </div>

    <!-- Grille des sessions -->
    <div v-else class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
      <div
        v-for="session in filteredSessions"
        :key="session.id"
        @click="openDetail(session)"
        class="bg-white rounded-2xl shadow-sm border border-gray-100 p-5 hover:shadow-md hover:border-indigo-200 transition-all cursor-pointer flex flex-col gap-3"
        :class="isPast(session.endDate) ? 'opacity-70' : ''"
      >
        <!-- Type + badge statut -->
        <div class="flex items-start justify-between gap-2">
          <span
            class="inline-flex items-center gap-1.5 text-[10px] font-bold uppercase tracking-widest px-2.5 py-1 rounded-full"
            :class="typeStyle(session.categoryType).badge"
          >
            <i :class="typeStyle(session.categoryType).icon" class="text-[9px]"></i>
            {{ typeLabel(session.categoryType) }}
          </span>
          <span
            class="text-[10px] font-semibold px-2 py-0.5 rounded-full"
            :class="isPast(session.endDate)
              ? 'bg-gray-100 text-gray-400'
              : isToday(session.startDate)
              ? 'bg-amber-100 text-amber-600'
              : 'bg-emerald-100 text-emerald-600'"
          >
            {{ isPast(session.endDate) ? 'Passée' : isToday(session.startDate) ? "Aujourd'hui" : 'À venir' }}
          </span>
        </div>

        <!-- Nom de l'événement -->
        <div>
          <h3 class="text-base font-bold text-gray-900 leading-tight">{{ session.name }}</h3>
          <p v-if="session.description" class="text-xs text-gray-400 mt-0.5 line-clamp-1">{{ session.description }}</p>
        </div>

        <!-- Dates & heures -->
        <div
          v-if="session.startDate"
          class="rounded-xl px-3 py-2.5 text-xs"
          :class="typeStyle(session.categoryType).bg"
        >
          <div class="flex items-center gap-1.5 font-semibold mb-1.5">
            <i class="fa-solid fa-calendar-days text-[10px]"></i>
            <span class="capitalize">{{ formatDate(session.startDate) }}</span>
          </div>
          <div class="grid grid-cols-2 gap-2">
            <div class="flex flex-col gap-0.5">
              <span class="uppercase tracking-wide text-[9px] opacity-60 font-semibold">Début</span>
              <span class="flex items-center gap-1 font-medium">
                <i class="fa-solid fa-clock text-[10px]"></i>
                {{ formatTime(session.startDate) }}
              </span>
            </div>
            <div class="flex flex-col gap-0.5">
              <span class="uppercase tracking-wide text-[9px] opacity-60 font-semibold">Fin</span>
              <span class="flex items-center gap-1 font-medium">
                <i class="fa-regular fa-clock text-[10px]"></i>
                {{ formatTime(session.endDate) }}
              </span>
            </div>
          </div>
        </div>

        <!-- Groupe + enseignants -->
        <div v-if="myGroups(session).length" class="space-y-1.5">
          <div
            v-for="g in myGroups(session)"
            :key="g.id"
            class="rounded-xl bg-slate-50 border border-slate-100 px-3 py-2 text-xs"
          >
            <p class="font-semibold text-slate-700 flex items-center gap-1.5 mb-1">
              <i class="fa-solid fa-rocket text-[10px] text-slate-400"></i>
              {{ g.name }}
              <span v-if="g.categoryName" class="text-slate-400 font-normal">· {{ g.categoryName }}</span>
            </p>
            <div v-if="Array.from(g.teachers ?? []).length" class="flex flex-wrap gap-1">
              <span
                v-for="teacher in [...(g.teachers ?? [])].sort((a, b) => (a.teacherType === 'OPTION_LEADER' ? -1 : b.teacherType === 'OPTION_LEADER' ? 1 : 0))"
                :key="teacher.name"
                class="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[11px] font-medium"
                :class="teacher.teacherType === 'OPTION_LEADER'
                  ? 'bg-amber-50 text-amber-700 border border-amber-200'
                  : 'bg-violet-50 text-violet-700 border border-violet-100'"
              >
                <i v-if="teacher.teacherType === 'OPTION_LEADER'" class="fa-solid fa-star text-[8px]"></i>
                <i v-else class="fa-solid fa-chalkboard-teacher text-[9px]"></i>
                {{ teacher.name }}
              </span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Modal de détail -->
    <UModal v-model:open="detailOpen" :title="selectedSession?.name" :ui="{ width: 'sm:max-w-lg' }">
      <template #body>
        <div v-if="selectedSession" class="space-y-4">

          <!-- Motif -->
          <div class="flex items-center gap-2">
            <span
              class="inline-flex items-center gap-1.5 text-xs font-bold uppercase tracking-widest px-3 py-1.5 rounded-full"
              :class="typeStyle(selectedSession.categoryType).badge"
            >
              <i :class="typeStyle(selectedSession.categoryType).icon"></i>
              {{ typeLabel(selectedSession.categoryType) }}
            </span>
            <span class="text-sm text-gray-500">· {{ selectedSession.categoryName }}</span>
          </div>

          <!-- Description -->
          <p v-if="selectedSession.description" class="text-sm text-gray-600 leading-relaxed">
            {{ selectedSession.description }}
          </p>

          <!-- Dates & modalités -->
          <div class="rounded-xl border border-gray-100 bg-gray-50 p-4 space-y-3">
            <p class="text-[10px] font-bold uppercase tracking-widest text-gray-400">Modalités de la session</p>

            <div class="flex items-center gap-2 text-sm text-gray-700">
              <i class="fa-solid fa-calendar-days text-indigo-400 w-4"></i>
              <span class="capitalize font-medium">{{ formatDate(selectedSession.startDate) }}</span>
            </div>

            <div class="grid grid-cols-2 gap-3">
              <div class="flex items-center gap-2 text-sm text-gray-700">
                <i class="fa-solid fa-clock text-emerald-400 w-4"></i>
                <div>
                  <p class="text-[10px] text-gray-400 uppercase tracking-wide">Début</p>
                  <p class="font-semibold">{{ formatTime(selectedSession.startDate) }}</p>
                </div>
              </div>
              <div class="flex items-center gap-2 text-sm text-gray-700">
                <i class="fa-regular fa-clock text-rose-400 w-4"></i>
                <div>
                  <p class="text-[10px] text-gray-400 uppercase tracking-wide">Fin</p>
                  <p class="font-semibold">{{ formatTime(selectedSession.endDate) }}</p>
                </div>
              </div>
            </div>

            <div class="flex items-center gap-2 text-sm text-gray-500">
              <i class="fa-solid fa-hourglass-half text-amber-400 w-4"></i>
              <span>Durée : <strong class="text-gray-700">{{ duration(selectedSession.startDate, selectedSession.endDate) }}</strong></span>
            </div>
          </div>

          <!-- Groupes & enseignants -->
          <div v-if="selectedSession.projectGroups?.length" class="space-y-3">
            <p class="text-[10px] font-bold uppercase tracking-widest text-gray-400">Groupes & enseignants</p>
            <div
              v-for="g in selectedSession.projectGroups"
              :key="g.id"
              class="rounded-xl border border-slate-100 bg-slate-50 p-3"
            >
              <div class="flex items-center gap-1.5 mb-2">
                <i class="fa-solid fa-rocket text-slate-400 text-xs"></i>
                <p class="text-sm font-semibold text-slate-800">{{ g.name }}</p>
                <span v-if="g.parentGroupName" class="text-xs text-slate-400">
                  · <i class="fa-solid fa-code-branch text-[9px]"></i> {{ g.parentGroupName }}
                </span>
              </div>

              <div v-if="Array.from(g.teachers ?? []).length" class="flex flex-wrap gap-1.5">
                <span
                  v-for="teacher in [...(g.teachers ?? [])].sort((a, b) => (a.teacherType === 'OPTION_LEADER' ? -1 : b.teacherType === 'OPTION_LEADER' ? 1 : 0))"
                  :key="teacher.name"
                  class="inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-xs font-medium"
                  :class="teacher.teacherType === 'OPTION_LEADER'
                    ? 'bg-amber-50 text-amber-700 border border-amber-200'
                    : 'bg-violet-50 text-violet-700 border border-violet-200'"
                >
                  <i v-if="teacher.teacherType === 'OPTION_LEADER'" class="fa-solid fa-star text-[9px]"></i>
                  <i v-else class="fa-solid fa-chalkboard-teacher text-[10px]"></i>
                  {{ teacher.name }}
                  <span v-if="teacher.teacherType === 'OPTION_LEADER'" class="opacity-60 text-[9px] uppercase tracking-wide">Option Leader</span>
                </span>
              </div>
              <p v-else class="text-xs text-gray-400">Aucun enseignant assigné</p>
            </div>
          </div>

          <!-- Organisateur -->
          <p class="text-xs text-gray-400 text-right">
            <i class="fa-solid fa-user mr-1"></i>Organisé par {{ selectedSession.createdBy }}
          </p>
        </div>
      </template>
    </UModal>

  </div>
</template>

<script setup>
definePageMeta({ ssr: false })

import { formatDate, formatTime } from '~/utils/helpers'

const api = useApi()
const { user } = useAuth()

const loading = ref(false)
const error = ref(null)
const sessions = ref([])
const activeFilter = ref('upcoming')
const detailOpen = ref(false)
const selectedSession = ref(null)

const filters = [
  { value: 'upcoming', label: 'À venir', icon: 'fa-solid fa-clock' },
  { value: 'past',     label: 'Passées',  icon: 'fa-solid fa-history' },
]

onMounted(async () => {
  loading.value = true
  try {
    sessions.value = await api('/events/my-sessions')
  } catch (e) {
    error.value = e?.data?.message ?? 'Impossible de charger vos sessions.'
  } finally {
    loading.value = false
  }
})

const isPast   = (date) => date && new Date(date) < new Date()
const isToday  = (date) => {
  if (!date) return false
  const d = new Date(date)
  const n = new Date()
  return d.getFullYear() === n.getFullYear() && d.getMonth() === n.getMonth() && d.getDate() === n.getDate()
}

const filteredSessions = computed(() =>
  sessions.value.filter(s =>
    activeFilter.value === 'upcoming' ? !isPast(s.endDate) : isPast(s.endDate)
  )
)

/** Groupes de la session qui contiennent l'étudiant courant (simplifié : on affiche tous) */
const myGroups = (session) => session.projectGroups ?? []

const duration = (start, end) => {
  if (!start || !end) return '—'
  const diff = Math.round((new Date(end) - new Date(start)) / 60000)
  if (diff < 60) return `${diff} min`
  const h = Math.floor(diff / 60)
  const m = diff % 60
  return m ? `${h}h${String(m).padStart(2, '0')}` : `${h}h`
}

const typeLabel = (type) => ({
  AUDIT:   'Audit',
  DEFENSE: 'Soutenance',
  MEETING: 'Réunion',
  ISSUE:   'Issue',
}[type] ?? type)

const typeStyle = (type) => ({
  AUDIT:   { badge: 'bg-indigo-100 text-indigo-700', bg: 'bg-indigo-50 text-indigo-600', icon: 'fa-solid fa-magnifying-glass' },
  DEFENSE: { badge: 'bg-emerald-100 text-emerald-700', bg: 'bg-emerald-50 text-emerald-600', icon: 'fa-solid fa-flag-checkered' },
  MEETING: { badge: 'bg-amber-100 text-amber-700', bg: 'bg-amber-50 text-amber-600', icon: 'fa-solid fa-handshake' },
  ISSUE:   { badge: 'bg-rose-100 text-rose-700', bg: 'bg-rose-50 text-rose-600', icon: 'fa-solid fa-circle-exclamation' },
}[type] ?? { badge: 'bg-gray-100 text-gray-600', bg: 'bg-gray-50 text-gray-600', icon: 'fa-solid fa-calendar' })

const openDetail = (session) => {
  selectedSession.value = session
  detailOpen.value = true
}
</script>
