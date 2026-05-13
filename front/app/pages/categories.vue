<template>
    <div class="p-8 h-full bg-gray-50">

        <!-- Header page -->
        <div class="flex justify-between items-center mb-8">
            <h1 class="text-2xl font-bold text-gray-800">Gestion des Catégories</h1>

            <!-- Bouton ouverture modal création -->
            <Button @click="isCreateModalOpen = true" icon="i-lucide-plus" label="Créer une catégorie" color="secondary"
                size="lg" variant="solid" rounded />
        </div>

        <!-- MODALE CREATION -->
        <Modal v-model:open="isCreateModalOpen" title="Création de catégorie">

            <!-- Affichage erreur API -->
            <Alert v-if="error" color="error" title="Erreur" description="La catégorie renseignée existe déjà !">
                {{ error }}
            </Alert>

            <!-- Formulaire création -->
            <CategoryForms @submit="handleCreate" @cancel="isCreateModalOpen = false" />
        </Modal>

        <!-- LISTE -->
        <div v-if="categories.length > 0" class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">

            <CategoryCard v-for="cat in categories" :key="cat.id" :category="cat"
                @click="selectedCat = cat; isDetailOpen = true" />

        </div>

        <!-- Etat vide -->
        <p v-else class="text-gray-500">Aucune catégorie</p>

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

import { ref, onMounted } from 'vue'
import { useCategories } from '~/composables/useCategories'
import Button from '~/components/ui/Button.vue'

const {
    categories,
    fetchCategories,
    createCategory,
    deleteCategory,
    error
} = useCategories()

/**
 * Catégorie sélectionnée
 */
const selectedCat = ref(null)

/**
 * Etat modal création
 */
const isCreateModalOpen = ref(false)

/**
 * Etat modal détail
 */
const isDetailOpen = ref(false)

/**
 * Chargement initial des catégories
 */
onMounted(async () => {
    await fetchCategories()
})

/**
 * Création catégorie
 */
const handleCreate = async (data) => {
    const res = await createCategory(data)

    if (!res) return

    await fetchCategories()
    isCreateModalOpen.value = false
}

/**
 * Suppression catégorie
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
</script>
