<template>
  <div class="bg-white border-l-4 rounded-lg shadow-sm p-5 hover:shadow-lg transition-all cursor-pointer group border-gray-200 relative overflow-hidden" 
       :class="[borderClass, category.status === 'archived' ? 'grayscale opacity-60' : '']"
       @click="$emit('click')">
    
    <!-- Overlay Archive -->
    <div v-if="category.status === 'archived'" class="absolute inset-0 bg-white/10 flex items-center justify-center z-10 pointer-events-none">
      <span class="text-red-600 text-5xl font-black rotate-[-20deg] opacity-40 border-8 border-red-600 p-4 rounded-xl tracking-widest">ARCHIVE</span>
    </div>

    <!-- Badge Suivi -->
    <div v-if="isFollowed && category.status !== 'archived'" class="absolute top-0 right-0 bg-indigo-600 text-white text-[8px] font-bold px-2 py-1 rounded-bl-lg uppercase tracking-tighter">
      Suivi
    </div>

    <div class="flex justify-between items-start mb-2">
      <div>
        <p class="text-[10px] font-bold text-indigo-600 uppercase tracking-widest">{{ category.name }}</p>
        <h3 class="text-xl font-bold text-gray-900 mt-1">{{ category.subject }}</h3>
      </div>
      
      <div v-if="category.status !== 'archived'" class="flex gap-2">
        <!-- Badge Propriétaire -->
        <div v-if="category.owner === user?.id" class="text-[10px] bg-amber-100 text-amber-700 px-3 py-1 rounded-full font-bold border border-amber-200">
          <i class="fa-solid fa-crown mr-1"></i> PROPRIÉTAIRE
        </div>

        <template v-else>
          <button 
            v-if="isFollowed" 
            @click.stop="$emit('unfollow')"
            class="text-[10px] text-gray-400 hover:text-red-500 transition-colors p-1"
            title="Ne plus suivre"
          >
            <i class="fa-solid fa-heart-circle-minus"></i>
          </button>
          
          <button 
            v-if="!isFollowed" 
            @click.stop="$emit('follow')"
            class="text-[10px] bg-indigo-100 text-indigo-700 px-3 py-1 rounded-full font-bold hover:bg-indigo-600 hover:text-white transition-colors"
          >
            <i class="fa-solid fa-plus mr-1"></i> Suivre
          </button>
        </template>
      </div>
    </div>

    <div class="mt-4 pt-4 border-t border-gray-50 flex flex-col gap-2 text-[10px] text-gray-400">
      <div class="flex items-center">
        <i class="fa-solid fa-user-tie mr-2 text-indigo-400"></i>
        <span class="font-medium">Responsable : {{ category.owner || 'Non assigné' }}</span>
      </div>
      <div class="flex justify-between items-center opacity-70">
        <div class="flex items-center">
          <i class="fa-solid fa-calendar-plus mr-1"></i>
          {{ formatDate(category.createdAt) }}
        </div>
        <div v-if="category.updatedAt" class="flex items-center italic">
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

const { user } = useAuth()

const props = defineProps({ 
  category: {
    type: Object,
    required: true
  },
  isFollowed: {
    type: Boolean,
    default: false
  }
})
defineEmits(['click', 'follow', 'unfollow'])

const formatDate = (dateString) => {
  if (!dateString) return ''
  const date = new Date(dateString)
  return date.toLocaleDateString('fr-FR', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  })
}

const borderClass = computed(() => {
  const colors = ['border-indigo-500', 'border-emerald-500', 'border-amber-500', 'border-rose-500', 'border-blue-500']
  // S'assure que l'ID est un nombre avant de faire le modulo
  const index = (Number(props.category.id) || 0) % colors.length
  return colors[index]
})
</script>