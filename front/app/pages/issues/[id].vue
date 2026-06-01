<script setup lang="js">
/**
 * Page détail d'une issue
 * - charge une issue par ID
 * - permet mise à jour status + visibilité
 * - gère les messages et conversations
 */

import { computed, ref, onMounted, onUnmounted } from 'vue'
import ConfirmModal from '~/components/ui/ConfirmModal.vue'
import IssueMeetingProposal from "~/components/issues/meeting/IssueMeetingProposal.vue";
import IssueMeetingDemand from "~/components/issues/meeting/IssueMeetingDemand.vue";

const route = useRoute()
const { ready, user, wsToken } = useAuth()
const { getIssueById, updateIssueStatus, updateIssuePrivacy } = useIssues()
const { getIssueMessages, sendIssueMessage, normalizeMessage, initializeConversation } = useConversations()
const { followIssue, unfollowIssue, isFollowing } = useIssueFollowers()

// ID de l'issue depuis l'URL
const issueId = Number(route.params.id)

// state local
const issue = ref(null)
const loading = ref(true)
const error = ref(null)
const messages = ref([])
const newMessage = ref('')
const replyingToMessage = ref(null)
const sending = ref(false)
const socket = ref(null)
const conversationId = ref(null)

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
  const apiBase = config.public.apiBase || ''

  const token = wsToken.value || localStorage.getItem('wsToken')
  if (!token || !conversationId.value) return

  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
  const isRelativeBase = apiBase.startsWith('/')
  const parsedBase = !isRelativeBase && apiBase ? new URL(apiBase) : null
  const host = parsedBase?.host || window.location.host
  const basePath = isRelativeBase
    ? apiBase.replace(/\/$/, '')
    : parsedBase?.pathname && parsedBase.pathname !== '/'
      ? parsedBase.pathname.replace(/\/$/, '')
      : ''
  const wsUrl = `${protocol}//${host}${basePath}/ws/conversation?token=${token}&conversationId=${conversationId.value}`

  socket.value = new WebSocket(wsUrl)

  socket.value.onmessage = (event) => {
    try {
      const data = JSON.parse(event.data)
      if (data.type === 'new_message' && data.message) {
        const normalized = normalizeMessage(data.message)
        if (!messages.value.find((m) => m.id === normalized.id)) {
          messages.value.push(normalized)
        }
      } else if (data.type === 'privacy_update' && Number(data.issueId) === issueId) {
        // Déclencher un check immédiat de l'accès
        console.log('[WebSocket] Notification de changement de visibilité reçue')
        checkAccess()
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
    alreadyFollowing.value = await isFollowing(issueId)
    const conversation = await initializeConversation(issueId)
    conversationId.value = conversation?.id ?? null
    messages.value = await getIssueMessages(issueId)
    connectWebSocket()

    // Écouter les événements de synchronisation pour vérifier l'accès
    if (process.client) {
      window.addEventListener('issue-sync', checkAccess)
    }

    // Premier check d'accès au montage pour éviter de rester sur une page interdite
    // si on y accède directement via l'URL
    if (issue.value?.private) {
      await checkAccess()
    }
  } catch (err) {
    error.value = err?.message || 'Erreur de chargement'
  } finally {
    loading.value = false
  }
})

/**
 * Vérifie si l'utilisateur a toujours accès à l'issue
 */
const checkAccess = async () => {
  // Si on est déjà en train de changer la visibilité, on évite le double check
  // ou on attend que l'action en cours se termine.
  // Mais ici, getIssueById est rapide.
  try {
    const updatedIssue = await getIssueById(issueId)

    // Si l'issue est privée, on vérifie les droits
    if (updatedIssue.private) {
      const currentUserId = user?.value?.id
      const hasAccess =
        currentUserId === updatedIssue.createdId ||
        currentUserId === updatedIssue.categoryManagerId ||
        currentUserId === updatedIssue.categoryOwnerId

      if (!hasAccess) {
        console.warn('[Issue Detail] Accès révoqué, redirection...')
        // On force un message d'erreur pour que l'utilisateur comprenne pourquoi il est redirigé
        error.value = 'Vous n\'avez plus accès à ce ticket (passé en privé).'
        setTimeout(async () => {
          await navigateTo('/issues', { replace: true })
        }, 2000)
        return
      }
    }

    // Mettre à jour l'issue locale si elle a changé (ex: statut changé par admin)
    // Cela permet d'avoir l'UI à jour sans refresh
    issue.value = updatedIssue
  } catch (err) {
    // Si l'API renvoie une erreur (403/404), on redirige
    console.error('[Issue Detail] Erreur lors de la vérification d\'accès:', err)
    await navigateTo('/issues', { replace: true })
  }
}

/**
 * Nettoie la connexion WebSocket à la fermeture
 */
onUnmounted(() => {
  socket.value?.close()
  if (process.client) {
    window.removeEventListener('issue-sync', checkAccess)
  }
})

/**
 * Marquer l'issue comme terminée
 */
