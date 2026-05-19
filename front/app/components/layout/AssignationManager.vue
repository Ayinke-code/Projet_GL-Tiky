<template>
  <div class="space-y-6">
    <UCard>
      <template #header>
        <div class="flex items-center justify-between">
          <h2 class="text-lg font-semibold">Gérer mes affectations</h2>
          <UBadge color="orange" variant="soft" size="sm">{{ alerts.length }} en attente</UBadge>
        </div>
      </template>

      <div v-if="alerts.length > 0" class="space-y-4">
        <div v-for="req in alerts" :key="req.id" class="flex items-center justify-between p-4 border rounded-xl bg-gray-50/50">
          <div class="flex flex-col">
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

          <div class="flex items-center gap-3">
            <USelectMenu
              v-model="selectedTeachers[req.id]"
              :items="formattedTeachers"
              placeholder="Assigner à..."
              value-attribute="value"
              option-attribute="label"
              class="w-56"
            />
            <UButton
              label="Assigner"
              icon="i-heroicons-paper-airplane"
              :disabled="!selectedTeachers[req.id] || loadingIds.has(req.id)"
              :loading="loadingIds.has(req.id)"
              @click="openConfirm(req)"
            />
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
      <UTable :data="history ?? []" :columns="columns">
        <template #status-cell="{ row }">
          <UBadge :color="getStatusColor(row.original.status)" variant="subtle" size="sm">
            {{ translateStatus(row.original.status) }}
          </UBadge>
        </template>
      </UTable>
    </UCard>
    <UModal v-model="isConfirmOpen" title="Confirmer l'assignation">
      <template #body>
        <p class="text-gray-600">
          Voulez-vous vraiment assigner
          <strong class="text-gray-900">{{ getSelectedTeacherName }}</strong>
          pour traiter l'issue :
        </p>
        <div class="mt-2 p-3 bg-gray-50 rounded border border-gray-100 italic">
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
  </div>
</template>

<script setup>
const props = defineProps(['userId'])

// 1. On ajoute getHistory à la déstructuration du composable
const { getOwnerAlerts, getHistory, getTeachersList, assignTeacher } = useAssignation()

const alerts = ref([])
const history = ref([])
const teachers = ref([])
const selectedTeachers = ref({})
const loadingIds = ref(new Set())

const isConfirmOpen = ref(false)
const activeReq = ref(null)

const columns = [
  { accessorKey: 'issueTitle', header: 'Titre de l\'Issue' },
  { accessorKey: 'creator', header: 'Créé par' },
  { accessorKey: 'target', header: 'Assigné à' },
  {
    accessorKey: 'status',
    header: 'Statut',
    cell: ({ row }) => row.original.status  // sera intercepté par le slot
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
      getHistory(props.userId), // Nouvel appel API
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
  loadingIds.value.add(req.id)

  try {
    await assignTeacher(req.id, teacherId)
    await loadData()
  } catch (e) {
    console.error("Erreur d'assignation :", e)
  } finally {
    loadingIds.value.delete(req.id)
  }
}

const getStatusColor = (s) => {
  if (s === 'ACCEPTED') return 'green'
  if (s === 'DENIED') return 'red'
  return 'orange'
}

const translateStatus = (s) => {
  const map = {
    'PENDING': 'En attente',
    'ACCEPTED': 'Accepté',
    'DENIED': 'Refusé'
  }
  return map[s] || s
}

const formatDate = (d) => {
  if (!d) return 'N/A'
  return new Date(d).toLocaleDateString('fr-FR', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric'
  })
}

onMounted(loadData)
</script>
