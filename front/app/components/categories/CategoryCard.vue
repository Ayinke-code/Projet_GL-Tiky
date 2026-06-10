<template>
  <div
    class="border-l-4 rounded-lg shadow-sm p-5 hover:shadow-lg transition-all group border-gray-200 relative overflow-hidden"
    :class="[
      borderClass,
      category.status === 'ARCHIVED' ? 'grayscale opacity-60' : '',
      canAccessCategory ? 'cursor-pointer bg-blue-50' : 'bg-white'
    ]" @click="canAccessCategory ? $emit('click') : null">

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

        <template v-if="category.ownerName === user?.name">
            <template v-if="category.ownerId === user?.value?.id">
          <div class="text-[10px] bg-amber-100 text-amber-700 px-3 py-1 rounded-full font-bold border border-amber-200">
          <div :id="'category-card-proprio-' + category.name.replace(/\s+/g, '-')"
            class="text-[10px] bg-amber-100 text-amber-700 px-3 py-1 rounded-full font-bold border border-amber-200">
            <i class="fa-solid fa-crown mr-1"></i> PROPRIÉTAIRE
          </div>
        </template>

        <template v-else>

          <!-- Le follower voit le statut "Suivi" -->
          <span v-if="isFollowed"
                class="text-[10px] bg-emerald-100 text-emerald-700 px-3 py-1 rounded-full font-bold border border-emerald-200">
            <i class="fa-solid fa-check mr-1"></i>
            Suivi
          </span>

          <button v-else-if="isTeacher" @click.stop="$emit('follow')"
                  class="text-[10px] bg-indigo-100 text-indigo-700 px-3 py-1 rounded-full font-bold hover:bg-indigo-600 hover:text-white transition-colors cursor-pointer">
          <button v-else-if="canFollow"
            @click.stop="$emit('follow')"
            class="text-[10px] bg-indigo-100 text-indigo-700 px-3 py-1 rounded-full font-bold hover:bg-indigo-600 hover:text-white">
            <i class="fa-solid fa-plus mr-1"></i>
            Suivre
          </button>

        </template>

      </div>

    </div>

    <div class="mt-4 pt-4 border-t border-gray-50 flex flex-col gap-2 text-[10px] text-gray-400">

      <div v-if="user?.name !== category.ownerName" class="flex items-center">
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
import { ROLE } from '~/utils/roles'

const { user, ready } = useAuth()

const emit = defineEmits(['click', 'follow', 'unfollow'])

const props = defineProps({
  category: Object,
  isFollowed: Boolean
})

const isTeacher = user.value.roles?.includes(ROLE.TEACHER)
const isOwner = user.value.name === props.category.ownerName

const canFollow = computed(() => {
  return ready && user?.value?.roles?.some(role =>
    role === ROLE.TEACHER || role === ROLE.STUDENT
  )
})

/**
 * Vérifie si l'utilisateur peut accéder au détail de la catégorie.
 * Autorisée si Teacher ET (Propriétaire OU Follower).
 */
const canAccessCategory = computed(() => {
  if (!ready || !user?.value) return false

  // Accessible si propriétaire OU si l'utilisateur suit la catégorie
  return isTeacher && (isOwner || props.isFollowed)
  const isTeacherRole =
    user.value.roles?.includes(ROLE.TEACHER)

  const isOwner =
    user.value.name === props.category.ownerName

  return isTeacherRole && isOwner
})

const formatDate = (date) => {
  if (!date) return ''
  return new Date(date).toLocaleString('fr-FR')
}

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
