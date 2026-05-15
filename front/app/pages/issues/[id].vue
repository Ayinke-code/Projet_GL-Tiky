<script setup lang="js">
/**
 * Page détail d'une issue
 * - charge une issue par ID
 * - permet mise à jour status + visibilité
 */

const route = useRoute()
const { ready, user } = useAuth()
const { getIssueById, updateIssueStatus, updateIssuePrivacy } = useIssues()

// ID de l'issue depuis l'URL
const issueId = Number(route.params.id)

// state local
const issue = ref(null)
const loading = ref(true)
const error = ref(null)

/**
 * Chargement initial de l'issue
 */
onMounted(async () => {
    // redirection si non connecté
    if (ready && !user?.value?.id) {
        await navigateTo('/login')
        return
    }

    try {
        issue.value = await getIssueById(issueId)
    } catch (err) {
        error.value = err.message || 'Erreur de chargement'
    } finally {
        loading.value = false
    }
})

/**
 * Marquer l'issue comme terminée
 */
const handleMarkResolved = async () => {
    try {
        await updateIssueStatus(issueId, 'TERMINE')

        // mise à jour locale optimiste
        if (issue.value) {
            issue.value.status = 'TERMINE'
        }
    } catch (err) {
        console.error(err)
    }
}

/**
 * Couleurs des statuts (aligné backend)
 */
const statutColor = (statut) => {
    const map = {
        'En cours': 'bg-blue-100 text-blue-700',
        'résolu': 'bg-green-100 text-green-700',
        'Ouvert': 'bg-yellow-100 text-yellow-700',
        'Fermé': 'bg-gray-100 text-gray-600',
    }
    return map[statut] || 'bg-gray-100 text-gray-600'
}

/**
 * Modals UI
 */
const isResolveModalOpen = ref(false)
const isVisibilityModalOpen = ref(false)

/**
 * Toggle visibilité (public / privé)
 */
const toggleVisibility = async () => {
    try {
        await updateIssuePrivacy(issueId)

        if (issue.value) {
            issue.value.private = !issue.value.private
        }
    } catch (err) {
        console.error(err)
    }
}
</script>

