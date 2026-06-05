<template>
  <div class="p-8">
    <div class="flex justify-between items-center mb-8">
      <h1 class="text-3xl font-bold text-black">
        {{ $t('tickets_followed') }}
      </h1>
      <UButton
        to="/issues"
        variant="outline"
        icon="i-lucide-arrow-left"
      >
        {{ $t('return_to_tickets') }}
      </UButton>
    </div>

    <div v-if="loading" class="text-center py-8">
      <USkeleton class="h-20 w-full mb-4" />
      <USkeleton class="h-20 w-full mb-4" />
      <USkeleton class="h-20 w-full" />
    </div>

    <div v-else-if="followedIssues.length === 0" class="text-center py-12">
      <p class="text-gray-500 text-lg">
        {{ $t('no_followed_tickets') }}
      </p>
      <UButton to="/issues" variant="outline" class="mt-4">
        {{ $t('see_all_tickets') }}
      </UButton>
    </div>

    <div v-else class="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-4">
      <IssuesCard
        v-for="issue in followedIssues"
        :key="issue.id"
        :issue="issue"
      />
    </div>

    <UAlert v-if="error" color="error" class="mt-4">
      <template #title>
        {{ $t('error') }}
      </template>
      {{ error }}
    </UAlert>
  </div>
</template>

<script setup lang="js">
/**
 * Page listant les issues suivies par l'utilisateur courant.
 * - Charge les issues via GET /issues/followed
 * - Affiche uniquement les issues avec statut OUVERT
 * - Redirige vers /login si non connecté
 * - Utilise un state global pour rester synchronisé avec les autres pages
 */

import IssuesCard from '~/components/issues/IssuesCard.vue'

const { ready, user } = useAuth()
const { followedIssues, refreshFollowedIssues, error: stateError } = useFollowedIssuesState()
const { t } = useI18n()

const loading = ref(true)
const error = computed(() => stateError.value)

onMounted(async () => {
  if (ready && !user?.value?.id) {
    await navigateTo('/login')
    return
  }

  try {
    await refreshFollowedIssues()
  } catch (err) {
    console.error(t('tickets_followed_page.error_loading_console'), err)
  } finally {
    loading.value = false
  }
})
</script>
