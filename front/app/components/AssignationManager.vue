<template>
  <div class="space-y-6">
    <UCard>
      <template #header>
        <div class="flex items-center justify-between">
          <h2 class="text-lg font-semibold">Gérer mes assignations</h2>
          <UBadge color="orange" variant="soft" size="sm">{{ alerts.length }} en attente</UBadge>
        </div>
      </template>

      <div v-if="alerts.length > 0" class="space-y-4">
        <div v-for="req in alerts" :key="req.id" class="flex items-center justify-between p-4 border rounded-xl bg-gray-50/50">
          <!-- Infos -->
          <div class="flex flex-col w-1/3">
            <span class="font-bold text-gray-900">{{ req.issueTitle }}</span>
            <div class="mt-1 flex flex-col space-y-0.5">
      <span class="text-xs font-semibold text-primary-600 uppercase tracking-wide">
        Par : {{ req.creator }}
      </span>
              <span class="text-[10px] text-gray-500 uppercase">
        Le {{ formatDate(req.creationDate) }}
      </span>
            </div>
          </div>

          <!-- Bouton voir : centré dans la card -->
          <div class="flex justify-center w-1/3">
            <UButton
              label="Voir le détail"
              icon="i-heroicons-eye"
              color="neutral"
              variant="outline"
              size="lg"
              @click="openDetail(req)"
            />
          </div>

          <!-- Actions : droite -->
          <div class="flex items-center justify-end gap-3 w-1/3">
            <USelectMenu
              v-model="selectedTeachers[req.id]"
              :items="formattedTeachers"
              placeholder="Assigner à..."
              value-attribute="value"
              option-attribute="label"
              class="w-56"
            />
            <div class="flex flex-col gap-1.5">
              <UButton
                label="Assigner"
                icon="i-heroicons-paper-airplane"
                :disabled="!selectedTeachers[req.id] || loadingIds.has(req.id)"
                :loading="loadingIds.has(req.id)"
                @click="openConfirm(req)"
              />
              <UButton
                label="Incomplet"
                icon="i-heroicons-x-mark"
                class="bg-red-600 hover:bg-red-700 text-white border-0"
                :disabled="loadingIncompleteIds.has(req.id)"
                :loading="loadingIncompleteIds.has(req.id)"
                @click="openIncompleteConfirm(req)"
              />
            </div>
          </div>
        </div>
      </div>
      <div v-else class="text-center py-8 text-gray-400 italic bg-gray-50/30 rounded-lg border border-dashed">
        Aucune demande d'assignation à compléter.
      </div>
    </UCard>

    <UCard>
      <template #header>
        <h3 class="text-lg font-semibold">Historique des demandes</h3>
      </template>
      <UTable v-if="history && history.length > 0" :data="history" :columns="columns">
        <template #status-cell="{ row }">
          <UBadge :color="getStatusColor(row.original.status)" variant="subtle" size="sm">
            {{ translateStatus(row.original.status) }}
          </UBadge>
        </template>
      </UTable>
      <div v-else class="text-center py-8 text-gray-400 italic bg-gray-50/30 rounded-lg border border-dashed">
        Aucune assignation effectuée.
      </div>
    </UCard>

    <!-- Modale : Confirmer l'assignation -->
    <UModal v-model:open="isConfirmOpen" title="Confirmer l'assignation">
      <template #body>
        <p class="text-gray-600">
          Voulez-vous vraiment assigner
          <strong class="text-gray-900">{{ getSelectedTeacherName }}</strong>
          pour traiter l'issue :
        </p>
        <div class="mt-2 p-3 bg-gray-50 rounded border border-gray-100 italic text-sm text-gray-700">
          "{{ activeReq?.issueTitle }}"
        </div>
      </template>
      <template #footer>
        <div class="flex justify-end gap-3">
          <UButton color="neutral" variant="ghost" label="Annuler" @click="isConfirmOpen = false"/>
          <UButton color="primary" label="Confirmer l'envoi" @click="confirmAssignation"/>
        </div>
      </template>
    </UModal>

    <!-- Modale : Confirmer le marquage incomplet -->
    <UModal v-model:open="isIncompleteConfirmOpen" title="Marquer comme incomplet">
      <template #body>
        <p class="text-gray-600">
          Voulez-vous vraiment marquer l'issue suivante comme incomplète ? Cette action refusera la demande associée.
        </p>
        <div class="mt-2 p-3 bg-gray-50 rounded border border-gray-100 italic text-sm text-gray-700">
          "{{ incompleteReq?.issueTitle }}"
        </div>
      </template>
      <template #footer>
        <div class="flex justify-end gap-3">
          <UButton color="neutral" variant="ghost" label="Annuler" @click="isIncompleteConfirmOpen = false"/>
          <UButton class="bg-red-600 hover:bg-red-700 text-white border-0" label="Confirmer" @click="confirmMarkIncomplete"/>
        </div>
      </template>
    </UModal>

    <!-- Modale : Détail de l'issue -->
    <UModal v-model:open="isDetailOpen" title="Détail de l'issue">
      <template #body>
        <div v-if="loadingDetail" class="flex justify-center py-8">
          <UIcon name="i-heroicons-arrow-path" class="animate-spin text-gray-400 text-2xl"/>
        </div>
        <div v-else-if="issueDetail" class="space-y-4">
          <div class="space-y-1">
            <p class="text-xs font-semibold text-gray-400 uppercase tracking-wide">Titre</p>
            <p class="text-gray-900 font-medium">{{ issueDetail.titre }}</p>
          </div>
          <div class="border-t border-gray-100"/>
          <div class="space-y-1">
            <p class="text-xs font-semibold text-gray-400 uppercase tracking-wide">Créé par</p>
            <p class="text-gray-700 text-sm">{{ issueDetail.creator }}</p>
          </div>
          <div class="border-t border-gray-100"/>
          <div class="space-y-1">
            <p class="text-xs font-semibold text-gray-400 uppercase tracking-wide">Catégorie</p>
            <UBadge color="primary" variant="soft">{{ issueDetail.category }}</UBadge>
          </div>
          <div class="border-t border-gray-100"/>
          <div class="space-y-1">
            <p class="text-xs font-semibold text-gray-400 uppercase tracking-wide">Description</p>
            <p class="text-gray-700 text-sm leading-relaxed whitespace-pre-wrap">{{ issueDetail.description }}</p>
          </div>
        </div>
        <div v-else class="text-center py-8 text-gray-400 italic">
          Impossible de charger les détails de l'issue.
        </div>
      </template>
      <template #footer>
        <div class="flex justify-end">
          <UButton color="neutral" variant="ghost" label="Fermer" @click="isDetailOpen = false"/>
        </div>
      </template>
    </UModal>
  </div>
