<template>
  <!--
    Bordure gauche colorée selon le type : indigo (AUDIT) ou emerald (DEFENSE).
    Même convention visuelle que CategoryCard.vue.
  -->
  <div class="bg-white border-l-4 rounded-lg shadow-sm p-5 hover:shadow-lg transition-all"
    :class="event.categoryType === 'AUDIT' ? 'border-indigo-500' : 'border-emerald-500'">

    <div class="flex justify-between items-start mb-2">
      <div>
        <!-- Type + nom de la catégorie en petit -->
        <p class="text-[10px] font-bold uppercase tracking-widest"
          :class="event.categoryType === 'AUDIT' ? 'text-indigo-600' : 'text-emerald-600'">
          {{ event.categoryType === 'AUDIT' ? 'Audit' : 'Soutenance' }} · {{ event.categoryName }}
        </p>

        <!-- Nom de l'événement -->
        <h3 class="text-lg font-bold text-gray-900 mt-1">{{ event.name }}</h3>

        <!-- Description tronquée si trop longue -->
        <p v-if="event.description" class="text-sm text-gray-500 mt-1 line-clamp-2">
          {{ event.description }}
        </p>
      </div>
    </div>

    <!-- Pied de carte : créateur + date -->
    <div class="flex items-center justify-between mt-3 text-xs text-gray-400">
      <span><i class="fa-solid fa-user mr-1"></i>{{ event.createdBy }}</span>
      <span>{{ formatDateTime(event.createdAt) }}</span>
    </div>

    <!-- Plage de dates -->
    <div
      v-if="event.startDate && event.endDate"
      class="mt-3 grid grid-cols-2 gap-2 text-xs rounded-lg overflow-hidden"
      :class="event.categoryType === 'AUDIT' ? 'text-indigo-600' : 'text-emerald-600'"
    >
      <div class="flex flex-col gap-0.5 px-3 py-2 rounded-lg"
        :class="event.categoryType === 'AUDIT' ? 'bg-indigo-50' : 'bg-emerald-50'">
        <span class="font-semibold uppercase tracking-wide text-[9px] opacity-70">Début</span>
        <span class="flex items-center gap-1">
          <i class="fa-solid fa-calendar-days text-[10px]"></i>
          {{ formatDateTime(event.startDate) }}
        </span>
      </div>
      <div class="flex flex-col gap-0.5 px-3 py-2 rounded-lg"
        :class="event.categoryType === 'AUDIT' ? 'bg-indigo-50' : 'bg-emerald-50'">
        <span class="font-semibold uppercase tracking-wide text-[9px] opacity-70">Fin</span>
        <span class="flex items-center gap-1">
          <i class="fa-solid fa-calendar-check text-[10px]"></i>
          {{ formatDateTime(event.endDate) }}
        </span>
      </div>
    </div>

  </div>
</template>

<script setup>
/**
 * Carte d'affichage d'un événement.
 * Reçoit un EventResponseDto depuis la page events/index.vue.
 * Utilise formatDateTime (déjà présent dans utils/helpers.js).
 */
import { formatDateTime } from '~/utils/helpers'

defineProps({
  event: { type: Object, required: true }
})
</script>