<template>
    <div class="p-8 min-h-screen bg-gray-50">

        <!-- Header -->
        <div class="flex justify-between items-center mb-8">
            <h1 class="text-2xl font-bold text-gray-800">Groupes d'étudiants</h1>

            <UButton
                v-if="ready && (user?.roles?.includes(ROLE.TEACHER) || user?.roles?.includes(ROLE.ADMIN))"
                @click="showModal = true"
                icon="i-lucide-plus"
                label="Créer un groupe"
                color="primary"
                size="lg"
            />
        </div>

        <!-- Modal sélection type -->
        <CreateGroupModal v-model:open="showModal" />

        <!-- Onglets OPTION / PROJECT -->
        <div class="flex gap-2 mb-6 border-b border-gray-200">
            <button
                v-for="tab in tabs"
                :key="tab.value"
                @click="switchTab(tab.value)"
                :class="[
                    'px-5 py-2.5 text-sm font-medium rounded-t-lg border-b-2 transition-colors cursor-pointer',
                    activeTab === tab.value
                        ? 'border-indigo-600 text-indigo-600'
                        : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300',
                ]"
            >
                <i :class="tab.icon" class="mr-2"></i>
                {{ tab.label }}
            </button>
        </div>

        <!-- Chargement -->
        <div v-if="loading" class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            <USkeleton class="h-36 rounded-2xl" v-for="i in 6" :key="i" />
        </div>

        <!-- Aucun groupe -->
        <div v-else-if="!loading && groups.length === 0" class="text-center py-20">
            <div class="text-gray-300 text-6xl mb-4">
                <i :class="currentTab.icon"></i>
            </div>
            <p class="text-gray-500 text-lg">
                Aucun groupe {{ currentTab.label.toLowerCase() }} pour le moment
            </p>
        </div>

        <!-- Grille des groupes -->
        <div v-else class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            <div
                v-for="group in groups"
                :key="group.id"
                @click="navigateToEdit(group)"
                class="bg-white rounded-2xl shadow-sm border border-gray-100 p-5 hover:shadow-md hover:border-indigo-200 transition-all cursor-pointer flex flex-col h-full"
            >
                <!-- Nom + badge catégorie -->
                <div class="flex items-start justify-between mb-3">
                    <h3 class="font-semibold text-gray-800 text-lg leading-snug">{{ group.name }}</h3>
                    <UBadge
                        v-if="group.categoryName"
                        :color="activeTab === 'OPTION' ? 'warning' : 'success'"
                        variant="soft"
                        size="sm"
                        class="ml-2 shrink-0"
                    >
                        {{ group.categoryName }}
                    </UBadge>
                </div>

                <!-- Créé par -->
                <p class="text-xs text-gray-400 mb-3">
                    Créé par <span class="font-medium text-gray-600">{{ group.createdBy?.name }}</span>
                </p>

                <!-- Membres -->
                <div>
                    <p class="text-xs font-medium text-gray-500 mb-2">
                        {{ group.members?.length ?? 0 }} étudiant(s)
                    </p>
                    <div class="flex flex-wrap gap-1">
                        <span
                            v-for="member in (expandedGroups.has(group.id) ? group.members : group.members?.slice(0, 10))"
                            :key="member.id"
                            class="bg-indigo-50 text-indigo-700 text-xs px-2 py-0.5 rounded-full"
                        >
                            {{ member.name }}
                        </span>
                        <span v-if="!group.members?.length" class="text-gray-300 text-xs">
                            Aucun membre
                        </span>
                    </div>
                    <button
                        v-if="group.members?.length > 10"
                        @click.stop="toggleExpand(group.id)"
                        class="mt-2 text-xs text-indigo-500 hover:text-indigo-700 font-medium transition cursor-pointer"
                    >
                        <i :class="expandedGroups.has(group.id) ? 'fa-solid fa-chevron-up' : 'fa-solid fa-chevron-down'" class="mr-1"></i>
                        {{ expandedGroups.has(group.id) ? 'Voir moins' : `+${group.members.length - 10} de plus` }}
                    </button>
                </div>

                <!-- Bouton suppression -->
                <div class="flex justify-end mt-auto pt-3">
                    <UButton
                        @click.stop="deleteValidation(group)"
                        variant="ghost"
                        size="xs"
                        color="error"
                        icon="i-lucide-trash-2"
                        class="cursor-pointer"
                    />
                </div>            </div>
        </div>

        <ConfirmModal
            v-model:open="isDeleteValidateModalOpen"
            title="Suppression"
            :description="`Voulez-vous vraiment supprimer le groupe : ${deletingGroup?.name}`"
            @confirm="handleDelete"
        />

    </div>
</template>

<script setup>
definePageMeta({ ssr: false })

import CreateGroupModal from '~/components/groups/CreateGroupModal.vue'
import ConfirmModal from '~/components/ui/ConfirmModal.vue'
import { ROLE } from '~/utils/roles.js'

const router = useRouter()
const { user, ready } = useAuth()
const { groups, loading, fetchGroups, deleteGroup } = useGroups()

const showModal = ref(false)

const deletingGroup = ref(null)
const isDeleteValidateModalOpen = ref(false)

const deleteValidation = (group) => {
    deletingGroup.value = { id: group?.id, name: group?.name }
    isDeleteValidateModalOpen.value = true
}

const handleDelete = async () => {
    if (!deletingGroup.value) return
    const id = deletingGroup.value.id
    try {
        await deleteGroup(id)
    } catch (err) {
        console.error('Erreur lors de la suppression', err)
    } finally {
        deletingGroup.value = null
        isDeleteValidateModalOpen.value = false
    }
}

const navigateToEdit = (group) => {
    if (group.type === 'PROMOTION') {
        router.push(`/groups/create/promotion?id=${group.id}`)
    } else {
        router.push(`/groups/create/${group.type}?id=${group.id}`)
    }
}

const expandedGroups = ref(new Set())
const toggleExpand = (groupId) => {
    const next = new Set(expandedGroups.value)
    if (next.has(groupId)) next.delete(groupId)
    else next.add(groupId)
    expandedGroups.value = next
}

const tabs = [
    { value: 'PROMOTION', label: 'Promotions', icon: 'fa-solid fa-users' },
    { value: 'OPTION',  label: 'Options', icon: 'fa-solid fa-code-branch' },
    { value: 'PROJECT', label: 'Projets', icon: 'fa-solid fa-rocket' },
]

const activeTab = ref('OPTION')
const currentTab = computed(() => tabs.find(t => t.value === activeTab.value))

const switchTab = async (type) => {
    activeTab.value = type
    await fetchGroups(type)
}

onMounted(() => fetchGroups('OPTION'))
</script>
