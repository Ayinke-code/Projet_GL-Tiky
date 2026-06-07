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
import IssueRelationCard from '~/components/issues/IssueRelationCard.vue'
import { getFileIcon } from '#imports';

const route = useRoute()
const { ready, user, wsToken } = useAuth()
const { getIssueById, updateIssueStatus, updateIssuePrivacy } = useIssues()
const { getIssueMessages, sendIssueMessage, normalizeMessage, initializeConversation } = useConversations()
const { followIssue, unfollowIssue, isFollowing } = useIssueFollowers()
const { acknowledgeFollowedIssueUpdate } = useFollowedIssuesState()
const { downloadFile, deleteFile } = useDocuments()

const FOLLOWER_NOTIFICATION_TYPES = ['NEW_FOLLOWER', 'NEW_UNFOLLOW']

const onNotificationReceived = async (e) => {
  if (FOLLOWER_NOTIFICATION_TYPES.includes(e.detail?.type) && e.detail?.issueId === issueId) {
    const updated = await getIssueById(issueId).catch(() => null)
    if (updated) issue.value = updated
  }
}

// ID de l'issue depuis l'URL
const issueId = Number(route.params.id)

// State local
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

// Contient les fichiers associés au ticket si il y en a
const files = ref(null)

// Contient les fichiers associés à un message
const messageFiles = ref(null)

/**
 * Téléchargement des documents
 */
const downloadDoc = async (file) => {
  try{
    await downloadFile(file)
  } catch(err) {
    console.error(err)
  }
}

/**
 * Créateur du ticket peut supprimer un fichier
 */
const deleteDoc = async (file) => {
  try {
    await deleteFile(file)
    issue.value.documents = issue.value.documents.filter(d => d.id !== file.id)
    files.value = files.value.filter(d => d.id !== file.id)
  } catch (err) {
    console.error(err)
  }
}

// IDs de tous les documents déjà présents dans les messages
const messageDocIds = computed(() => {
  return new Set(
    messages.value.flatMap(m =>
      (m.documents || []).map(d => d.id)
    )
  )
})

// Filtre les fichiers de l'issue en excluant ceux déjà utilisés dans les messages
const filteredFiles = computed(() => {
  return files.value.filter(f => !messageDocIds.value.has(f.id))
})

/**
 * Callback après suivi réussi
 */
const onFollowed = () => {
  alreadyFollowing.value = true
  followSuccess.value = true
  acknowledgeFollowedIssueUpdate(issue.value)
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
          acknowledgeFollowedIssueUpdate(issue.value)
          window.dispatchEvent(new CustomEvent('conversation:new_message', { detail: data.message }))
        }
      } else if ((data.type === 'privacy_update' || data.type === 'status_update') && Number(data.issueId) === issueId) {
        console.log(`[WebSocket] Notification de changement (${data.type}) reçue`)
        checkAccess()
        window.dispatchEvent(new CustomEvent('issue:updated', { detail: { issueId: data.issueId, type: data.type } }))
      }
    } catch (e) {
      console.error('Erreur WebSocket:', e)
    }
  }

  socket.value.onerror = () => {
    console.error('Erreur WebSocket')
    socket.value?.close()
  }

  socket.value.onclose = () => {
    console.warn('[WebSocket] Connexion perdue. Tentative de reconnexion dans 3 secondes...')
    setTimeout(connectWebSocket, 3000)
  }
}

/**
 * Chargement initial de l'issue et des messages
 */