const handleMarkResolved = async () => {
  try {
    await updateIssueStatus(issueId, 'TERMINE')

    // On rafraîchit l'issue locale pour être sûr d'avoir les données à jour
    issue.value = await getIssueById(issueId)
  } catch (err) {
    console.error(err)
  }
}

/**
 * Demander la réouverture d'un ticket 'terminé'
 */
const handleReopening = async () => {
    // TODO: US-6700: Demande de réouverture
}

/**
 * Couleurs des statuts (aligné backend)
 */
const getStatusBadgeClass = (status) => {
  const classes = {
    'OUVERT':'bg-yellow-100 text-yellow-800 border border-yellow-300',
    'EN_ATTENTE': 'bg-gray-100 text-gray-800 border border-gray-300',
    'INCOMPLET': 'bg-orange-100 text-orange-800 border border-orange-300',
    'TERMINE': 'bg-green-100 text-green-800 border border-green-300'
  }
  return classes[status] || 'bg-gray-100 text-gray-800 border border-gray-300'
}

/**
 * Labels statuts
 */
const getStatusLabel = (status) => {
  const labels = {
    'OUVERT': 'Ouvert',
    'EN_ATTENTE': 'En attente',
    'INCOMPLET': 'Incomplet',
    'TERMINE': 'Terminé'
  }
  return labels[status] || status
}

/**
 * Modals UI
 */
const isResolveModalOpen = ref(false)
const isReopeningModalOpen = ref(false)
const isVisibilityModalOpen = ref(false)

const canTogglePrivacy = computed(() => {
  const currentUserId = user?.value?.id
  if (!currentUserId || !issue.value) return false

  return currentUserId === issue.value.categoryOwnerId || currentUserId === issue.value.categoryManagerId
})

/**
 * Toggle visibilité (public / privé)
 */
const toggleVisibility = async () => {
  if (!canTogglePrivacy.value) return

  try {
    await updateIssuePrivacy(issueId)

    // On rafraîchit l'issue locale pour être sûr d'avoir les données à jour
    issue.value = await getIssueById(issueId)
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
    const sentMessage = await sendIssueMessage(issueId, newMessage.value, replyingToMessage.value?.id ?? null)
    // Avoid duplicates: the server broadcasts the new message via WebSocket,
    // which may arrive before or after the HTTP response. Only push the
    // message locally if we don't already have it (compare by id).
    if (!messages.value.find((m) => m.id === sentMessage.id)) {
      messages.value.push(sentMessage)
    }
    newMessage.value = ''
    replyingToMessage.value = null
  } catch (err) {
    error.value = err?.message || 'Erreur lors de l\'envoi du message'
  } finally {
    sending.value = false
  }
}

