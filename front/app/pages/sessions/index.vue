<template>
  <div class="p-8 min-h-screen bg-gray-50">

    <!-- En-tête -->
    <div class="flex justify-between items-center mb-8">
      <div>
        <h1 class="text-2xl font-bold text-gray-800">Mes sessions</h1>
        <p class="text-sm text-gray-400 mt-1">Vos événements et rencontres planifiés</p>
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
          <!-- Badge participation (TICKET uniquement) -->
          <template v-if="session.categoryType === 'ISSUE'">
            <span v-if="session.myParticipationStatus === false"
              class="inline-flex items-center gap-1 text-[10px] font-semibold px-2 py-0.5 rounded-full bg-rose-50 text-rose-600 border border-rose-200">
              <i class="fa-solid fa-circle-xmark text-[8px]"></i>
              {{ session.createdBy === user?.name ? 'RDV refusé' : 'Refusé' }}
            </span>
            <span v-else-if="session.myParticipationStatus === null"
              class="inline-flex items-center gap-1 text-[10px] font-semibold px-2 py-0.5 rounded-full bg-amber-50 text-amber-600 border border-amber-200">
              <i class="fa-solid fa-hourglass-half text-[8px]"></i>
              {{ session.createdBy === user?.name ? 'En attente' : 'En attente' }}
            </span>
            <span v-else
              class="inline-flex items-center gap-1 text-[10px] font-semibold px-2 py-0.5 rounded-full bg-emerald-50 text-emerald-600 border border-emerald-200">
              <i class="fa-solid fa-circle-check text-[8px]"></i>
              {{ session.createdBy === user?.name ? 'RDV confirmé' : 'Accepté' }}
            </span>
          </template>
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

        <!-- Boutons accepter / refuser (RDV en attente uniquement, non passé, destinataire uniquement) -->
        <div
          v-if="session.categoryType === 'ISSUE' && session.myParticipationStatus === null && session.createdBy !== user?.name && !isPast(session.endDate)"
          class="flex gap-2 mt-auto pt-1"
          @click.stop
        >
          <UButton
            size="xs" color="success" variant="soft" icon="i-lucide-check"
            :loading="pendingResponse === session.id + '-accept'"
            @click.stop="respond(session, true)"
          >Accepter</UButton>
          <UButton
            size="xs" color="error" variant="soft" icon="i-lucide-x"
            :loading="pendingResponse === session.id + '-refuse'"
            @click.stop="respond(session, false)"
          >Refuser</UButton>
        </div>

        <!-- Bouton supprimer (prof / admin, sessions passées uniquement) -->
        <div
          v-if="isTeacherOrAdmin && isPast(session.endDate)"
          class="flex justify-end mt-auto pt-1"
          @click.stop
        >
          <UButton
            size="xs" color="error" variant="ghost" icon="i-lucide-trash-2"
            @click.stop="openDeleteConfirm(session)"
          ></UButton>
        </div>
      </div>
    </div>

    <!-- Modal de confirmation de suppression -->
    <UModal v-model:open="deleteConfirmOpen" title="Supprimer la session" :ui="{ width: 'sm:max-w-md' }">
      <template #body>
        <div class="space-y-3">
          <div class="flex items-center gap-3 p-4 bg-rose-50 rounded-xl border border-rose-200">
            <i class="fa-solid fa-triangle-exclamation text-rose-500 text-lg"></i>
            <p class="text-sm text-rose-700">
              Vous êtes sur le point de supprimer <strong>{{ sessionToDelete?.name }}</strong>.<br>
              Les participants seront notifiés de l'annulation.
            </p>
          </div>
        </div>
      </template>
      <template #footer>
        <div class="flex justify-end gap-2">
          <UButton color="neutral" variant="ghost" @click="deleteConfirmOpen = false">Annuler</UButton>
          <UButton color="error" variant="solid" icon="i-lucide-trash-2"
            :loading="deleting"
            @click="confirmDelete">
            Supprimer définitivement
          </UButton>
        </div>
      </template>
    </UModal>

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

          <!-- Statut participation (RDV) -->
          <div v-if="selectedSession.categoryType === 'ISSUE'" class="rounded-xl border px-4 py-3 flex items-center justify-between gap-3"
            :class="selectedSession.myParticipationStatus === true ? 'bg-emerald-50 border-emerald-200'
              : selectedSession.myParticipationStatus === false ? 'bg-rose-50 border-rose-200'
              : 'bg-amber-50 border-amber-200'"
          >
            <div class="flex items-center gap-2">
              <i :class="selectedSession.myParticipationStatus === true ? 'fa-solid fa-circle-check text-emerald-500'
                : selectedSession.myParticipationStatus === false ? 'fa-solid fa-circle-xmark text-rose-500'
                : 'fa-solid fa-hourglass-half text-amber-500'"
              ></i>
              <span class="text-sm font-semibold"
                :class="selectedSession.myParticipationStatus === true ? 'text-emerald-700'
                  : selectedSession.myParticipationStatus === false ? 'text-rose-700'
                  : 'text-amber-700'"
              >
                <template v-if="selectedSession.createdBy === user?.name">
                  {{ selectedSession.myParticipationStatus === true ? 'RDV confirmé par le professeur'
                    : selectedSession.myParticipationStatus === false ? 'RDV refusé par le professeur'
                    : 'En attente de confirmation du professeur' }}
                </template>
                <template v-else>
                  {{ selectedSession.myParticipationStatus === true ? 'Vous avez accepté ce RDV'
                    : selectedSession.myParticipationStatus === false ? 'Vous avez refusé ce RDV'
                    : 'En attente de votre réponse' }}
                </template>
              </span>
            </div>
            <!-- Boutons dans le modal pour répondre (prof uniquement, si en attente) -->
            <div v-if="selectedSession.myParticipationStatus === null && selectedSession.createdBy !== user?.name && !isPast(selectedSession.endDate)"
              class="flex gap-2" @click.stop>
              <UButton size="xs" color="success" variant="solid" icon="i-lucide-check"
                :loading="pendingResponse === selectedSession.id + '-accept'"
                @click.stop="respondFromModal(true)">
                Accepter
              </UButton>
              <UButton size="xs" color="error" variant="soft" icon="i-lucide-x"
                :loading="pendingResponse === selectedSession.id + '-refuse'"
                @click.stop="respondFromModal(false)">
                Refuser
              </UButton>
            </div>
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
const pendingResponse = ref(null)
const deleteConfirmOpen = ref(false)
const sessionToDelete = ref(null)
const deleting = ref(false)

