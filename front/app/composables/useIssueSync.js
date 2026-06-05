export const useIssueSync = () => {
  const { getDisplayableIssues } = useIssues()
  const { refreshFollowedIssues } = useFollowedIssuesState()
  const { ready, user } = useAuth()

  const syncInterval = ref(null)

  const startSync = (intervalMs = 10000) => {
    if (process.server) return
    if (syncInterval.value) return


    syncInterval.value = setInterval(async () => {
      if (ready.value && user.value?.id) {
        try {
          await Promise.all([
            getDisplayableIssues(),
            refreshFollowedIssues()
          ])

          // On émet un événement personnalisé après le fetch réussi
          // pour que les pages puissent réagir aux nouvelles données
          window.dispatchEvent(new CustomEvent('issue-sync'))
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
