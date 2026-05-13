<script setup lang="ts">
const route = useRoute()
const { getIssueById, getIssueMessages, sendIssueMessage, updateIssueStatus, normalizeMessage } = useApi()
const { user, wsToken } = useAuth()
const config = useRuntimeConfig()

const issueId = computed(() => Number(route.params.id))

const issue = ref<any>(null)
const messages = ref<any[]>([])
const loading = ref(true)
const error = ref<string | null>(null)
const newMessage = ref('')
const sending = ref(false)
const socket = ref<WebSocket | null>(null)

const isResolveModalOpen = ref(false)
const isVisibilityModalOpen = ref(false)

const connectWebSocket = () => {
  if (process.server) return
  if (socket.value) socket.value.close()

  const token = wsToken.value || localStorage.getItem('wsToken')
  if (!token) return

  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
  const apiBase = config.public.apiBase
  const host = apiBase.startsWith('/') ? window.location.host + apiBase : new URL(apiBase).host + new URL(apiBase).pathname
  const wsUrl = `${protocol}//${host}/ws/issue?token=${token}&issueId=${issueId.value}`

  socket.value = new WebSocket(wsUrl)

  socket.value.onmessage = (event) => {
    try {
      const data = JSON.parse(event.data)
      if (data.type === 'new_message' && data.message) {
        const normalized = normalizeMessage(data.message)
        if (!messages.value.find((m: any) => m.id === normalized.id)) {
          messages.value.push(normalized)
        }
      }
    } catch (e) {}
  }

  socket.value.onerror = () => socket.value?.close()
}

const loadIssue = async () => {
  loading.value = true
  error.value = null
  try {
    issue.value = await getIssueById(issueId.value)
    messages.value = await getIssueMessages(issueId.value)
    connectWebSocket()
  } catch (err: any) {
    error.value = err.message || 'Erreur de chargement'
  } finally {
    loading.value = false
  }
}

onMounted(() => loadIssue())
onUnmounted(() => socket.value?.close())

watch(issueId, () => {
  socket.value?.close()
  loadIssue()
})

const handleSend = async () => {
  if (!newMessage.value.trim()) return
  if (newMessage.value.length > 4000) {
    error.value = 'Le message ne doit pas dépasser 4000 caractères'
    return
  }

  sending.value = true
  try {
    const sentMessage = await sendIssueMessage(issueId.value, newMessage.value)
    if (!messages.value.find((m: any) => m.id === sentMessage.id)) {
      messages.value.push(sentMessage)
    }
    newMessage.value = ''
  } catch (err: any) {
    error.value = err.message
  } finally {
    sending.value = false
  }
}

const handleMarkResolved = async () => {
  try {
    await updateIssueStatus(issueId.value, 'résolu')
    issue.value.statut = 'résolu'
  } catch {}
}

const toggleVisibility = async () => {
  try {
    const newStatus = issue.value.visibilite === 'Privé' ? 'Public' : 'Privé'
    issue.value.visibilite = newStatus
  } catch {}
}

</script>

<template>
  <div class="min-h-screen bg-gray-50">
    <!-- Loader -->
    <div v-if="loading" class="flex items-center justify-center min-h-[60vh]">
      <IssueLoader text="Chargement du ticket..." />
    </div>

    <!-- Erreur -->
    <div v-else-if="error" class="p-8">
      <UAlert color="error" :description="error" />
    </div>

    <!-- Contenu principal -->
    <div v-else-if="issue" class="max-w-7xl mx-auto px-4 py-6 space-y-4">

      <!-- En-tête avec breadcrumb, titre et actions -->
      <IssueHeader
        v-if="issue"
        :issueId="issueId"
        :title="issue.titre || ''"
        :status="issue.statut || ''"
        :visibility="issue.visibilite || 'Public'"
        :category="issue.categorie?.nom"
        :isResolved="issue.statut === 'résolu'"
        @resolveClick="isResolveModalOpen = true"
        @visibilityClick="isVisibilityModalOpen = true"
      />

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
              :roles="msg.roles"
              :date="msg.date"
              :contenu="msg.contenu"
            />

            <!-- État vide -->
            <IssueEmptyMessages v-if="messages.length === 0" />
          </div>

          <!-- Zone de réponse -->
          <MessageInput
            v-model="newMessage"
            :sending="sending"
            @send="handleSend"
          />

        </div>

        <!-- ===== COLONNE DROITE : SIDEBAR ===== -->
        <div class="space-y-4">

          <!-- Carte INFORMATIONS -->
          <IssueInfoCard
            :auteur="issue.auteur"
            :auteurRole="issue.auteurRole"
            :auteurRoles="issue.auteurRoles"
            :professeur="issue.professeur"
            :professeurRole="issue.professeurRole"
            :professeurRoles="issue.professeurRoles"
            :abonnes="issue.abonnes"
          />

          <!-- Slot pour futures fonctionnalités sidebar -->
          <slot name="sidebar-extra" />

        </div>
      </div>
    </div>
  </div>

  <!-- Modale de résolution -->
  <IssueResolveModal
    v-if="issue"
    :isOpen="isResolveModalOpen"
    @update:isOpen="isResolveModalOpen = $event"
    @confirm="handleMarkResolved"
  />

  <!-- Modale de visibilité -->
  <IssueVisibilityModal
    v-if="issue"
    :isOpen="isVisibilityModalOpen"
    :visibility="issue.visibilite || 'Public'"
    @update:isOpen="isVisibilityModalOpen = $event"
    @confirm="toggleVisibility"
  />
</template>
