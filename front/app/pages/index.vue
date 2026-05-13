<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useCategory } from '~/composables/useCategory'

const { categories, loading, error, fetchCategories } = useCategory()

onMounted(() => {
  fetchCategories()
})
</script>

<template>
  <div class="min-h-screen bg-gray-50 p-6">
    <div class="max-w-7xl mx-auto">
      <!-- Header -->
      <div class="mb-8">
        <h1 class="text-4xl font-bold text-gray-900 mb-2">Catégories</h1>
        <p class="text-gray-600">Consultez et gérez les catégories de votre cours</p>
      </div>

      <!-- Loading -->
      <div v-if="loading" class="flex justify-center items-center py-12">
        <div class="text-center">
          <svg class="animate-spin h-8 w-8 mx-auto text-indigo-600 mb-3" fill="none" viewBox="0 0 24 24">
            <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" />
            <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8z" />
          </svg>
          <p class="text-gray-600">Chargement des catégories...</p>
        </div>
      </div>

      <!-- Error -->
      <div v-else-if="error" class="p-4 bg-red-50 border border-red-200 rounded-lg text-red-700">
        {{ error }}
      </div>

      <!-- Categories Grid -->
      <div v-else class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        <NuxtLink
          v-for="category in categories"
          :key="category.id || category.idCat"
          :to="`/categorie?id=${category.id || category.idCat}`"
          class="group"
        >
          <div class="bg-white rounded-lg shadow-sm hover:shadow-lg transition-shadow p-6 cursor-pointer border-l-4 border-indigo-600">
            <h3 class="text-lg font-semibold text-gray-900 mb-2 group-hover:text-indigo-600 transition-colors">
              {{ category.nom || category.name }}
            </h3>
            <p class="text-gray-600 text-sm">{{ category.description }}</p>
            <div class="mt-4 pt-4 border-t border-gray-100">
              <span class="text-xs font-medium text-indigo-600">Voir plus →</span>
            </div>
          </div>
        </NuxtLink>
      </div>

      <!-- Empty State -->
      <div v-if="!loading && categories.length === 0" class="text-center py-12">
        <svg class="h-12 w-12 mx-auto text-gray-400 mb-3" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M20 13V6a2 2 0 00-2-2H6a2 2 0 00-2 2v7m16 0v5a2 2 0 01-2 2H6a2 2 0 01-2-2v-5m16 0h-2.586a1 1 0 00-.707.293l-2.414 2.414a1 1 0 01-.707.293h-3.172a1 1 0 01-.707-.293l-2.414-2.414A1 1 0 006.586 13H4" />
        </svg>
        <p class="text-gray-600">Aucune catégorie disponible</p>
      </div>
    </div>
  </div>
</template>
