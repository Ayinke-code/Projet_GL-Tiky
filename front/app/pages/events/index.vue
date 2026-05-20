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
    <CreateEventModal
      v-model:open="isModalOpen"
      @created="onEventCreated"
    />

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
      <EventCard v-for="event in events" :key="event.id" :event="event" />
    </div>

  </div>
</template>

<script setup>
// SSR désactivé sur cette page : le cookie JWT est indisponible côté serveur
// → user=null au rendu serveur → bouton invisible + hydration mismatch.
definePageMeta({ ssr: false })

const { user } = useAuth()
const { events, fetchEventsByType } = useEvents()

const activeTab = ref('AUDIT')
const loading   = ref(true)
const fetchError = ref(null)
const isModalOpen = ref(false)

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

/** Appelé par CreateEventModal après une création réussie. */
const onEventCreated = () => {
  isModalOpen.value = false
  switchTab(activeTab.value) // rafraîchit la liste du bon onglet
}

onMounted(() => switchTab('AUDIT'))
</script>