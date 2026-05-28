export const useIssueSync = () => {
  const { fetchIssues } = useIssues()
  const { refreshFollowedIssues } = useFollowedIssuesState()
  const { ready, user } = useAuth()

  const syncInterval = ref(null)

  const startSync = (intervalMs = 1000) => {
    if (process.server) return
    if (syncInterval.value) return

    console.log('[useIssueSync] Démarrage de la synchronisation périodique...')

    syncInterval.value = setInterval(async () => {
      if (ready.value && user.value?.id) {
        try {
          await Promise.all([
            fetchIssues(),
            refreshFollowedIssues()
          ])
        } catch (err) {
          console.error('[useIssueSync] Erreur lors de la synchronisation:', err)
        }
      }
    }, intervalMs)
  }

  const stopSync = () => {
    if (syncInterval.value) {
      clearInterval(syncInterval.value)
      syncInterval.value = null
    }
  }

  return {
    startSync,
    stopSync
  }
}
