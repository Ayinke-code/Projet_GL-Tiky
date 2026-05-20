<script setup lang="js">
/**
 * Page détail d'une issue
 * - charge une issue par ID
 * - permet mise à jour status + visibilité
 * - gère les messages et conversations
 */

import { ref, onMounted, onUnmounted } from 'vue'

const route = useRoute()
const { ready, user, wsToken } = useAuth()
const { getIssueById, updateIssueStatus, updateIssuePrivacy } = useIssues()
const { getIssueMessages, sendIssueMessage, normalizeMessage, initializeConversation } = useConversations()
const { followIssue } = useIssueFollowers()

// ID de l'issue depuis l'URL
const issueId = Number(route.params.id)

// state local
const issue = ref(null)
const loading = ref(true)
const error = ref(null)
const messages = ref([])
const newMessage = ref('')
const sending = ref(false)
const socket = ref(null)

const showFollowModal = ref(false)
const alreadyFollowing = ref(false)
const followSuccess = ref(false)

/**
 * Callback après suivi réussi
 */
const onFollowed = () => {
  alreadyFollowing.value = true
  followSuccess.value = true
  setTimeout(() => followSuccess.value = false, 3000)
}

/**
 * Connecte au WebSocket pour les mises à jour en temps réel
 */
const connectWebSocket = () => {
  if (process.server) return
  if (socket.value) socket.value.close()

  const config = useRuntimeConfig()

  const token = wsToken.value || localStorage.getItem('wsToken')
  if (!token) return

  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
  const apiBase = config.public.apiBase
  const host = apiBase.startsWith('/') ? window.location.host + apiBase : new URL(apiBase).host + new URL(apiBase).pathname
  const wsUrl = `${protocol}//${host}/ws/issue?token=${token}&issueId=${issueId}`

  socket.value = new WebSocket(wsUrl)

  socket.value.onmessage = (event) => {
    try {
      const data = JSON.parse(event.data)
      if (data.type === 'new_message' && data.message) {
        const normalized = normalizeMessage(data.message)
        if (!messages.value.find((m) => m.id === normalized.id)) {
          messages.value.push(normalized)
        }
      }
    } catch (e) {
      console.error('Erreur WebSocket:', e)
    }
  }

  socket.value.onerror = () => {
    console.error('Erreur WebSocket')
    socket.value?.close()
  }
}

/**
 * Chargement initial de l'issue et des messages
 */
onMounted(async () => {
  // redirection si non connecté
  if (ready && !user?.value?.id) {
    await navigateTo('/login')
    return
  }

  try {
    issue.value = await getIssueById(issueId)
    await initializeConversation(issueId)
    messages.value = await getIssueMessages(issueId)
    connectWebSocket()
  } catch (err) {
    error.value = err?.message || 'Erreur de chargement'
  } finally {
    loading.value = false
  }
})

/**
 * Nettoie la connexion WebSocket à la fermeture
 */
onUnmounted(() => {
  socket.value?.close()
})

/**
 * Marquer l'issue comme terminée
 */
const handleMarkResolved = async () => {
  try {
    await updateIssueStatus(issueId, 'TERMINE')

    // mise à jour locale optimiste
    if (issue.value) {
      issue.value.status = 'TERMINE'
    }
  } catch (err) {
    console.error(err)
  }
}

/**
 * Couleurs des statuts (aligné backend)
 */
const statutColor = (statut) => {
  const map = {
    'En cours': 'bg-blue-100 text-blue-700',
    'résolu': 'bg-green-100 text-green-700',
    'Ouvert': 'bg-yellow-100 text-yellow-700',
    'Fermé': 'bg-gray-100 text-gray-600',
  }
  return map[statut] || 'bg-gray-100 text-gray-600'
}

/**
 * Modals UI
 */
const isResolveModalOpen = ref(false)
const isVisibilityModalOpen = ref(false)

/**
 * Toggle visibilité (public / privé)
 */
const toggleVisibility = async () => {
  try {
    await updateIssuePrivacy(issueId)

    if (issue.value) {
      issue.value.private = !issue.value.private
    }
  } catch (err) {
    console.error(err)
  }
}

/**
 * Envoie un nouveau message
 */
