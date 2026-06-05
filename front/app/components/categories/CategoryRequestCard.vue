<template>
    <div @click="handleOpenDetailsModal()"
        class="bg-white border-l-4 rounded-lg shadow-sm p-5 hover:shadow-lg transition-all border-gray-200 relative overflow-hidden h-full flex flex-col cursor-pointer">

        <!-- HEADER -->
        <div class="flex justify-between items-start mb-4">

            <!-- Infos -->
            <div>
                <p class="text-[10px] font-bold text-indigo-600 uppercase tracking-widest">
                    {{ categoryRequest.name }}
                </p>

                <h3 class="text-xl font-bold text-gray-900 mt-1">
                    {{ categoryRequest.subject }}
                </h3>

                <!-- Meta - Date de création et ID -->
                <div class="flex items-center text-xs text-gray-500 gap-2">
                    <span>
                        {{ formatDateTime(categoryRequest.createdAt) }}
                    </span>

                    <span class="w-1 h-1 rounded-full bg-gray-400"></span>

                    <span>
                        ID #{{ categoryRequest.id }}
                    </span>
                </div>
            </div>

            <!-- Badge utilisateur -->
            <template v-if="ready && categoryRequest.requesterId === user?.id">
                <div
                    :id="'category-card-proprio-'+categoryRequest.name"
                    class="text-[10px] bg-blue-100 text-blue-700 px-3 py-1 rounded-full font-bold border border-blue-200">
                    <i class="fa-solid fa-crown mr-1"></i>
                    Propriétaire
                </div>
            </template>

            <template v-else>
                <div
                    class="text-[10px] bg-amber-100 text-amber-700 px-3 py-1 rounded-full font-bold border border-amber-200">
                    <i class="fa-solid fa-user mr-1"></i>
                    {{ categoryRequest.requesterName }}
                </div>
            </template>
        </div>

        <!-- ACTIONS POUR LES PROFESSEURS -->
        <div v-if="ready && user?.roles?.includes(ROLE.TEACHER) && categoryRequest.requesterId !== user?.id"
            class="flex gap-2 mt-auto">

            <button @click.stop="handleAcceptCategoryRequest"
                    id="btn-validate-category"
                class="text-[10px] bg-emerald-600 text-white px-3 py-1 rounded-full font-bold hover:bg-green-800 transition-all cursor-pointer">
                <i class="fa-solid fa-check mr-1"></i>
                Accepter
            </button>

            <!-- US pas dans le Sprint 1 à faire plus tard -->
            <!-- <button @click.stop="handleAcceptCategoryRequest"
                class="text-[10px] bg-indigo-100 text-indigo-700 px-3 py-1 rounded-full font-bold hover:bg-red-600 hover:text-white transition-all cursor-pointer">
                <i class="fa-solid fa-xmark mr-1"></i>
                Refuser
            </button> -->
        </div>

    </div>

    <!-- MODALE DETAIL -->
    <Modal v-model:open="isDetailOpen" title="Détails de la catégorie">

        <!-- Protection rendu -->
        <div v-if="categoryRequest" class="rounded-xl shadow-2xl w-full max-w-lg p-6">

            <!-- Titre -->
            <h2 class="text-xl font-bold mb-2">
                {{ categoryRequest.subject }}
            </h2>

            <!-- Nom -->
            <p class="text-indigo-600 text-sm mb-6">
                {{ categoryRequest.name }}
            </p>

            <!-- Description (optionnel) -->
            <p v-if="categoryRequest?.description" class="text-indigo-600 text-sm mb-6">
                {{ categoryRequest.description }}
            </p>

            <!-- Owner -->
            <div class="text-sm text-gray-500 mb-6">
                Propriétaire : {{ categoryRequest.requesterName }}
            </div>
        </div>
    </Modal>
</template>

<script setup>
import Modal from '../ui/Modal.vue'
import { formatDateTime } from '#imports'
import { ROLE } from '~/utils/roles'

const props = defineProps({
    categoryRequest: {
        type: Object,
        required: true
    }
})

const emit = defineEmits([
    'accept',
    'reject'
])

/**
 * Etat utilisateur authentifié.
 */
const {
    user,
    ready
} = useAuth()

/**
 * Etat modal détail
 */
const isDetailOpen = ref(false)

/**
 * Ouvre la modal avec la demande cliquée
 */
const handleOpenDetailsModal = () => {
    isDetailOpen.value = true
}

/**
 * Émet l'événement "accept" vers le composant parent.
 *
 * Permet de déléguer la validation de la demande au parent
 * (appel API + refresh des données).
 */
const handleAcceptCategoryRequest = () => {
    // renvoie au parent
    emit('accept', props.categoryRequest)
}
</script>
