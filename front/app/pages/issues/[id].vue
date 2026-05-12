<template>
  <ClientOnly>
    <div class="p-8 max-w-3xl mx-auto">

      <div v-if="loading" class="text-center py-8">
        <USkeleton class="h-10 w-full mb-4" />
        <USkeleton class="h-40 w-full" />
      </div>

      <div v-else-if="issue">
        <div class="flex justify-between items-start mb-6">
          <div>
            <h1 class="text-3xl font-bold">{{ issue.titre }}</h1>
            <div class="flex gap-2 mt-2 flex-wrap">
              <UBadge variant="soft" color="blue">
                {{ issue.categorie?.nom || 'Sans catégorie' }}
              </UBadge>
              <UBadge :color="statusColor(issue.statut)" variant="soft">
                {{ getStatusLabel(issue.statut) }}
              </UBadge>
              <UBadge v-if="issue.estPrive" color="orange" variant="soft">
                🔒 Privé
              </UBadge>
            </div>
          </div>

          <UButton
            icon="i-lucide-bell"
            @click="showFollowModal = true"
          >
            Suivre
          </UButton>
        </div>

        <!-- Description -->
        <UCard class="mb-6">
          <template #header>
            <h2 class="font-semibold">Description</h2>
          </template>
          <p class="text-gray-700 whitespace-pre-wrap">{{ issue.description }}</p>
          <template #footer>
            <p class="text-sm text-gray-500">
              Créé le {{ formatDate(issue.dateCreation) }}
            </p>
          </template>
        </UCard>

        <!-- Droits d'interaction -->
        <div v-if="alreadyFollowing" class="mt-4">
          <UBadge v-if="canInteractValue" color="green" variant="soft">
            ✅ Vous pouvez interagir sur cette issue
          </UBadge>
          <UBadge v-else color="gray" variant="soft">
            Consultation uniquement
          </UBadge>
        </div>

        <!-- Message succès -->
        <UAlert
          v-if="followSuccess"
          color="green"
          class="mb-4"
          title="Vous suivez maintenant cette issue !"
        />
      </div>

      <UAlert v-if="error" color="red" class="mt-4">
        <template #title>Erreur</template>
        {{ error }}
      </UAlert>

    </div>
  </ClientOnly>

  <FollowModal
    v-model="showFollowModal"
    :issue-id="Number(route.params.id)"
    @followed="onFollowed"
  />
</template>


<script setup lang="ts">
definePageMeta({
  layout: 'default'
})

const route = useRoute()
const { getIssueById, isFollowing, canInteract } = useApi()

const issue = ref<any>(null)
const loading = ref(true)
const error = ref<string | null>(null)
const showFollowModal = ref(false)
const followSuccess = ref(false)
const canInteractValue = ref(false)
const alreadyFollowing = ref(false)

const statusColor = (status: string): any => {
  const colors: Record<string, string> = {
    'OUVERT': 'yellow',
    'COMPLET': 'green',
    'INCOMPLET': 'orange',
    'TERMINE': 'blue'
  }
  return colors[status] || 'gray'
}

const getStatusLabel = (status: string): string => {
  const labels: Record<string, string> = {
    'OUVERT': 'Ouvert',
    'COMPLET': 'Complet',
    'INCOMPLET': 'Incomplet',
    'TERMINE': 'Terminé'
  }
  return labels[status] || status
}

const formatDate = (date: string) => {
  return new Date(date).toLocaleDateString('fr-FR', {
    year: 'numeric',
    month: 'short',
    day: 'numeric'
  })
}

const onFollowed = () => {
  followSuccess.value = true
  setTimeout(() => followSuccess.value = false, 3000)
}

onMounted(async () => {
  try {
    issue.value = await getIssueById(Number(route.params.id))
    console.log('Issue chargée ✅')
  } catch (err: any) {
    error.value = err.message || 'Erreur lors du chargement'
    console.log('Erreur issue:', err)
  } finally {
    loading.value = false
  }

  try {
    alreadyFollowing.value = await isFollowing(Number(route.params.id))
    console.log('alreadyFollowing:', alreadyFollowing.value)
  } catch (err: any) {
    console.log('Erreur isFollowing:', err)
  }

  try {
    canInteractValue.value = await canInteract(Number(route.params.id))
    console.log('canInteract:', canInteractValue.value)
  } catch (err: any) {
    console.log('Erreur canInteract:', err)
  }
})
</script>