const handleSend = async () => {
  if (!newMessage.value.trim()) return
  if (newMessage.value.length > 4000) {
    error.value = 'Le message ne doit pas dépasser 4000 caractères'
    return
  }

  sending.value = true
  try {
    const sentMessage = await sendIssueMessage(issueId, newMessage.value)
    if (!messages.value.find((m) => m.id === sentMessage.id)) {
      messages.value.push(sentMessage)
    }
    newMessage.value = ''
  } catch (err) {
    error.value = err?.message || 'Erreur lors de l\'envoi du message'
  } finally {
    sending.value = false
  }
}
</script>

<template>
  <div class="min-h-screen bg-gray-50">
    <!-- Loader -->
    <div v-if="loading" class="flex items-center justify-center min-h-[60vh]">
      <div class="flex flex-col items-center gap-3 text-gray-400">
        <svg class="animate-spin h-8 w-8" fill="none" viewBox="0 0 24 24">
          <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" />
          <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8z" />
        </svg>
        <span class="text-sm">Chargement du ticket...</span>
      </div>
    </div>

    <!-- Erreur -->
    <div v-else-if="error" class="p-8">
      <UAlert color="red" :description="error" />
    </div>

    <!-- Contenu principal -->
    <div v-else class="max-w-7xl mx-auto px-4 py-6 space-y-4">

      <!-- Breadcrumb -->
      <div class="flex items-center gap-2 text-sm text-gray-500">
        <NuxtLink to="/issues" class="hover:text-gray-800 flex items-center gap-1 transition-colors">
          <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24"
               stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                  d="M10 19l-7-7m0 0l7-7m-7 7h18" />
          </svg>
          Retour aux tickets
        </NuxtLink>
        <span class="text-gray-300">/</span>
        <span class="text-gray-700 font-medium">Ticket #{{ issueId }}</span>
      </div>

      <!-- Header : titre + actions -->
      <div class="flex flex-col sm:flex-row sm:items-start sm:justify-between gap-3">
        <div class="space-y-2">
          <h1 class="text-2xl font-bold text-gray-900">{{ issue.title }}</h1>
          <div class="flex flex-wrap items-center gap-2">
            <!-- Catégorie -->
            <span v-if="issue?.categoryId && issue?.categoryName"
                  class="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-semibold bg-emerald-100 text-emerald-700">
                            {{ issue.categoryName }}
                        </span>
            <!-- Statut -->
            <span class="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-semibold"
                  :class="statutColor(issue.status)">
                            {{ issue.status }}
                        </span>
            <!-- Visibilité -->
            <span class="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-semibold"
                  :class="!issue.private ? 'text-blue-600 bg-white' : 'text-red-600 bg-white'">
                            <svg xmlns="http://www.w3.org/2000/svg" class="h-3 w-3" fill="none" viewBox="0 0 24 24"
                                 stroke="currentColor">
                                <path v-if="!issue.private" stroke-linecap="round" stroke-linejoin="round"
                                      stroke-width="2"
                                      d="M12 2a10 10 0 1 0 0 20 10 10 0 1 0 0-20M2 12h20M12 2a15.3 15.3 0 0 1 4 10 15.3 15.3 0 0 1-4 10 15.3 15.3 0 0 1-4-10 15.3 15.3 0 0 1 4-10z" />
                                <path v-else stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                                      d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z" />
                            </svg>
                            {{ issue.private ? 'Privé' : 'Public' }}
                        </span>
          </div>
        </div>

        <!-- Boutons d'action -->
        <div class="flex items-center gap-2 shrink-0">
          <button v-if="issue.status !== 'TERMINE'" @click="isResolveModalOpen = true"
                  :disabled="issue.status === 'résolu'"
                  class="inline-flex items-center gap-1.5 px-3 py-2 rounded-lg border border-emerald-300 text-emerald-700 bg-white hover:bg-emerald-50 text-sm font-medium transition-colors disabled:opacity-40 disabled:cursor-not-allowed cursor-pointer transition-transform duration-200 hover:scale-105">
            <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24"
                 stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
            </svg>
            Marquer Résolu
          </button>
          <button @click="isVisibilityModalOpen = true"
                  class="inline-flex items-center gap-1.5 px-3 py-2 rounded-lg border text-sm font-medium transition-colors transition-transform duration-200 hover:scale-105 cursor-pointer"
                  :class="issue.private
                            ? 'border-blue-300 text-blue-600 bg-white hover:bg-blue-50'
                            : 'border-red-300 text-red-600 bg-white hover:bg-red-50'">
            <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24"
                 stroke="currentColor">
              <path v-if="issue.private" stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                    d="M12 2a10 10 0 1 0 0 20 10 10 0 1 0 0-20M2 12h20M12 2a15.3 15.3 0 0 1 4 10 15.3 15.3 0 0 1-4 10 15.3 15.3 0 0 1-4-10 15.3 15.3 0 0 1 4-10z" />
              <path v-else stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                    d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z" />
            </svg>
            {{ issue.private ? 'Rendre Public' : 'Rendre Privé' }}
          </button>

          <!--  Bouton Suivre / badge Suivi -->
          <button
            v-if="!alreadyFollowing"
            @click="showFollowModal = true"
            class="inline-flex items-center gap-1.5 px-3 py-2 rounded-lg border border-indigo-300 text-indigo-700 bg-white hover:bg-indigo-50 text-sm font-medium transition-colors cursor-pointer transition-transform duration-200 hover:scale-105">
            <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6 6 0 10-12 0v3.159c0 .538-.214 1.055-.595 1.437L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9" />
            </svg>
            Suivre
          </button>
          <span
            v-else
            class="inline-flex items-center gap-1.5 px-3 py-2 rounded-lg border border-green-300 text-green-700 bg-green-50 text-sm font-medium">
                        ✓ Suivi
                    </span>

        </div>
      </div>

      <!--Message succès après suivi -->
      <div v-if="followSuccess" class="rounded-lg bg-green-50 border border-green-200 px-4 py-2 text-sm text-green-700">
        Vous suivez maintenant cette issue !
      </div>

      <!-- Grid principal -->
      <div class="grid grid-cols-1 lg:grid-cols-3 gap-6 items-start">

        <!-- ===== COLONNE GAUCHE : CONVERSATION ===== -->
        <div class="lg:col-span-2 space-y-4">
          <!-- Messages -->
          <div class="space-y-3">
            <IssueMessage
              v-for="msg in messages"
              :key="msg.id"
              :auteur="msg.auteur"
              :role="msg.role"
              :date="msg.date"
              :contenu="msg.contenu"
              :is-mine="msg.userId === user?.id"
            />

            <!-- État vide -->
            <IssueEmptyMessages v-if="messages.length === 0" />
          </div>

          <!-- Zone d'envoi de message -->
          <MessageInput
            v-model="newMessage"
            :sending="sending"
            @send="handleSend"
          />
        </div>

        <!-- ===== COLONNE DROITE : SIDEBAR ===== -->
        <div class="space-y-4">

          <!-- Carte INFORMATIONS -->
          <div class="bg-white rounded-xl border border-gray-200 overflow-hidden">
            <div class="px-5 py-3 border-b border-gray-100">
              <h2 class="text-xs font-bold text-gray-500 uppercase tracking-widest">Informations</h2>
            </div>
            <div class="px-5 py-4 space-y-3 text-sm">
              <div class="flex justify-between items-center">
                <span class="text-gray-500">Auteur</span>
                <span class="font-medium text-gray-900">{{ issue.createdName || 'Inconnu' }}</span>
              </div>
              <!-- TODO -->
              <div v-if="false" class="flex justify-between items-center">
                <span class="text-gray-500">Enseignant en charge</span>
                <span class="font-semibold text-gray-900">{{ issue.professeur || 'Non défini' }}</span>
              </div>
              <!-- TODO -->
              <div v-if="false" class="flex justify-between items-center">
                <span class="text-gray-500">Abonnés</span>
                <span class="font-medium text-gray-900">{{ issue.abonnes || 0 }}</span>
              </div>
            </div>
          </div>

          <!-- Carte RDV -->
          <div v-if="false" class="bg-white rounded-xl border border-gray-200 overflow-hidden">
            <div class="px-5 py-3 border-b border-gray-100">
              <div class="flex items-center gap-2">
                <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4 text-indigo-500" fill="none"
                     viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                        d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" />
                </svg>
                <h2 class="text-sm font-semibold text-gray-800">Besoin d'un RDV ?</h2>
              </div>
            </div>

            <div class="px-5 py-4">
              <div v-if="rdvProposal?.active" class="space-y-3">
                <div class="bg-amber-50 border border-amber-200 rounded-lg p-3 space-y-2">
                  <p class="text-xs font-bold text-amber-700 uppercase tracking-wide">Nouvelle demande</p>
                  <p class="text-sm text-gray-600">
                    <span class="font-medium">{{ rdvProposal.auteur }}</span> propose un point le :
                  </p>
                  <p class="text-center font-mono text-sm font-semibold text-gray-800 bg-white border border-amber-200 rounded py-2 px-3">
                    {{ rdvProposal.date }}
                  </p>
                </div>
                <div class="grid grid-cols-2 gap-2">
                  <button @click="handleRdvValider"
                          class="w-full py-2 rounded-lg bg-emerald-600 hover:bg-emerald-700 text-white text-sm font-medium transition-colors transition-transform duration-200 hover:scale-105">
                    Valider
                  </button>
                  <button @click="handleRdvRefuser"
                          class="w-full py-2 rounded-lg border border-red-200 text-red-600 hover:bg-red-50 text-sm font-medium transition-colors transition-transform duration-200 hover:scale-105">
                    Refuser
                  </button>
                </div>
              </div>
              <div v-else class="text-center space-y-3 py-2">
                <p class="text-sm text-gray-400">Aucune demande de rendez-vous en cours.</p>
                <button disabled
                        class="w-full py-2 rounded-lg bg-gray-100 text-gray-400 text-sm font-medium cursor-not-allowed">
                  Proposer un créneau
                </button>
              </div>
            </div>
          </div>

          <!-- Slot pour futures fonctionnalités sidebar -->
          <slot name="sidebar-extra" />

        </div>
      </div>
    </div>
  </div>

  <!-- Modal Résolu -->
  <div v-if="isResolveModalOpen" class="fixed inset-0 bg-black/40 flex items-center justify-center z-50 p-4"
       @click.self="isResolveModalOpen = false">
    <div class="bg-white rounded-xl shadow-2xl w-full max-w-md overflow-hidden animate-in fade-in zoom-in duration-200">
      <div class="p-6 border-b border-gray-100 bg-gray-50 flex justify-between items-center">
        <h2 class="text-lg font-bold text-gray-800">Confirmer</h2>
        <button @click="isResolveModalOpen = false" class="text-gray-400 hover:text-gray-600">✕</button>
      </div>
      <div class="p-6 space-y-4">
        <p class="text-sm text-gray-600">
          Êtes-vous sûr de vouloir marquer ce ticket comme <span class="font-semibold text-emerald-600">résolu</span> ?
        </p>
        <div class="flex justify-end gap-2">
          <button @click="isResolveModalOpen = false"
                  class="px-4 py-2 text-sm rounded-lg border border-gray-300 text-gray-600 hover:bg-gray-50">
            Annuler
          </button>
          <button @click="() => { handleMarkResolved(); isResolveModalOpen = false }"
                  class="px-4 py-2 text-sm rounded-lg bg-emerald-600 text-white hover:bg-emerald-700">
            Confirmer
          </button>
        </div>
      </div>
    </div>
  </div>

  <!-- Modal Visibilité -->
  <div v-if="isVisibilityModalOpen" class="fixed inset-0 bg-black/40 flex items-center justify-center z-50 p-4"
       @click.self="isVisibilityModalOpen = false">
    <div class="bg-white rounded-xl shadow-2xl w-full max-w-md overflow-hidden animate-in fade-in zoom-in duration-200">
      <div class="p-6 border-b border-gray-100 bg-gray-50 flex justify-between items-center">
        <h2 class="text-lg font-bold text-gray-800">Changer la visibilité</h2>
        <button @click="isVisibilityModalOpen = false" class="text-gray-400 hover:text-gray-600">✕</button>
      </div>
      <div class="p-6 space-y-4">
        <p class="text-sm text-gray-600">
          Voulez-vous rendre ce ticket <span class="font-semibold">{{ issue.private ? 'public' : 'privé' }}</span> ?
        </p>
        <div class="flex justify-end gap-2">
          <button @click="isVisibilityModalOpen = false"
                  class="px-4 py-2 text-sm rounded-lg border border-gray-300 text-gray-600 hover:bg-gray-50">
            Annuler
          </button>
          <button @click="() => { toggleVisibility(); isVisibilityModalOpen = false }"
                  class="px-4 py-2 text-sm rounded-lg bg-indigo-600 text-white hover:bg-indigo-700">
            Confirmer
          </button>
        </div>
      </div>
    </div>
  </div>

  <!-- Modale de suivi -->
  <FollowModal
    v-model="showFollowModal"
    :issue-id="issueId"
    @followed="onFollowed"
  />

</template>