const isTeacherOrAdmin = computed(() =>
  user.value?.roles?.includes('TEACHER') || user.value?.roles?.includes('ADMIN')
)

const filters = [
  { value: 'upcoming', label: 'À venir', icon: 'fa-solid fa-clock' },
  { value: 'past',     label: 'Passées',  icon: 'fa-solid fa-history' },
]

const loadSessions = async () => {
  loading.value = true
  error.value = null
  try {
    sessions.value = await api('/events/my-sessions')
  } catch (e) {
    error.value = e?.data?.message ?? 'Impossible de charger vos sessions.'
  } finally {
    loading.value = false
  }
}

const REFRESH_NOTIF_TYPES = ['NEW_EVENT_INVITATION', 'EVENT_RESPONSE', 'UPDATE_EVENT', 'EVENT_CANCEL']
const onNotificationReceived = (e) => {
  if (REFRESH_NOTIF_TYPES.includes(e.detail?.type)) {
    loadSessions()
  }
}

onMounted(async () => {
  await loadSessions()
  window.addEventListener('notification:received', onNotificationReceived)
})

onUnmounted(() => {
  window.removeEventListener('notification:received', onNotificationReceived)
})

/** Répondre à un RDV (depuis la carte) */
const respond = async (session, accepted) => {
  const key = `${session.id}-${accepted ? 'accept' : 'refuse'}`
  pendingResponse.value = key
  try {
    await api(`/events/${session.id}/invitations/respond`, {
      method: 'PUT',
      body: { response: accepted, raison: null },
    })
    // Mettre à jour localement sans recharger
    const idx = sessions.value.findIndex(s => s.id === session.id)
    if (idx !== -1) {
      sessions.value[idx] = {
        ...sessions.value[idx],
        myParticipationStatus: accepted,
        // Si le prof accepte, l'event devient validé → apparaît dans le calendrier
        validated: accepted ? true : sessions.value[idx].validated,
      }
    }
  } catch (e) {
    console.error('Erreur réponse invitation', e)
  } finally {
    pendingResponse.value = null
  }
}

/** Répondre depuis le modal de détail */
const respondFromModal = async (accepted) => {
  if (!selectedSession.value) return
  await respond(selectedSession.value, accepted)
  // Mettre à jour la session sélectionnée aussi
  if (selectedSession.value) {
    selectedSession.value = {
      ...selectedSession.value,
      myParticipationStatus: accepted,
      validated: accepted ? true : selectedSession.value.validated,
    }
  }
}

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
  ISSUE:   'Ticket',
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

const openDeleteConfirm = (session) => {
  sessionToDelete.value = session
  deleteConfirmOpen.value = true
}

const confirmDelete = async () => {
  if (!sessionToDelete.value) return
  deleting.value = true
  try {
    await api(`/events/${sessionToDelete.value.id}/cancel`, { method: 'DELETE' })
    sessions.value = sessions.value.filter(s => s.id !== sessionToDelete.value.id)
    if (selectedSession.value?.id === sessionToDelete.value.id) {
      detailOpen.value = false
      selectedSession.value = null
    }
    deleteConfirmOpen.value = false
    sessionToDelete.value = null
  } catch (e) {
    console.error('Erreur suppression session', e)
  } finally {
    deleting.value = false
  }
}
</script>
