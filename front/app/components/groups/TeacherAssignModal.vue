<template>
    <UModal v-model:open="open" title="Gérer les enseignants" :ui="{ width: 'sm:max-w-lg' }">
        <template #body>
            <!-- Recherche -->
            <UInput
                v-model="search"
                placeholder="Rechercher un enseignant..."
                icon="i-lucide-search"
                class="mb-4"
            />

            <!-- Chargement -->
            <div v-if="loading" class="flex justify-center py-6">
                <UIcon name="i-lucide-loader-circle" class="animate-spin text-indigo-500 text-2xl" />
            </div>

            <!-- Liste des enseignants -->
            <div v-else class="space-y-2 max-h-80 overflow-y-auto pr-1">
                <div
                    v-for="teacher in filteredTeachers"
                    :key="teacher.id"
                    class="flex items-center justify-between p-3 rounded-xl border transition-colors"
                    :class="isAssigned(teacher.id)
                        ? 'bg-indigo-50 border-indigo-200'
                        : 'bg-gray-50 border-gray-100 hover:border-gray-200'"
                >
                    <div class="flex items-center gap-3">
                        <div
                            class="w-8 h-8 rounded-full flex items-center justify-center text-sm font-semibold"
                            :class="isAssigned(teacher.id) ? 'bg-indigo-500 text-white' : 'bg-gray-200 text-gray-600'"
                        >
                            {{ teacher.name.charAt(0) }}
                        </div>
                        <div>
                            <p class="text-sm font-medium text-gray-800">
                                <span v-if="teacher.id === currentUser?.id" class="font-semibold text-indigo-600">Moi</span>
                                <span v-else>{{ teacher.name }}</span>
                            </p>
                            <span
                                v-if="teacher.teacherType === 'OPTION_LEADER'"
                                class="inline-flex items-center gap-1 text-[10px] font-semibold px-1.5 py-0.5 rounded-full bg-amber-100 text-amber-700"
                            >
                                <i class="fa-solid fa-star text-[8px]"></i> Option Leader
                            </span>
                            <span
                                v-else-if="teacher.teacherType === 'COURSE_LEADER'"
                                class="inline-flex items-center gap-1 text-[10px] font-semibold px-1.5 py-0.5 rounded-full bg-violet-100 text-violet-700"
                            >
                                Course Leader
                            </span>
                        </div>
                    </div>

                    <UButton
                        v-if="!isAssigned(teacher.id)"
                        size="xs"
                        color="primary"
                        variant="soft"
                        icon="i-lucide-plus"
                        :loading="pendingId === teacher.id"
                        @click="assign(teacher.id)"
                    >
                        Assigner
                    </UButton>
                    <UButton
                        v-else
                        size="xs"
                        color="error"
                        variant="soft"
                        icon="i-lucide-x"
                        :loading="pendingId === teacher.id"
                        @click="unassign(teacher.id)"
                    >
                        Retirer
                    </UButton>
                </div>

                <p v-if="filteredTeachers.length === 0" class="text-center text-gray-400 text-sm py-4">
                    Aucun enseignant trouvé
                </p>
            </div>
        </template>
    </UModal>
</template>

<script setup>
const props = defineProps({
    open: { type: Boolean, default: false },
    group: { type: Object, default: null },
})

const emit = defineEmits(['update:open', 'updated'])

const open = computed({
    get: () => props.open,
    set: (v) => emit('update:open', v),
})

const { user: currentUser } = useAuth()

const api = useApi()
const search = ref('')
const loading = ref(false)
const allTeachers = ref([])
const assignedIds = ref(new Set())
const pendingId = ref(null)

// Initialiser les IDs assignés depuis le groupe reçu
watch(() => props.group, (g) => {
    if (g) {
        assignedIds.value = new Set((g.teachers ?? []).map(t => t.id))
    }
}, { immediate: true })

// Charger la liste des enseignants à l'ouverture
watch(() => props.open, async (v) => {
    if (v && allTeachers.value.length === 0) {
        loading.value = true
        try {
            allTeachers.value = await api('/users/byRoles', {
                method: 'POST',
                body: ['TEACHER'],
            })
        } catch {
            allTeachers.value = []
        } finally {
            loading.value = false
        }
    }
})

const filteredTeachers = computed(() => {
    const q = search.value.toLowerCase().trim()
    if (!q) return allTeachers.value
    return allTeachers.value.filter(t => t.name.toLowerCase().includes(q))
})

const isAssigned = (id) => assignedIds.value.has(id)

const assign = async (teacherId) => {
    if (!props.group) return
    pendingId.value = teacherId
    try {
        const updated = await api(`/groups/${props.group.id}/teachers/${teacherId}`, { method: 'PUT' })
        assignedIds.value = new Set((updated.teachers ?? []).map(t => t.id))
        emit('updated', updated)
    } catch (e) {
        console.error('Erreur assignation', e)
    } finally {
        pendingId.value = null
    }
}

const unassign = async (teacherId) => {
    if (!props.group) return
    pendingId.value = teacherId
    try {
        const updated = await api(`/groups/${props.group.id}/teachers/${teacherId}`, { method: 'DELETE' })
        assignedIds.value = new Set((updated.teachers ?? []).map(t => t.id))
        emit('updated', updated)
    } catch (e) {
        console.error('Erreur désassignation', e)
    } finally {
        pendingId.value = null
    }
}
</script>