const handleUnfollow = async () => {
  try {
    await unfollowIssue(issueId)
    alreadyFollowing.value = false
  } catch (err) {
    console.error(err)
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
                    <h1 id="issue-title" class="text-2xl font-bold text-gray-900">{{ issue.title }}</h1>
                    <div class="flex flex-wrap items-center gap-2">
                        <!-- Catégorie -->
                        <span v-if="issue?.categoryId && issue?.categoryName"
                            class="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-semibold bg-emerald-100 text-emerald-700">
                            {{ issue.categoryName }}
                        </span>
                        <!-- Statut -->
                        <span class="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-semibold"
                            :class="getStatusBadgeClass(issue.status)">
                            {{getStatusLabel(issue.status) }}
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
                    <!--  -->
                    <!-- Résolu : TODO: Quand on aura les 'Issue Manager', on pourra bloquer ce bouton pour les autres -->
                    <!--  -->
                    <button
                        v-if="issue.status !== 'TERMINE' && user?.id === issue?.categoryManagerId && user.id !== null"
                        @click="isResolveModalOpen = true" :disabled="issue.status === 'résolu'"
                        class="inline-flex items-center gap-1.5 px-3 py-2 rounded-lg border border-emerald-300 text-emerald-700 bg-white hover:bg-emerald-50 text-sm font-medium transition-colors disabled:opacity-40 disabled:cursor-not-allowed cursor-pointer transition-transform duration-200 hover:scale-105">
                        <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24"
                            stroke="currentColor">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
                        </svg>
                        Marquer Résolu
                    </button>
                    <button
                        v-else-if="issue.status === 'TERMINE' && user?.id === issue?.createdId && user.id !== null"
                        @click="isReopeningModalOpen = true" :disabled="issue.status === 'résolu'"
                        class="inline-flex items-center gap-1.5 px-3 py-2 rounded-lg border border-emerald-300 text-emerald-700 bg-white hover:bg-emerald-50 text-sm font-medium transition-colors disabled:opacity-40 disabled:cursor-not-allowed cursor-pointer transition-transform duration-200 hover:scale-105">
                        <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24"
                            stroke="currentColor">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
                        </svg>
                        Rouvrir
                    </button>
                    <!--  -->
                    <!-- Privé/Public : TODO: Quand on aura les 'Issue Manager', on pourra bloquer ce bouton pour les autres -->
                    <!--  -->
                    <button v-if="canTogglePrivacy" @click="isVisibilityModalOpen = true"
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
          <button
            v-else
            @click="handleUnfollow"
            class="inline-flex items-center gap-1.5 px-3 py-2 rounded-lg border border-red-300 text-red-600 bg-white hover:bg-red-50 text-sm font-medium transition-colors cursor-pointer transition-transform duration-200 hover:scale-105">
            <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
            </svg>
            Ne plus suivre
          </button>

        </div>
      </div>

      <!--Message succès après suivi -->
      <div v-if="followSuccess" class="rounded-lg bg-green-50 border border-green-200 px-4 py-2 text-sm text-green-700">
        Vous suivez maintenant ce ticket !
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
                            :reply-to="msg.replyTo"
                            @reply="replyingToMessage = msg"
                        />

                        <!-- État vide -->
                        <IssueEmptyMessages v-if="messages.length === 0" />
                    </div>

                    <!-- Zone d'envoi de message -->
                    <MessageInput
                        v-model="newMessage"
                        :sending="sending"
                        :replying-to="replyingToMessage"
                        @cancel-reply="replyingToMessage = null"
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

                        <div v-if="issue?.categoryManagerId && issue?.categoryManagerId === issue?.createdId" class="flex justify-between items-center">
                          <span class="text-gray-500">Auteur & Manager</span>
                          <span class="font-medium text-gray-900">
                            {{ issue.createdId === user?.id ? 'Moi' : (issue.createdName || 'Inconnu') }}
                          </span>
                        </div>

                        <template v-else>
                          <div class="flex justify-between items-center">
                            <span class="text-gray-500">Auteur</span>
                            <span class="font-medium text-gray-900">
                              {{ issue.createdId === user?.id ? 'Moi' : (issue.createdName || 'Inconnu') }}
                            </span>
                          </div>

                          <div class="flex justify-between items-center">
                            <span class="text-gray-500">Manager en charge</span>
                            <span class="font-medium text-gray-900">
                              {{ issue?.categoryManagerId === user?.id ? 'Moi' : (issue?.teacherManagerName || issue?.categoryManagerName || 'Non défini') }}
                            </span>
                          </div>
                        </template>

                        <div class="flex justify-between items-center">
                          <span class="text-gray-500">Abonnés</span>
                          <span class="font-medium text-gray-900">{{ issue.followersCount || 0 }}</span>
                        </div>

                      </div>

                    </div>

                  <!-- Visible uniquement si l'utilisateur est le manager de l'issue -->
                  <IssueMeetingProposal
                    v-if="user?.id === issue?.categoryManagerId && user?.id !== null"
                    :issue="issue"
                  />
                  <!-- Visible pour tout les utilisatuer sauf le manager de l'issue -->
                  <IssueMeetingDemand
                    v-if="user?.id !== issue?.categoryManagerId && user?.id !== null"
                    :issue="issue"
                  />

                    <!-- Slot pour futures fonctionnalités sidebar -->
                  <slot name="sidebar-extra" />

        </div>
      </div>
    </div>
  </div>

  <!-- Modal Résolu -->
  <div v-if="isResolveModalOpen" class="fixed inset-0 bg-black/40 flex items-center justify-center z-50 p-4"
       @click.self="isResolveModalOpen = false">
    <div
      class="bg-white rounded-xl shadow-2xl w-full max-w-md overflow-hidden animate-in fade-in zoom-in duration-200">

      <div class="p-6 border-b border-gray-100 bg-gray-50 flex justify-between items-center">
        <h2 class="text-lg font-bold text-gray-800">Confirmer</h2>
        <button @click="isResolveModalOpen = false" class="text-gray-400 hover:text-gray-600">
          ✕
        </button>
      </div>

      <div class="p-6 space-y-4">
        <p class="text-sm text-gray-600">
          Êtes-vous sûr de vouloir marquer ce ticket comme <span
          class="font-semibold text-emerald-600">résolu</span> ?
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
  <div v-if="isVisibilityModalOpen && canTogglePrivacy" class="fixed inset-0 bg-black/40 flex items-center justify-center z-50 p-4"
       @click.self="isVisibilityModalOpen = false">
    <div
      class="bg-white rounded-xl shadow-2xl w-full max-w-md overflow-hidden animate-in fade-in zoom-in duration-200">

      <div class="p-6 border-b border-gray-100 bg-gray-50 flex justify-between items-center">
        <h2 class="text-lg font-bold text-gray-800">Changer la visibilité</h2>
        <button @click="isVisibilityModalOpen = false" class="text-gray-400 hover:text-gray-600">
          ✕
        </button>
      </div>

      <div class="p-6 space-y-4">
        <p class="text-sm text-gray-600">
          Voulez-vous rendre ce ticket
          <span class="font-semibold">
                        {{ issue.private ? 'public' : 'privé' }}
                    </span> ?
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


  <!-- Modal de confirmation de réouverture -->
  <ConfirmModal
      v-model:open="isReopeningModalOpen"
      title="Demande de réouverture"
      description="Voulez-vous vraiment rouvrir ce ticket ?"
      @confirm="handleReopening"
  />
</template>