onMounted(async () => {
  if (ready && !user?.value?.id) {
    await navigateTo('/login')
    return
  }

  try {
    issue.value = await getIssueById(issueId)
    acknowledgeFollowedIssueUpdate(issue.value)

    if (process.client && issue.value) {
      useHead({
        title: `${issue.value.title} - Ticket #${issueId}`
      })
    }

    alreadyFollowing.value = await isFollowing(issueId)
    const conversation = await initializeConversation(issueId)
    conversationId.value = conversation?.id ?? null
    messages.value = await getIssueMessages(issueId)
    connectWebSocket()

    if (process.client) {
      window.addEventListener('issue-sync', checkAccess)
      window.addEventListener('notification:received', onNotificationReceived)
    }

    if (issue.value?.private) {
      await checkAccess()
    }

    if (issue.value?.documents?.length > 0){
      files.value = issue?.value?.documents
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
  try {
    const updatedIssue = await getIssueById(issueId)

    if (updatedIssue.private) {
      const currentUserId = user?.value?.id
      const hasAccess =
        currentUserId === updatedIssue.createdId ||
        currentUserId === updatedIssue.categoryManagerId ||
        currentUserId === updatedIssue.categoryOwnerId

      if (!hasAccess) {
        console.warn('[Issue Detail] Accès révoqué, redirection...')
        error.value = 'Vous n\'avez plus accès à ce ticket (passé en privé).'
        setTimeout(async () => {
          await navigateTo('/issues', { replace: true })
        }, 2000)
        return
      }
    }

    issue.value = updatedIssue
    acknowledgeFollowedIssueUpdate(issue.value)
  } catch (err) {
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
    window.removeEventListener('notification:received', onNotificationReceived)
  }
})

/**
 * Marquer l'issue comme terminée
 */
const handleMarkResolved = async () => {
  try {
    await updateIssueStatus(issueId, 'TERMINE')
    issue.value = await getIssueById(issueId)
  } catch (err) {
    console.error(err)
    error.value = err?.message || 'Erreur lors de la mise à jour du statut'
  }
}

/**
 * Issue manager peut réouvrir un ticket TERMINE
 */
const handleReopening = async () => {
  if(issue?.value?.status !== 'TERMINE') return
  try {
    await updateIssueStatus(issue?.value?.id, 'OUVERT')
  } catch(err) {
    console.error(err)
  }
}

/**
 * Couleurs des statuts (aligné backend)
 */
const getStatusBadgeClass = (status) => {
  const classes = {
    'OUVERT': 'bg-yellow-100 text-yellow-800 border border-yellow-300',
    'EN_ATTENTE': 'bg-gray-100 text-gray-800 border border-gray-300',
    'INCOMPLET': 'bg-orange-100 text-orange-800 border border-orange-300',
    'TERMINE': 'bg-green-100 text-green-800 border border-green-300',
    'DEMANDE_REOUVERTURE': 'bg-blue-100 text-blue-800 border border-blue-300'
  }
  return classes[status] || 'bg-gray-100 text-gray-600'
}

/**
 * Labels statuts
 */
const getStatusLabel = (status) => {
  const labels = {
    'OUVERT': 'Ouvert',
    'EN_ATTENTE': 'En attente',
    'INCOMPLET': 'Incomplet',
    'TERMINE': 'Terminé',
    'DEMANDE_REOUVERTURE': 'Demande réouverture'
  }
  return labels[status] || status
}

// Modals UI
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
    issue.value = await getIssueById(issueId)
  } catch (err) {
    console.error(err)
    error.value = err?.message || 'Erreur lors de la modification de la visibilité'
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
    const sentMessage = await sendIssueMessage(issueId, newMessage.value, messageFiles.value, replyingToMessage.value?.id ?? null)
    if (!messages.value.find((m) => m.id === sentMessage.id)) {
      messages.value.push(sentMessage)
    }

    const existingMessage = messages.value.find(m => m.id === sentMessage.id)
    if (!existingMessage) {
      messages.value.push(sentMessage)
    } else {
      if (sentMessage.documents?.length && !existingMessage.documents?.length) {
        existingMessage.documents = sentMessage.documents
      }
    }

    newMessage.value = ''
    messageFiles.value = null
    replyingToMessage.value = null

    const updatedIssue = await getIssueById(issueId)
    issue.value = updatedIssue
    acknowledgeFollowedIssueUpdate(updatedIssue)
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
    error.value = err?.message || 'Erreur lors du désabonnement'
  }
}

// Slider sidebar
const activeSlide = ref(0)
const slides = computed(() => {
  if (user?.value?.id === issue?.value?.categoryManagerId) {
    return ['meeting-proposal', 'relations']
  }
  return ['meeting-demand', 'relations']
})
</script>

