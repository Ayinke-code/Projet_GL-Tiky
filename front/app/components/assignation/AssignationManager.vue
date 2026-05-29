<template>
  <div class="space-y-6">
    <UCard>
      <template #header>
        <div class="flex items-center justify-between">
          <h2 class="text-lg font-semibold">Gérer mes affectations</h2>
          <UBadge color="orange" variant="soft" size="sm">{{ totalAlerts }} en attente</UBadge>
        </div>
      </template>

      <!-- Si on a au moins 1 alerte, on affiche le bloc principal -->
      <div v-if="alerts.length > 0">
        <div class="space-y-4">
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
                id="selector-assignation-teacher"
                v-model="selectedTeachers[req.id]"
                :items="formattedTeachers"
                placeholder="Assigner à..."
                value-key="value"
                label-key="label"
                class="w-56">
                <template #option="{ option }">
                  <div class="flex items-center gap-2">
                    <UIcon v-if="option.isFollower" name="i-heroicons-star" class="text-primary-500 w-4 h-4" />
                    <span :class="{'font-medium': option.isFollower, 'text-gray-600': !option.isFollower}">
                      {{ option.label }}
                    </span>
                  </div>
                </template>
              </USelectMenu>

              <div class="flex flex-col gap-1.5">
                <UButton
                  label="Assigner"
                  id="btn-affecter-manager"
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

        <!-- Si il y a plus de 5 demandes -->
        <div v-if="totalAlerts > 5" class="text-center text-xs text-gray-400 italic mt-4">
          {{ totalAlerts - 5 }} demande(s) supplémentaire(s) en attente de traitement.
        </div>
      </div>

      <!-- Sinon, message vide -->
      <div id="empty-assignation" v-else class="text-center py-8 text-gray-400 italic bg-gray-50/30 rounded-lg border border-dashed mt-4">
        Aucune demande d'affectation à compléter.
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
        Aucune affectation effectuée.
      </div>
    </UCard>

    <!-- Modale : Confirmer l'assignation -->
    <UModal v-model:open="isConfirmOpen" title="Confirmer l'assignation">
      <template #body>
        <p class="text-gray-600">
          Voulez-vous vraiment assigner
          <strong class="text-gray-900">{{ getSelectedTeacherName }}</strong>
          pour traiter le ticket :
        </p>
        <div class="mt-2 p-3 bg-gray-50 rounded border border-gray-100 italic text-sm text-gray-700">
          "{{ activeReq?.issueTitle }}"
        </div>
      </template>
      <template #footer>
        <div class="flex justify-end gap-3">
          <UButton color="neutral" variant="ghost" label="Annuler" @click="isConfirmOpen = false"/>
          <UButton id="btn-confirm-assignation" color="primary" label="Confirmer l'envoi" @click="confirmAssignation"/>
        </div>
      </template>
    </UModal>

    <!-- Modale : Confirmer le marquage incomplet -->
    <UModal v-model:open="isIncompleteConfirmOpen" title="Marquer comme incomplet">
      <template #body>
        <div class="space-y-4">
          <p class="text-gray-600">
            Voulez-vous vraiment marquer le ticket suivant comme incomplet ? Cette action refusera la demande associée.
          </p>
          <div class="p-3 bg-gray-50 rounded border border-gray-100 italic text-sm text-gray-700">
            "{{ incompleteReq?.issueTitle }}"
          </div>

          <div class="space-y-4 mt-4">
            <UFormField label="Raison" required>
              <UInput v-model="incompleteReason" placeholder="Ex: Informations manquantes, Sujet trop vaste..." class="w-full" />
            </UFormField>

            <UFormField label="Commentaire détaillé" required>
              <UTextarea v-model="incompleteComment" placeholder="Précisez ce qui manque ou pose problème..." :rows="3" class="w-full" />
            </UFormField>
          </div>
        </div>
      </template>
      <template #footer>
        <div class="flex justify-end gap-3">
          <UButton color="neutral" variant="ghost" label="Annuler" @click="isIncompleteConfirmOpen = false"/>
          <UButton
            class="bg-red-600 hover:bg-red-700 text-white border-0"
            label="Confirmer le refus"
            :disabled="!incompleteReason || !incompleteComment"
            @click="confirmMarkIncomplete"
          />
        </div>
      </template>
    </UModal>

    <!-- Modale : Détail de l'issue -->
    <IssueDetailModal
      v-model:open="isDetailOpen"
      :issue-detail="issueDetail"
      :loading="loadingDetail"
    />
  </div>
