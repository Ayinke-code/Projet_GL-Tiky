<template>
  <!--
    Bordure gauche colorée selon le type : indigo (AUDIT), emerald (DEFENSE) ou amber (RDV).
    Même convention visuelle que CategoryCard.vue.
  -->
  <div
    class="bg-white border-l-4 rounded-lg shadow-sm p-5 hover:shadow-lg transition-all flex flex-col h-full"
    :class="{
      'border-indigo-500': event.categoryType === 'AUDIT',
      'border-emerald-500': event.categoryType === 'DEFENSE',
      'border-amber-500': event.categoryType === 'RDV'
    }"
  >
    <div class="mb-2">
      <!-- Type + nom de la catégorie en petit -->
      <p
        class="text-[10px] font-bold uppercase tracking-widest"
        :class="{
          'text-indigo-600': event.categoryType === 'AUDIT',
          'text-emerald-600': event.categoryType === 'DEFENSE',
          'text-amber-600': event.categoryType === 'RDV'
        }"
      >
        {{ getCategoryLabel }} · {{ event.categoryName }}
      </p>

      <!-- Nom de l'événement -->
      <h3 class="text-lg font-bold text-gray-900 mt-1">{{ event.name }}</h3>

      <!-- Description tronquée si trop longue -->
      <p v-if="event.description" class="text-sm text-gray-500 mt-1 line-clamp-2">
        {{ event.description }}
      </p>
    </div>

    <!-- Section Spécifique RDV : Professeur et/ou Issue -->
    <div v-if="event.categoryType === 'RDV'" class="mt-3 space-y-2">
      <!-- Professeur associé -->
      <div v-if="event.associatedTeacher"
        class="flex items-center gap-2 text-sm text-gray-700 bg-gray-50 p-2 rounded border border-gray-100">
        <i class="fa-solid fa-chalkboard-user text-amber-500 w-4"></i>
        <span class="font-medium">Prof : {{ event.associatedTeacher }}</span>
      </div>

      <!-- Issue / Problématique associée -->
      <div v-if="event.associatedIssue"
        class="flex items-center gap-2 text-sm text-gray-700 bg-gray-50 p-2 rounded border border-gray-100">
        <i class="fa-solid fa-circle-exclamation text-amber-500 w-4"></i>
        <span class="italic">Sujet : {{ event.associatedIssue }}</span>
      </div>

      <!-- Créateur -->
      <div class="mt-3 text-xs text-gray-400">
        <span><i class="fa-solid fa-user mr-1"></i>{{ event.createdBy }}</span>
      </div>
    </div>

    <!-- Plage de dates (même jour, heures différentes) -->
    <div
      v-if="event.startDate && event.endDate"
      class="mt-3 rounded-lg px-3 py-2 text-xs"
      :class="{
        'bg-indigo-50 text-indigo-600': event.categoryType === 'AUDIT',
        'bg-emerald-50 text-emerald-600': event.categoryType === 'DEFENSE',
        'bg-amber-50 text-amber-600': event.categoryType === 'RDV'
      }"
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

    <!-- Groupes projet affectés -->
    <div v-if="event.projectGroups && event.projectGroups.length" class="mt-3">
      <p class="text-[9px] uppercase tracking-widest font-semibold text-gray-400 mb-1.5">
        <i class="fa-solid fa-rocket mr-1"></i>Groupes projet
      </p>
      <div class="flex flex-col gap-2">
        <div v-for="g in event.projectGroups" :key="g.id">
          <span
            class="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-medium
                   bg-emerald-50 text-emerald-700 border border-emerald-200"
          >
            {{ g.name }}
            <span v-if="g.categoryName" class="opacity-60">· {{ g.categoryName }}</span>
            <span v-if="g.parentGroupName" class="opacity-60">· <i class="fa-solid fa-code-branch text-[9px]"></i> {{ g.parentGroupName }}</span>
          </span>
          
          <!-- Enseignants du groupe -->
          <div v-if="g.teachers && g.teachers.length" class="mt-1 ml-1">
            <p class="text-[9px] uppercase tracking-widest font-semibold text-violet-400 mb-1">
              <i class="fa-solid fa-chalkboard-teacher mr-1"></i>Enseignants
            </p>
            <div class="flex flex-wrap gap-1">
              <span
                v-for="teacher in g.teachers"
                :key="teacher"
                class="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px]
                       bg-violet-50 text-violet-700 border border-violet-200"
              >
                <i class="fa-solid fa-chalkboard-teacher text-[9px]"></i>
                {{ teacher }}
              </span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Bouton suppression -->
    <div class="flex justify-end mt-auto pt-3">
      <UButton
        @click.stop="emit('delete', event)"
        variant="ghost"
        size="xs"
        color="error"
        icon="i-lucide-trash-2"
        class="cursor-pointer"
      />
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { formatDateTime, formatDate, formatTime } from '~/utils/helpers'

const props = defineProps({
  event: { type: Object, required: true }
})

// Définition du label selon le type
const getCategoryLabel = computed(() => {
  switch (props.event.categoryType) {
    case 'AUDIT': return 'Audit'
    case 'DEFENSE': return 'Soutenance'
    case 'RDV': return 'Rendez-vous'
    default: return 'Événement'
  }
})

const emit = defineEmits(['delete'])
</script>