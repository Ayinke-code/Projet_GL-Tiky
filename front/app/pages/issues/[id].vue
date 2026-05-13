<script setup lang="ts">
const route = useRoute()
const { getIssueById, getIssueMessages, sendIssueMessage, updateIssueStatus } = useApi()

const issueId = computed(() => Number(route.params.id))

const issue = ref<any>(null)
const messages = ref<any[]>([])
const loading = ref(true)
const error = ref<string | null>(null)
const newMessage = ref('')
const sending = ref(false)

const isResolveModalOpen = ref(false)
const isVisibilityModalOpen = ref(false)

const loadIssue = async () => {
  loading.value = true
  error.value = null
  try {
    issue.value = await getIssueById(issueId.value)
    messages.value = await getIssueMessages(issueId.value)
  } catch (err: any) {
    error.value = err.message || 'Erreur de chargement'
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadIssue()
})

watch(issueId, () => {
  loadIssue()
})

const handleSend = async () => {
  if (!newMessage.value.trim()) return
  sending.value = true
  try {
    await sendIssueMessage(issueId.value, newMessage.value)
    messages.value = await getIssueMessages(issueId.value)
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
            @keydown.ctrl.enter="handleSend"
          />

        </div>

        <!-- ===== COLONNE DROITE : SIDEBAR ===== -->
        <div class="space-y-4">

          <!-- Carte INFORMATIONS -->
          <IssueInfoCard
            :auteur="issue.auteur"
            :professeur="issue.professeur"
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
