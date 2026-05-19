<template>
  <UCard class="bg-primary-50/30">
    <template #header>
      <h2 class="text-lg font-semibold text-primary-700">Mes assignations reçues</h2>
    </template>

    <div v-if="tasks.length > 0" class="space-y-3">
      <div v-for="task in tasks" :key="task.id" class="p-4 bg-white border rounded shadow-sm flex items-center justify-between">
        <div class="flex items-center gap-3">
          <UIcon name="i-heroicons-briefcase" class="text-primary-500 w-5 h-5" />
          <div>
            <div class="font-bold">{{ task.issueTitle }}</div>
            <div class="text-xs text-gray-500 italic">Assigné par le propriétaire de la catégorie</div>
          </div>
        </div>

        <div class="flex gap-3">
          <UButton
            color="green"
            variant="soft"
            label="Accepter"
            icon="i-heroicons-check"
            @click="reply(task.id, true)"
          />
          <UButton
            color="red"
            variant="ghost"
            label="Refuser"
            icon="i-heroicons-x-mark"
            @click="reply(task.id, false)"
          />
        </div>
      </div>
    </div>
    <div v-else class="text-center py-8 text-gray-400">
      Aucune assignation en attente de réponse.
    </div>
  </UCard>
</template>

<script setup>
const props = defineProps(['userId'])
const { getTeacherAssignments, respondToRequest } = useAssignation()
const tasks = ref([])

const load = async () => {
  const data = await getTeacherAssignments(props.userId)
  tasks.value = Array.isArray(data) ? data.filter(t => t.status === 'PENDING') : []
}

const reply = async (id, accept) => {
  try {
    await respondToRequest(id, accept, props.userId)
    await load()
  } catch (e) {
    console.error(e)
  }
}

onMounted(load)
</script>