<template>
  <div class="min-h-screen bg-gray-50">
    <div v-if="loading" class="flex items-center justify-center min-h-[60vh]">
      <div class="flex flex-col items-center gap-3 text-gray-400">
        <svg class="animate-spin h-8 w-8" fill="none" viewBox="0 0 24 24">
          <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" />
          <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8z" />
        </svg>
        <span class="text-sm">Chargement du ticket...</span>
      </div>
    </div>

    <div v-else-if="error" class="p-8">
      <UAlert color="red" :description="error" />
    </div>

    <div v-else class="max-w-7xl mx-auto px-4 py-6 space-y-4">

      <div class="flex items-center gap-2 text-sm text-gray-500">
        <NuxtLink to="/issues" class="hover:text-gray-800 flex items-center gap-1 transition-colors">
          <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 19l-7-7m0 0l7-7m-7 7h18" />
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
            <span class="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-semibold"
                  :class="getStatusBadgeClass(issue.status)">
              {{ getStatusLabel(issue.status) }}
            </span>
            <span class="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-semibold"
                  :class="!issue.private ? 'text-blue-600 bg-white' : 'text-red-600 bg-white'">
              <svg xmlns="http://www.w3.org/2000/svg" class="h-3 w-3" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path v-if="!issue.private" stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
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
          <button
            v-if="issue.status !== 'TERMINE' && user?.id === issue?.categoryManagerId && user.id !== null"
            @click="isResolveModalOpen = true"
            :disabled="issue.status === 'résolu'"
            class="inline-flex items-center gap-1.5 px-3 py-2 rounded-lg border border-emerald-300 text-emerald-700 bg-white hover:bg-emerald-50 text-sm font-medium transition-colors disabled:opacity-40 disabled:cursor-not-allowed cursor-pointer transition-transform duration-200 hover:scale-105">
            <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
            </svg>
            Marquer Résolu
          </button>

          <button
            v-else-if="issue.status === 'TERMINE' && user?.id === issue?.categoryManagerId && user.id !== null"
            @click="isReopeningModalOpen = true"
            :disabled="issue.status === 'résolu'"
            class="inline-flex items-center gap-1.5 px-3 py-2 rounded-lg border border-emerald-300 text-emerald-700 bg-white hover:bg-emerald-50 text-sm font-medium transition-colors disabled:opacity-40 disabled:cursor-not-allowed cursor-pointer transition-transform duration-200 hover:scale-105">
            <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
            </svg>
            Rouvrir
          </button>

          <button v-if="canTogglePrivacy" @click="isVisibilityModalOpen = true"
                  class="inline-flex items-center gap-1.5 px-3 py-2 rounded-lg border text-sm font-medium transition-colors transition-transform duration-200 hover:scale-105 cursor-pointer"
                  :class="issue.private
                    ? 'border-blue-300 text-blue-600 bg-white hover:bg-blue-50'
                    : 'border-red-300 text-red-600 bg-white hover:bg-red-50'">
            <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path v-if="issue.private" stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                    d="M12 2a10 10 0 1 0 0 20 10 10 0 1 0 0-20M2 12h20M12 2a15.3 15.3 0 0 1 4 10 15.3 15.3 0 0 1-4 10 15.3 15.3 0 0 1-4-10 15.3 15.3 0 0 1 4-10z" />
              <path v-else stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                    d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z" />
            </svg>
            {{ issue.private ? 'Rendre Public' : 'Rendre Privé' }}
          </button>

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

      <div v-if="followSuccess" class="rounded-lg bg-green-50 border border-green-200 px-4 py-2 text-sm text-green-700">
        Vous suivez maintenant ce ticket !
      </div>

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
              :files="msg.documents"
              :issue="issue"
              :user="user"
              :is-mine="msg.userId === user?.id"
              :reply-to="msg.replyTo"
              @reply="replyingToMessage = msg"
              @download="downloadDoc"
            />
            <IssueEmptyMessages v-if="messages.length === 0" />
          </div>

          <!-- Zone d'envoi de message -->
          <MessageInput
            v-model="newMessage"
            v-model:messageFiles="messageFiles"
            :sending="sending"
            :replying-to="replyingToMessage"
            @cancel-reply="replyingToMessage = null"
            @send="handleSend"
          />
        </div>

        <!-- ===== COLONNE DROITE : SIDEBAR ===== -->
        <div class="space-y-4">

          <!-- Informations -->
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

              <!-- Documents attachés à l'issue -->
              <div v-if="files" class="mt-6">
                <div class="text-sm font-medium text-gray-500 mb-2">Documents</div>
                <div class="space-y-2">
                  <div
                    v-for="doc in filteredFiles"
                    :key="doc.id"
                    class="flex items-center justify-between gap-3 p-3 border rounded-lg hover:bg-gray-50 transition"
                  >
                    <div class="flex items-center gap-3 min-w-0">
                      <UIcon :name="getFileIcon(doc.contentType)" class="text-gray-500 text-lg shrink-0" />
                      <div class="min-w-0">
                        <p class="font-medium text-gray-900 truncate">{{ doc.originalName }}</p>
                        <p class="text-xs text-gray-500">{{ (doc.size / 1024).toFixed(1) }} KB</p>
                      </div>
                    </div>
                    <div class="flex items-center gap-1 shrink-0">
                      <UButton v-if="issue.createdId === user?.id"
                        icon="streamline-ultimate:delete-bold"
                        size="sm" variant="ghost"
                        class="shrink-0 cursor-pointer"
                        color="error"
                        v-on:click="deleteDoc(doc)"
                      />
                      <UButton
                        icon="i-lucide-download"
                        size="sm" variant="ghost"
                        class="shrink-0 cursor-pointer"
                        color="info"
                        v-on:click="downloadDoc(doc)"
                      />
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- Slider sidebar : RDV + Relations -->
          <div v-if="alreadyFollowing" class="relative">
            <div class="overflow-hidden">
              <div
                class="flex transition-transform duration-300 ease-in-out"
                :style="`transform: translateX(-${activeSlide * 100}%)`"
              >
                <!-- Slide RDV Manager -->
                <div class="min-w-full" v-if="user?.id === issue?.categoryManagerId">
                  <ClientOnly>
                    <IssueMeetingProposal v-if="activeSlide === 0" :issue="issue" />
                  </ClientOnly>
                </div>
                <!-- Slide RDV Demande -->
                <div class="min-w-full" v-if="user?.id !== issue?.categoryManagerId">
                  <ClientOnly>
                    <IssueMeetingDemand v-if="activeSlide === 0" :issue="issue" />
                  </ClientOnly>
                </div>
                <!-- Slide Relations -->
                <div class="min-w-full">
                  <ClientOnly>
                    <IssueRelationCard
                      v-if="activeSlide === 1"
                      :current-issue-id="issueId"
                      :current-category-id="issue.categoryId"
                      :current-category-manager-id="issue.categoryManagerId"
                      :can-create="alreadyFollowing"
                      :current-user-id="user?.id"
                    />
                  </ClientOnly>
                </div>
              </div>
            </div>

            <!-- Dots de navigation -->
            <div class="flex justify-center gap-2 mt-3">
              <button
                v-for="(_, i) in slides"
                :key="i"
                @click="activeSlide = i"
                class="w-2 h-2 rounded-full transition-all duration-200"
                :class="activeSlide === i ? 'bg-indigo-600 w-4' : 'bg-gray-300 hover:bg-gray-400'"
              />
            </div>
          </div>

          <!-- Si non abonné : affichage direct sans slider -->
          <template v-else>
            <IssueMeetingProposal
              v-if="user?.id === issue?.categoryManagerId && user?.id !== null"
              :issue="issue"
            />
            <IssueMeetingDemand
              v-if="user?.id !== issue?.categoryManagerId && user?.id !== null"
              :issue="issue"
            />
          </template>

          <slot name="sidebar-extra" />
        </div>
      </div>
    </div>
  </div>

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

  <div v-if="isVisibilityModalOpen && canTogglePrivacy" class="fixed inset-0 bg-black/40 flex items-center justify-center z-50 p-4"
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

  <FollowModal
    v-model="showFollowModal"
    :issue-id="issueId"
    @followed="onFollowed"
  />

  <ConfirmModal
    v-model:open="isReopeningModalOpen"
    title="Demande de réouverture"
    description="Voulez-vous vraiment rouvrir ce ticket ?"
    @confirm="handleReopening"
  />
</template>