</template>

<script setup>
import IssueDetailModal from '~/components/issues/IssueDetailModal.vue'
const props = defineProps(['userId'])

const { getOwnerAlerts, getHistory, getTeachersList, assignTeacher, markIssueAsIncomplete, getIssueDtoById } = useAssignation()

const alerts = ref([])
const totalAlerts = ref(0)

const history = ref([])
const teachers = ref([])
const selectedTeachers = ref({})

const loadingIds = ref(new Set())
const loadingIncompleteIds = ref(new Set())

const isConfirmOpen = ref(false)
const activeReq = ref(null)

const isIncompleteConfirmOpen = ref(false)
const incompleteReq = ref(null)
const incompleteReason = ref('')
const incompleteComment = ref('')

const isDetailOpen = ref(false)
const issueDetail = ref(null)
const loadingDetail = ref(false)

// changement de l'état dynamiquement
const emit = defineEmits(['issue-updated'])

const columns = [
  { accessorKey: 'issueTitle', header: "Titre du ticket" },
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
    const alertsData = await getOwnerAlerts(props.userId)
    totalAlerts.value = Array.isArray(alertsData) ? alertsData.length : 0
    alerts.value = Array.isArray(alertsData) ? alertsData.slice(0, 5) : []

    let currentCategoryId = null

    if (alerts.value.length > 0) {
      const firstIssueId = alerts.value[0].issueId
      const issueDto = await getIssueDtoById(firstIssueId)
      if (issueDto && issueDto.categoryId) {
        currentCategoryId = issueDto.categoryId
      }
    }

    const [historyData, teachersData] = await Promise.all([
      getHistory(props.userId),
      currentCategoryId ? getTeachersList(currentCategoryId) : Promise.resolve([])
    ])

    history.value = Array.isArray(historyData) ? historyData : []
    teachers.value = Array.isArray(teachersData) ? teachersData : []
    console.log(teachers.value)

  } catch (err) {
    console.error("Erreur lors du chargement des données :", err)
  }
}

const formattedTeachers = computed(() => {
  const followers = teachers.value
    .filter(t => t.follower)
    .map(t => ({
      label: t.id === props.userId ? `${t.name} (Moi)` : t.name,
      value: t.id,
      isFollower: true
    }))

  const others = teachers.value
    .filter(t => !t.follower)
    .map(t => ({
      label: t.id === props.userId ? `${t.name} (Moi)` : t.name,
      value: t.id,
      isFollower: false
    }))

  const options = []

  if (followers.length > 0) {
    options.push({ label: '— Abonnés à la catégorie', value: '__header_followers__', disabled: true })
    options.push(...followers)
  }

  if (others.length > 0) {
    options.push({ label: '— Autres professeurs', value: '__header_others__', disabled: true })
    options.push(...others)
  }

  return options
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
    await assignTeacher(req.issueId, teacherId)
    await loadData()
    emit('issue-updated')
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
  incompleteReason.value = ''
  incompleteComment.value = ''
  isIncompleteConfirmOpen.value = true
}

const confirmMarkIncomplete = async () => {
  const req = incompleteReq.value
  isIncompleteConfirmOpen.value = false
  loadingIncompleteIds.value = new Set(loadingIncompleteIds.value).add(req.id)
  try {
    await markIssueAsIncomplete(req.issueId, incompleteComment.value, incompleteReason.value)
    await loadData()
    emit('issue-updated')
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
    issueDetail.value = await getIssueDtoById(req.issueId)
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