</template>

<script setup>
const props = defineProps(['userId'])

const { getOwnerAlerts, getHistory, getTeachersList, assignTeacher, markIssueAsIncomplete, getIssueById } = useAssignation()

const alerts = ref([])
const history = ref([])
const teachers = ref([])
const selectedTeachers = ref({})

const loadingIds = ref(new Set())
const loadingIncompleteIds = ref(new Set())

const isConfirmOpen = ref(false)
const activeReq = ref(null)

const isIncompleteConfirmOpen = ref(false)
const incompleteReq = ref(null)

const isDetailOpen = ref(false)
const issueDetail = ref(null)
const loadingDetail = ref(false)

const columns = [
  { accessorKey: 'issueTitle', header: "Titre de l'Issue" },
  { accessorKey: 'creator', header: 'Créé par' },
  { accessorKey: 'target', header: 'Assigné à' },
  {
    accessorKey: 'status',
    header: 'Statut',
    cell: ({ row }) => row.original.status
  },
  {
    accessorKey: 'processingDate',
    header: 'Date de traitement',
    cell: ({ row }) => formatDate(row.original.processingDate)
  }
]

const loadData = async () => {
  try {
    const [alertsData, historyData, teachersData] = await Promise.all([
      getOwnerAlerts(props.userId),
      getHistory(props.userId),
      getTeachersList()
    ])
    alerts.value = Array.isArray(alertsData) ? alertsData : []
    history.value = Array.isArray(historyData) ? historyData : []
    teachers.value = Array.isArray(teachersData) ? teachersData : []
  } catch (err) {
    console.error("Erreur lors du chargement des données :", err)
  }
}

const formattedTeachers = computed(() => {
  return teachers.value.map(t => ({
    label: t.id === props.userId ? `${t.name} (Moi)` : t.name,
    value: t.id
  }))
})

const getSelectedTeacherName = computed(() => {
  if (!activeReq.value) return ''
  const selection = selectedTeachers.value[activeReq.value.id]
  if (!selection) return 'aucun professeur'
  if (typeof selection === 'object' && selection.label) return selection.label
  const teacher = teachers.value.find(t => t.id === selection)
  return teacher ? teacher.name : 'Professeur inconnu'
})

const openConfirm = (req) => {
  activeReq.value = req
  isConfirmOpen.value = true
}

const confirmAssignation = async () => {
  const req = activeReq.value
  const selection = selectedTeachers.value[req.id]
  const teacherId = typeof selection === 'object' ? selection.value : selection
  if (!teacherId) return

  isConfirmOpen.value = false
  loadingIds.value = new Set(loadingIds.value).add(req.id)
  try {
    await assignTeacher(req.id, teacherId)
    await loadData()
  } catch (e) {
    console.error("Erreur d'assignation :", e)
  } finally {
    const next = new Set(loadingIds.value)
    next.delete(req.id)
    loadingIds.value = next
  }
}

const openIncompleteConfirm = (req) => {
  incompleteReq.value = req
  isIncompleteConfirmOpen.value = true
}

const confirmMarkIncomplete = async () => {
  const req = incompleteReq.value
  isIncompleteConfirmOpen.value = false
  loadingIncompleteIds.value = new Set(loadingIncompleteIds.value).add(req.id)
  try {
    await markIssueAsIncomplete(req.id)
    await loadData()
  } catch (e) {
    console.error("Erreur lors du marquage incomplet :", e)
  } finally {
    const next = new Set(loadingIncompleteIds.value)
    next.delete(req.id)
    loadingIncompleteIds.value = next
  }
}

const openDetail = async (req) => {
  issueDetail.value = null
  loadingDetail.value = true
  isDetailOpen.value = true
  try {
    issueDetail.value = await getIssueById(req.id)
  } catch (e) {
    console.error("Erreur lors du chargement de l'issue :", e)
  } finally {
    loadingDetail.value = false
  }
}

const getStatusColor = (s) => {
  if (s === 'ACCEPTED') return 'green'
  if (s === 'DENIED') return 'red'
  return 'orange'
}

const translateStatus = (s) => {
  const map = { PENDING: 'En attente', ACCEPTED: 'Accepté', DENIED: 'Refusé' }
  return map[s] || s
}

const formatDate = (d) => {
  if (!d) return 'N/A'
  return new Date(d).toLocaleDateString('fr-FR', { day: '2-digit', month: '2-digit', year: 'numeric' })
}

onMounted(loadData)
</script>
