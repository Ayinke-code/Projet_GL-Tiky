<template>
  <div
    class="bg-white border-l-4 rounded-lg shadow-sm p-5 hover:shadow-lg transition-all group border-gray-200 relative overflow-hidden"
    :class="[
      borderClass,
      category.status === 'ARCHIVED' ? 'grayscale opacity-60' : '',
      (ready && !user?.roles?.includes('TEACHER')) ? '' : 'cursor-pointer'
    ]" @click="$emit('click')">

    <!-- Archive overlay -->
    <div v-if="category.status === 'ARCHIVED'"
      class="absolute inset-0 bg-white/10 flex items-center justify-center z-10 pointer-events-none">
      <span
        class="text-red-600 text-5xl font-black rotate-[-20deg] opacity-40 border-8 border-red-600 p-4 rounded-xl tracking-widest">
        ARCHIVE
      </span>
    </div>

    <div class="flex justify-between items-start mb-2">

      <div>
        <p class="text-[10px] font-bold text-indigo-600 uppercase tracking-widest">
          {{ category.name }}
        </p>

        <h3 class="text-xl font-bold text-gray-900 mt-1">
          {{ category.subject }}
        </h3>
      </div>

      <div v-if="category.status !== 'ARCHIVED'" class="flex gap-2">

        <template v-if="category.owner === user?.id || category.owner === user?.email || category.ownerName === user?.name">
          <div class="text-[10px] bg-amber-100 text-amber-700 px-3 py-1 rounded-full font-bold border border-amber-200">
            <i class="fa-solid fa-crown mr-1"></i> PROPRIÉTAIRE
          </div>
        </template>

        <template v-else>

          <button v-if="isFollowed" @click.stop="confirmUnfollow"
            class="text-[10px] bg-emerald-600 text-white px-3 py-1 rounded-full font-bold hover:bg-red-600 transition-colors">
            <i class="fa-solid fa-check mr-1"></i>
            Suivi
          </button>

          <button v-else @click.stop="$emit('follow')"
            class="text-[10px] bg-indigo-100 text-indigo-700 px-3 py-1 rounded-full font-bold hover:bg-indigo-600 hover:text-white">
            <i class="fa-solid fa-plus mr-1"></i>
            Suivre
          </button>

        </template>

      </div>

    </div>

    <div class="mt-4 pt-4 border-t border-gray-50 flex flex-col gap-2 text-[10px] text-gray-400">

      <div class="flex items-center">
        <i class="fa-solid fa-user-tie mr-2 text-indigo-400"></i>
        <span>
          Responsable : {{ category.ownerName || 'Non assigné' }}
        </span>
      </div>

      <div class="flex justify-between items-center">
        <div>
          <i class="fa-solid fa-calendar-plus mr-1"></i>
          {{ formatDate(category.createdAt) }}
        </div>

        <div v-if="category.updatedAt" class="italic">
          <i class="fa-solid fa-clock-rotate-left mr-1"></i>
          {{ formatDate(category.updatedAt) }}
        </div>
      </div>

    </div>

  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useAuth } from '~/composables/useAuth'

/**
 * Récupération de l'utilisateur connecté.
 */
const { user, ready } = useAuth()

/**
 * Events émis par la carte catégorie.
 *
 * - click : ouverture du détail
 * - follow : suivre la catégorie
 * - unfollow : arrêter de suivre
 */
const emit = defineEmits(['click', 'follow', 'unfollow'])

/**
 * Props du composant.
 *
 * @property category  Catégorie affichée
 * @property isFollowed Indique si la catégorie est suivie
 */
const props = defineProps({
  category: Object,
  isFollowed: Boolean
})

/**
 * Demande confirmation avant de ne plus suivre
 * une catégorie.
 */
const confirmUnfollow = () => {
  if (confirm(`Arrêter de suivre "${props.category.subject}" ?`)) {
    emit('unfollow')
  }
}

/**
 * Formate une date au format français.
 *
 * @param date date à formatter
 * @returns date formatée
 */
const formatDate = (date) => {
  if (!date) return ''
  return new Date(date).toLocaleString('fr-FR')
}

/**
 * Génère dynamiquement une couleur de bordure
 * selon l'id de la catégorie.
 */
const borderClass = computed(() => {
  const colors = [
    'border-indigo-500',
    'border-emerald-500',
    'border-amber-500',
    'border-rose-500'
  ]

  const index = (Number(props.category.id) || 0) % colors.length
  return colors[index]
})
</script>
