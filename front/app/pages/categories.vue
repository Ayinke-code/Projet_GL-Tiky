<template>
    <div class="p-8 pb-8 min-h-screen bg-gray-50">

        <!-- Header page -->
        <div class="flex justify-between items-center mb-8">
            <h1 class="text-2xl font-bold text-gray-800">Gestion des Catégories</h1>

            <!-- Bouton ouverture modal création pour les professeur -->
            <Button v-if="ready && user?.roles?.includes('TEACHER')" @click="isCreateModalOpen = true"
                icon="i-lucide-plus" label="Créer une catégorie" color="secondary" size="lg" variant="solid" rounded />
            <!-- Bouton ouverture modal demande de création pour les étudiants -->
            <Button v-else-if="ready && user?.roles?.includes('STUDENT')" @click="isRequestCreateModalOpen = true"
                icon="i-lucide-plus" label="Demande de catégorie" color="secondary" size="lg" variant="solid" rounded />
        </div>

        <!-- MODALE CREATION -->
        <Modal v-model:open="isCreateModalOpen" title="Création de catégorie">

            <!-- Affichage erreur API -->
            <Alert v-if="categoriesError" color="error" title="Erreur"
                description="La catégorie renseigné existe déja !">
                {{ categoriesError }}
            </Alert>

            <!-- Formulaire création -->
            <CategoryForms @submit="handleCreate" @cancel="isCreateModalOpen = false" />
        </Modal>

        <!-- MODALE DEMANDE DE CREATION -->
        <Modal v-model:open="isRequestCreateModalOpen" title="Demande de catégorie">

            <!-- Formulaire de demande de création -->
            <div class="flex justify-center">
                <CategoryRequestForms @submit="handleRequestCreate" @cancel="isRequestCreateModalOpen = false" />
            </div>
        </Modal>

        <!-- LISTE DES CATEGORIES -->
        <div v-if="categories.length > 0" class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">

            <CategoryCard v-for="cat in categories" :key="cat.id" :category="cat"
                @click="handleClick(cat)" />

        </div>

        <!-- LISTE DES DEMANDES DES ETUDIANTS -->
        <div v-if="categoryRequests.length > 0 && categoryRequests.some(r => r.status === 'PENDING')">
            <Separator class="my-8" />

            <!-- Header page -->
            <div class="flex justify-between items-center mb-8">
                <h1 class="text-2xl font-bold text-gray-800">Demande des étudiants</h1>
            </div>
            <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">

                <!-- Affichage des catégories request si pas encore validé -->
                <CategoryRequestCard v-for="categoryRequest in categoryRequests.filter(r => r.status !== 'APPROVED')" :categoryRequest="categoryRequest"
                    @accept="handleAcceptCategoryRequest" />

            </div>
        </div>

        <!-- MODALE DETAIL -->
        <Modal v-model:open="isDetailOpen" title="Détails de la catégorie">

            <!-- Protection rendu -->
            <div v-if="selectedCat" class="rounded-xl shadow-2xl w-full max-w-lg p-6">

                <!-- Titre -->
                <h2 class="text-xl font-bold mb-2">
                    {{ selectedCat.subject }}
                </h2>

                <!-- Nom -->
                <p class="text-indigo-600 text-sm mb-6">
                    {{ selectedCat.name }}
                </p>

                <!-- Owner -->
                <div class="text-sm text-gray-500 mb-6">
                    Owner : {{ selectedCat.ownerName }}
                </div>

                <!-- Actions -->
                <div class="flex justify-end gap-3">
                    <Button @click="handleDelete(selectedCat.id)" color="error" label="Supprimer" size="lg"
                        variant="subtle" rounded="true">
                        Supprimer
                    </Button>
                </div>

            </div>
        </Modal>

    </div>
</template>

<script setup>
import CategoryForms from '~/components/forms/CategoryForms.vue'
import CategoryCard from '~/components/categories/CategoryCard.vue'
import Modal from '~/components/ui/Modal.vue'
import Alert from '~/components/ui/Alert.vue'
import CategoryRequestForms from '~/components/forms/CategoryRequestForms.vue'
import Separator from '~/components/ui/Separator.vue'
import CategoryRequestCard from '~/components/categories/CategoryRequestCard.vue'

import { ref, onMounted } from 'vue'
import { useCategories } from '~/composables/useCategories'
import Button from '~/components/ui/Button.vue'

/**
 * Composable de gestion des catégories.
 */
const {
    categories,
    fetchCategories,
    createCategory,
    deleteCategory,
    error: categoriesError
} = useCategories()

/**
 * Etat utilisateur authentifié.
 */
const {
    user,
    ready
} = useAuth()

/**
 * Composable de gestion des demandes de catégories.
 */
const {
    categoryRequests,
    fetchCategoryRequests,
    createCategoryRequest,
    acceptCategoryRequest,
    deleteCategoryRequest,
    error: categoryRequestsError
} = useCategoryRequest()

/**
 * Catégorie actuellement sélectionnée.
 */
const selectedCat = ref(null)

/**
 * Etat d'ouverture de la modal de création.
 */
const isCreateModalOpen = ref(false)

/**
 * Etat d'ouverture de la modal de demande de création.
 */
const isRequestCreateModalOpen = ref(false)

/**
 * Etat d'ouverture de la modal de détail.
 */
const isDetailOpen = ref(false)

/**
 * Chargement initial des catégories au montage de la page.
 */
onMounted(async () => {
    await fetchCategories()
    await fetchCategoryRequests()
})

/**
 * Gestion du clic sur une catégorie.
 *
 * Ouvre la modal de détail uniquement si l'utilisateur est autorisé (TEACHER).
 *
 * @param {Object} cat - catégorie sélectionnée
 */
const handleClick = (cat) => {
  if (ready && !user?.value?.roles?.includes('TEACHER')) return

  selectedCat.value = cat
  isDetailOpen.value = true
}

/**
 * Création d'une nouvelle catégorie.
 *
 * Appelle l'API puis rafraîchit la liste des catégories si succès.
 *
 * @param {Object} data - données du formulaire de création
 */
const handleCreate = async (data) => {
    const res = await createCategory(data)

    if (!res) return

    await fetchCategories()
    isCreateModalOpen.value = false
}

/**
 * Suppression d'une catégorie.
 *
 * Ferme la modal de détail, supprime la catégorie côté API,
 * puis recharge la liste des catégories.
 *
 * @param {number} id - identifiant de la catégorie à supprimer
 */
const handleDelete = async (id) => {
    // fermeture modal
    isDetailOpen.value = false

    // suppression API
    await deleteCategory(id)

    // reset sélection
    selectedCat.value = null

    // refresh liste
    await fetchCategories();
}

/**
 * Création d'une demande de catégorie.
 *
 * Envoie la demande à l'API puis ferme la modal.
 * Recharge ensuite l'application pour synchroniser les données.
 *
 * @param {Object} data - données du formulaire de demande
 */
const handleRequestCreate = async (data) => {
    const res = await createCategoryRequest(data)

    if (!res) return

    // fermeture modal
    isRequestCreateModalOpen.value = false

    // rechargement application
    await navigateTo('/categories')
}

/**
 * Acceptation d'une demande de catégorie.
 *
 * Valide la demande côté backend, puis rafraîchit les listes
 * des catégories et des demandes.
 *
 * @param {Object} data - demande à valider (contient l'id)
 */
const handleAcceptCategoryRequest = async (data) => {
    const res = await acceptCategoryRequest(data.id)
    if (!res) return

    await fetchCategories()
    await fetchCategoryRequests()
}
</script>