<template>
  <div class="p-8 min-h-screen bg-gray-50">

    <!-- En-tête -->
    <div class="flex justify-between items-center mb-8">
      <h1 class="text-2xl font-bold text-gray-800">Événements</h1>

      <!--
        Bouton visible pour TEACHER et ADMIN uniquement.
        On utilise user (pas ready) pour éviter le flash côté SSR.
      -->
      <Button
        v-if="user?.roles?.includes('TEACHER') || user?.roles?.includes('ADMIN')"
        @click="isModalOpen = true"
        icon="i-lucide-plus"
        label="Créer un événement"
        color="secondary" size="lg" variant="solid" rounded
      />
    </div>

    <!-- Onglets AUDIT / DEFENSE -->
    <div class="flex gap-2 mb-6">
      <button
        v-for="tab in tabs" :key="tab.value"
        @click="switchTab(tab.value)"
        class="px-5 py-2 rounded-full font-semibold text-sm transition-all cursor-pointer"
        :class="activeTab === tab.value
          ? 'bg-indigo-600 text-white shadow'
          : 'bg-white text-gray-500 border border-gray-200 hover:bg-gray-50'"
      >
        <i :class="tab.icon" class="mr-2"></i>{{ tab.label }}
      </button>
    </div>

    <!--
      Modal de création — composant dédié CreateEventModal.
      Reçoit l'onglet actif pour présélectionner le type si souhaité.
      Émet "created" quand la création réussit → on rafraîchit la liste.
    -->
    <CreateEventModal v-model:open="isModalOpen" />

    <!-- Chargement -->
    <div v-if="loading" class="space-y-4">
      <USkeleton v-for="n in 3" :key="n" class="h-24 w-full rounded-lg" />
    </div>

    <!-- Erreur de chargement -->
    <Alert
      v-else-if="fetchError"
      color="error"
      title="Erreur"
      :description="fetchError"
      class="mt-4"
    />

    <!-- Liste vide -->
    <div v-else-if="events.length === 0" class="text-center py-16 text-gray-400">
      <i class="fa-solid fa-calendar-xmark text-4xl mb-3 block"></i>
      <p class="text-lg">Aucun événement pour le moment</p>
    </div>

    <!-- Liste des événements -->
    <div v-else class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
      <EventCard v-for="event in events" :key="event.id" :event="event" @delete="deleteValidation" />
    </div>

    <ConfirmModal
      v-model:open="isDeleteValidateModalOpen"
      title="Suppression"
      :description="`Voulez-vous vraiment supprimer l'événement : ${deletingEvent?.name}`"
      @confirm="handleDelete"
    />

  </div>
</template>

<script setup>
// SSR désactivé sur cette page : le cookie JWT n'est pas transmis au backend
// lors du rendu serveur → 403 sur /auth/me → user=null → bouton invisible
// + hydration mismatch. Les autres pages héritées de dev ne sont pas impactées
// car elles bénéficiaient déjà d'une initialisation correcte côté client.
definePageMeta({ ssr: false })

import Button from '~/components/ui/Button.vue'
import Alert from '~/components/ui/Alert.vue'
import EventCard from '~/components/events/EventCard.vue'
import CreateEventModal from '~/components/events/CreateEventModal.vue'
import ConfirmModal from '~/components/ui/ConfirmModal.vue'
import { ref, onMounted } from 'vue'

const { user } = useAuth()
const { events, fetchEventsByType, deleteEvent } = useEvents()

const activeTab = ref('AUDIT')
const loading   = ref(true)
const fetchError = ref(null)
const isModalOpen = ref(false)

const deletingEvent = ref(null)
const isDeleteValidateModalOpen = ref(false)

const deleteValidation = (event) => {
  deletingEvent.value = { id: event?.id, name: event?.name }
  isDeleteValidateModalOpen.value = true
}

const handleDelete = async () => {
  if (!deletingEvent.value) return
  const id = deletingEvent.value.id
  try {
    await deleteEvent(id)
  } catch (err) {
    fetchError.value = err.message || 'Erreur lors de la suppression'
  } finally {
    deletingEvent.value = null
    isDeleteValidateModalOpen.value = false
  }
}

const tabs = [
  { value: 'AUDIT',   label: 'Audits',       icon: 'fa-solid fa-magnifying-glass' },
  { value: 'DEFENSE', label: 'Soutenances',   icon: 'fa-solid fa-graduation-cap'  }
]

/**
 * Change l'onglet et recharge la liste.
 * Le finally garantit que loading repasse à false même en cas d'erreur API.
 */
const switchTab = async (type) => {
  activeTab.value = type
  loading.value   = true
  fetchError.value = null
  try {
    await fetchEventsByType(type)
  } catch (e) {
    fetchError.value = 'Impossible de charger les événements.'
  } finally {
    // OBLIGATOIRE : sans finally, une erreur API bloque le spinner indéfiniment
    loading.value = false
  }
}

const EVENT_NOTIFICATION_TYPES = ['UPDATE_EVENT', 'EVENT_CANCEL']

const onNotificationReceived = async (e) => {
  if (EVENT_NOTIFICATION_TYPES.includes(e.detail?.type)) {
    await switchTab(activeTab.value)
  }
}

onMounted(() => {
  switchTab('AUDIT')
  window.addEventListener('notification:received', onNotificationReceived)
})

onUnmounted(() => {
  window.removeEventListener('notification:received', onNotificationReceived)
})
</script>