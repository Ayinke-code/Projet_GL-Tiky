<template>
  <ClientOnly>
    <div class="p-8">
      <h1 class="text-3xl font-bold mb-8">Issues suivies</h1>

      <div v-if="loading" class="text-center py-8">
        <USkeleton class="h-20 w-full mb-4" />
        <USkeleton class="h-20 w-full" />
      </div>

      <div v-else-if="issues.length === 0" class="text-center py-12">
        <p class="text-gray-500 text-lg">Vous ne suivez aucune issue pour le moment</p>
        <UButton to="/issues" variant="outline" class="mt-4">
          Voir toutes les issues
        </UButton>
      </div>

      <div v-else class="grid gap-4">
        <UCard
          v-for="issue in issues"
          :key="issue.idTick"
          class="hover:shadow-lg transition cursor-pointer"
          @click="navigateTo(`/issues/${issue.idTick}`)"
        >
          <template #header>
            <div class="flex justify-between items-start gap-4">
              <div class="flex-1">
                <h3 class="font-bold text-lg text-gray-900">{{ issue.titre }}</h3>
                <p class="text-sm text-gray-600 mt-1 line-clamp-2">{{ issue.description }}</p>
              </div>
              <div class="text-sm text-gray-500 whitespace-nowrap">
                {{ formatDate(issue.dateCreation) }}
              </div>
            </div>
          </template>

          <div class="flex gap-2 flex-wrap">
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
        </UCard>
      </div>

      <UAlert v-if="error" color="red" class="mt-4">
        <template #title>Erreur</template>
        {{ error }}
      </UAlert>
    </div>
  </ClientOnly>
</template>

<script setup lang="ts">
definePageMeta({
  layout: 'default'
})

const { getFollowedIssues } = useApi()
const issues = ref<any[]>([])
const loading = ref(true)
const error = ref<string | null>(null)

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

onMounted(async () => {
  try {
    issues.value = await getFollowedIssues()
    console.log('Issues suivies:', JSON.stringify(issues.value[0]))
  } catch (err: any) {
    error.value = err.message || 'Erreur lors du chargement'
  } finally {
    loading.value = false
  }
})
</script>
