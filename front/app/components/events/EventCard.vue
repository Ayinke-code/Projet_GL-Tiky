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

    <!-- Créateur -->
    <div class="mt-3 text-xs text-gray-400">
      <span><i class="fa-solid fa-user mr-1"></i>{{ event.createdBy }}</span>
    </div>

    <!-- Plage de dates (même jour, heures différentes) -->
    <div
      v-if="event.startDate && event.endDate"
      class="mt-3 rounded-lg px-3 py-2 text-xs"
      :class="event.categoryType === 'AUDIT' ? 'bg-indigo-50 text-indigo-600' : 'bg-emerald-50 text-emerald-600'"
    >
      <!-- Date unique -->
      <div class="flex items-center gap-1.5 font-semibold mb-2">
        <i class="fa-solid fa-calendar-days text-[10px]"></i>
        <span class="capitalize">{{ formatDate(event.startDate) }}</span>
      </div>
      <!-- Heures début / fin -->
      <div class="grid grid-cols-2 gap-2">
        <div class="flex flex-col gap-0.5">
          <span class="uppercase tracking-wide text-[9px] opacity-60 font-semibold">Début</span>
          <span class="flex items-center gap-1">
            <i class="fa-solid fa-clock text-[10px]"></i>
            {{ formatTime(event.startDate) }}
          </span>
        </div>
        <div class="flex flex-col gap-0.5">
          <span class="uppercase tracking-wide text-[9px] opacity-60 font-semibold">Fin</span>
          <span class="flex items-center gap-1">
            <i class="fa-regular fa-clock text-[10px]"></i>
            {{ formatTime(event.endDate) }}
          </span>
        </div>
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
import { formatDateTime, formatDate, formatTime } from '~/utils/helpers'

defineProps({
  event: { type: Object, required: true }
})
</script>