<template>
    <div class="min-h-screen bg-gray-50">
        <!-- Loader -->
        <div v-if="loading" class="flex items-center justify-center min-h-[60vh]">
            <div class="flex flex-col items-center gap-3 text-gray-400">
                <svg class="animate-spin h-8 w-8" fill="none" viewBox="0 0 24 24">
                    <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" />
                    <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8z" />
                </svg>
                <span class="text-sm">Chargement du ticket...</span>
            </div>
        </div>

        <!-- Erreur -->
        <div v-else-if="error" class="p-8">
            <UAlert color="red" :description="error" />
        </div>

        <!-- Contenu principal -->
        <div v-else class="max-w-7xl mx-auto px-4 py-6 space-y-4">

            <!-- Breadcrumb -->
            <div class="flex items-center gap-2 text-sm text-gray-500">
                <NuxtLink to="/issues" class="hover:text-gray-800 flex items-center gap-1 transition-colors">
                    <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24"
                        stroke="currentColor">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                            d="M10 19l-7-7m0 0l7-7m-7 7h18" />
                    </svg>
                    Retour aux tickets
                </NuxtLink>
                <span class="text-gray-300">/</span>
                <span class="text-gray-700 font-medium">Ticket #{{ issueId }}</span>
            </div>

            <!-- Header : titre + actions -->
            <div class="flex flex-col sm:flex-row sm:items-start sm:justify-between gap-3">
                <div class="space-y-2">
                    <h1 class="text-2xl font-bold text-gray-900">{{ issue.title }}</h1>
                    <div class="flex flex-wrap items-center gap-2">
                        <!-- Catégorie -->
                        <span v-if="issue?.categoryId && issue?.categoryName"
                            class="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-semibold bg-emerald-100 text-emerald-700">
                            {{ issue.categoryName }}
                        </span>
                        <!-- Statut -->
                        <span class="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-semibold"
                            :class="statutColor(issue.status)">
                            {{ issue.status }}
                        </span>
                        <!-- Visibilité -->
                        <span class="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-semibold"
                            :class="!issue.private ? 'text-blue-600 bg-white' : 'text-red-600 bg-white'">
                            <svg xmlns="http://www.w3.org/2000/svg" class="h-3 w-3" fill="none" viewBox="0 0 24 24"
                                stroke="currentColor">
                                <path v-if="!issue.private" stroke-linecap="round" stroke-linejoin="round"
                                    stroke-width="2"
                                    d="M12 2a10 10 0 1 0 0 20 10 10 0 1 0 0-20M2 12h20M12 2a15.3 15.3 0 0 1 4 10 15.3 15.3 0 0 1-4 10 15.3 15.3 0 0 1-4-10 15.3 15.3 0 0 1 4-10z" />
                                <path v-else stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                                    d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z" />
                            </svg>
                            {{ issue.private ? 'Privé' : 'Public' }}
                        </span>
                    </div>
                </div>

                <!-- Boutons d'action -->
                <div class="flex items-center gap-2 shrink-0">
                    <!--  -->
                    <!-- Résolu : TODO: Quand on aura les 'Issue Manager', on pourra bloquer ce bouton pour les autres -->
                    <!--  -->
                    <button v-if="issue.status !== 'TERMINE'" @click="isResolveModalOpen = true"
                        :disabled="issue.status === 'résolu'"
                        class="inline-flex items-center gap-1.5 px-3 py-2 rounded-lg border border-emerald-300 text-emerald-700 bg-white hover:bg-emerald-50 text-sm font-medium transition-colors disabled:opacity-40 disabled:cursor-not-allowed cursor-pointer transition-transform duration-200 hover:scale-105">
                        <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24"
                            stroke="currentColor">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
                        </svg>
                        Marquer Résolu
                    </button>
                    <!--  -->
                    <!-- Privé/Public : TODO: Quand on aura les 'Issue Manager', on pourra bloquer ce bouton pour les autres -->
                    <!--  -->
                    <button @click="isVisibilityModalOpen = true"
                        class="inline-flex items-center gap-1.5 px-3 py-2 rounded-lg border text-sm font-medium transition-colors transition-transform duration-200 hover:scale-105 cursor-pointer"
                        :class="issue.private
                            ? 'border-blue-300 text-blue-600 bg-white hover:bg-blue-50'
                            : 'border-red-300 text-red-600 bg-white hover:bg-red-50'">
                        <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24"
                            stroke="currentColor">
                            <path v-if="issue.private" stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                                d="M12 2a10 10 0 1 0 0 20 10 10 0 1 0 0-20M2 12h20M12 2a15.3 15.3 0 0 1 4 10 15.3 15.3 0 0 1-4 10 15.3 15.3 0 0 1-4-10 15.3 15.3 0 0 1 4-10z" />
                            <path v-else stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                                d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z" />
                        </svg>
                        {{ issue.private ? 'Rendre Public' : 'Rendre Privé' }}
                    </button>
                </div>
            </div>

            <!-- Grid principal -->
            <div class="grid grid-cols-1 lg:grid-cols-3 gap-6 items-start">

                <!-- ===== COLONNE GAUCHE : CONVERSATION ===== -->
                <div class="lg:col-span-2 space-y-4">
                    <template v-if="false">
                    <!-- Messages -->
                    <div class="space-y-3">
                        <div v-for="msg in messages" :key="msg.id"
                            class="bg-white rounded-xl border border-gray-200 overflow-hidden">
                            <!-- Header message -->
                            <div class="flex items-center gap-3 px-5 pt-4 pb-3">
                                <div class="h-9 w-9 rounded-full flex items-center justify-center text-sm font-bold shrink-0"
                                    :class="avatarColor(msg.role)">
                                    {{ initiales(msg.auteur) }}
                                </div>
                                <div class="flex-1 min-w-0">
                                    <div class="flex items-center gap-2 flex-wrap">
                                        <span class="font-semibold text-gray-900 text-sm">{{ msg.auteur }}</span>
                                        <span v-if="msg.role === 'ENSEIGNANT'"
                                            class="inline-flex items-center px-2 py-0.5 rounded text-xs font-semibold bg-blue-100 text-blue-700 uppercase tracking-wide">
                                            Enseignant
                                        </span>
                                    </div>
                                    <p class="text-xs text-gray-400 mt-0.5">
                                        {{ msg.role === 'ENSEIGNANT' ? 'Répondu' : 'Posté' }}
                                        le {{ new Date(msg.date).toLocaleDateString('fr-FR', {
                                            day: 'numeric', month:
                                                'long'
                                        }) }}
                                        à {{ new Date(msg.date).toLocaleTimeString('fr-FR', {
                                            hour: '2-digit', minute:
                                                '2-digit'
                                        }) }}
                                    </p>
                                </div>
                            </div>

                            <!-- Corps message -->
                            <div class="px-5 pb-5 text-gray-700 text-sm leading-relaxed"
                                :class="msg.role === 'ENSEIGNANT' ? 'bg-blue-50/40' : ''">
                                <div v-html="msg.contenu" />
                            </div>
                        </div>

                        <!-- État vide -->
                        <div v-if="messages?.length === 0"
                            class="bg-white rounded-xl border border-gray-200 p-10 text-center">
                            <svg xmlns="http://www.w3.org/2000/svg" class="h-10 w-10 mx-auto text-gray-300 mb-3"
                                fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"
                                    d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" />
                            </svg>
                            <p class="text-gray-400 text-sm">Aucun message pour le moment</p>
                        </div>
                    </div>

                    <!-- Zone de réponse -->
                    <div class="bg-white rounded-xl border border-gray-200 overflow-hidden">
                        <!-- Toolbar -->
                        <div class="flex items-center gap-1 px-3 py-2 border-b border-gray-100">
                            <button
                                class="p-1.5 rounded hover:bg-gray-100 text-gray-500 transition-colors font-bold text-sm">B</button>
                            <button
                                class="p-1.5 rounded hover:bg-gray-100 text-gray-500 transition-colors italic text-sm">I</button>
                            <button class="p-1.5 rounded hover:bg-gray-100 text-gray-500 transition-colors">
                                <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24"
                                    stroke="currentColor">
                                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                                        d="M13.828 10.172a4 4 0 00-5.656 0l-4 4a4 4 0 105.656 5.656l1.102-1.101" />
                                </svg>
                            </button>
                            <button class="p-1.5 rounded hover:bg-gray-100 text-gray-500 transition-colors">
                                <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24"
                                    stroke="currentColor">
                                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                                        d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14" />
                                </svg>
                            </button>
                            <button class="p-1.5 rounded hover:bg-gray-100 text-gray-500 transition-colors">
                                <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24"
                                    stroke="currentColor">
                                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                                        d="M9 7h6m0 10v-3m-3 3h.01M9 17h.01M9 14h.01M12 14h.01M15 11h.01M12 11h.01M9 11h.01M7 21h10a2 2 0 002-2V5a2 2 0 00-2-2H7a2 2 0 00-2 2v14a2 2 0 002 2z" />
                                </svg>
                            </button>
                        </div>

                        <!-- Textarea -->
                        <textarea v-model="newMessage" placeholder="Ajouter un commentaire ou une réponse..." rows="4"
                            class="w-full px-4 py-3 text-sm text-gray-700 placeholder-gray-400 resize-y focus:outline-none"
                            @keydown.ctrl.enter="handleSend" />

                        <!-- Footer -->
                        <div class="flex justify-end px-4 pb-3">
                            <button @click="handleSend" :disabled="sending || !newMessage?.trim()"
                                class="inline-flex items-center gap-2 px-5 py-2.5 rounded-lg bg-indigo-600 hover:bg-indigo-700 text-white text-sm font-medium transition-colors disabled:opacity-50 disabled:cursor-not-allowed transition-transform duration-200 hover:scale-105">
                                <svg v-if="sending" class="animate-spin h-4 w-4" fill="none" viewBox="0 0 24 24">
                                    <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor"
                                        stroke-width="4" />
                                    <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8z" />
                                </svg>
                                Envoyer la réponse
                            </button>
                        </div>
                    </div>
                    </template>
                </div>

                <!-- ===== COLONNE DROITE : SIDEBAR ===== -->
                <div class="space-y-4">

                    <!-- Carte INFORMATIONS -->
                    <div class="bg-white rounded-xl border border-gray-200 overflow-hidden">
                        <div class="px-5 py-3 border-b border-gray-100">
                            <h2 class="text-xs font-bold text-gray-500 uppercase tracking-widest">Informations</h2>
                        </div>
                        <div class="px-5 py-4 space-y-3 text-sm">
                            <div class="flex justify-between items-center">
                                <span class="text-gray-500">Auteur</span>
                                <span class="font-medium text-gray-900">{{ issue.createdName || 'Inconnu' }}</span>
                            </div>
                            <!-- TODO -->
                            <div v-if="false" class="flex justify-between items-center">
                                <span class="text-gray-500">Enseignant en charge</span>
                                <span class="font-semibold text-gray-900">{{ issue.professeur || 'Non défini' }}</span>
                            </div>
                            <!-- TODO -->
                            <div v-if="false" class="flex justify-between items-center">
                                <span class="text-gray-500">Abonnés</span>
                                <span class="font-medium text-gray-900">{{ issue.abonnes || 0 }}</span>
                            </div>
                        </div>
                    </div>

                    <!-- Carte RDV -->
                    <div v-if="false" class="bg-white rounded-xl border border-gray-200 overflow-hidden">
                        <div class="px-5 py-3 border-b border-gray-100">
                            <div class="flex items-center gap-2">
                                <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4 text-indigo-500" fill="none"
                                    viewBox="0 0 24 24" stroke="currentColor">
                                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                                        d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" />
                                </svg>
                                <h2 class="text-sm font-semibold text-gray-800">Besoin d'un RDV ?</h2>
                            </div>
                        </div>

                        <div class="px-5 py-4">
                            <!-- Proposition active -->
                            <div v-if="rdvProposal?.active" class="space-y-3">
                                <div class="bg-amber-50 border border-amber-200 rounded-lg p-3 space-y-2">
                                    <p class="text-xs font-bold text-amber-700 uppercase tracking-wide">Nouvelle demande
                                    </p>
                                    <p class="text-sm text-gray-600">
                                        <span class="font-medium">{{ rdvProposal.auteur }}</span> propose un point le :
                                    </p>
                                    <p
                                        class="text-center font-mono text-sm font-semibold text-gray-800 bg-white border border-amber-200 rounded py-2 px-3">
                                        {{ rdvProposal.date }}
                                    </p>
                                </div>
                                <div class="grid grid-cols-2 gap-2">
                                    <button @click="handleRdvValider"
                                        class="w-full py-2 rounded-lg bg-emerald-600 hover:bg-emerald-700 text-white text-sm font-medium transition-colors transition-transform duration-200 hover:scale-105">
                                        Valider
                                    </button>
                                    <button @click="handleRdvRefuser"
                                        class="w-full py-2 rounded-lg border border-red-200 text-red-600 hover:bg-red-50 text-sm font-medium transition-colors transition-transform duration-200 hover:scale-105">
                                        Refuser
                                    </button>
                                </div>
                            </div>

                            <!-- Aucune proposition -->
                            <div v-else class="text-center space-y-3 py-2">
                                <p class="text-sm text-gray-400">Aucune demande de rendez-vous en cours.</p>
                                <button disabled
                                    class="w-full py-2 rounded-lg bg-gray-100 text-gray-400 text-sm font-medium cursor-not-allowed">
                                    Proposer un créneau
                                </button>
                            </div>
                        </div>
                    </div>

                    <!-- Slot pour futures fonctionnalités sidebar -->
                    <slot name="sidebar-extra" />

                </div>
            </div>
        </div>
    </div>
    <div v-if="isResolveModalOpen" class="fixed inset-0 bg-black/40 flex items-center justify-center z-50 p-4"
        @click.self="isResolveModalOpen = false">
        <div
            class="bg-white rounded-xl shadow-2xl w-full max-w-md overflow-hidden animate-in fade-in zoom-in duration-200">

            <div class="p-6 border-b border-gray-100 bg-gray-50 flex justify-between items-center">
                <h2 class="text-lg font-bold text-gray-800">Confirmer</h2>
                <button @click="isResolveModalOpen = false" class="text-gray-400 hover:text-gray-600">
                    ✕
                </button>
            </div>

            <div class="p-6 space-y-4">
                <p class="text-sm text-gray-600">
                    Êtes-vous sûr de vouloir marquer ce ticket comme <span
                        class="font-semibold text-emerald-600">résolu</span> ?
                </p>

                <div class="flex justify-end gap-2">
                    <button @click="isResolveModalOpen = false"
                        class="px-4 py-2 text-sm rounded-lg border border-gray-300 text-gray-600 hover:bg-gray-50">
                        Annuler
                    </button>

                    <button @click="() => { handleMarkResolved(); isResolveModalOpen = false }"
                        class="px-4 py-2 text-sm rounded-lg bg-emerald-600 text-white hover:bg-emerald-700">
                        Confirmer
                    </button>
                </div>
            </div>

        </div>
    </div>
    <div v-if="isVisibilityModalOpen" class="fixed inset-0 bg-black/40 flex items-center justify-center z-50 p-4"
        @click.self="isVisibilityModalOpen = false">
        <div
            class="bg-white rounded-xl shadow-2xl w-full max-w-md overflow-hidden animate-in fade-in zoom-in duration-200">

            <div class="p-6 border-b border-gray-100 bg-gray-50 flex justify-between items-center">
                <h2 class="text-lg font-bold text-gray-800">Changer la visibilité</h2>
                <button @click="isVisibilityModalOpen = false" class="text-gray-400 hover:text-gray-600">
                    ✕
                </button>
            </div>

            <div class="p-6 space-y-4">
                <p class="text-sm text-gray-600">
                    Voulez-vous rendre ce ticket
                    <span class="font-semibold">
                        {{ issue.private ? 'public' : 'privé' }}
                    </span> ?
                </p>

                <div class="flex justify-end gap-2">
                    <button @click="isVisibilityModalOpen = false"
                        class="px-4 py-2 text-sm rounded-lg border border-gray-300 text-gray-600 hover:bg-gray-50">
                        Annuler
                    </button>

                    <button @click="() => { toggleVisibility(); isVisibilityModalOpen = false }"
                        class="px-4 py-2 text-sm rounded-lg bg-indigo-600 text-white hover:bg-indigo-700">
                        Confirmer
                    </button>
                </div>
            </div>

        </div>
    </div>
</template>
