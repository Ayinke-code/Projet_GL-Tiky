<template>
  <div class="bg-white border-l-4 rounded-lg shadow-sm p-5 hover:shadow-lg transition-all cursor-pointer group border-gray-200" 
       :class="borderClass"
       @click="$emit('click')">
    
    <div class="flex justify-between items-start mb-2">
      <div>
        <p class="text-[10px] font-bold text-indigo-600 uppercase tracking-widest">{{ category.name }}</p>
        <h3 class="text-xl font-bold text-gray-900 mt-1">{{ category.description }}</h3>
      </div>
      <div class="text-gray-300 group-hover:text-indigo-600 transition">
        <i class="fa-solid fa-arrow-right"></i>
      </div>
    </div>

    <div class="mt-4 pt-4 border-t border-gray-50 flex items-center text-xs text-gray-400">
      <i class="fa-solid fa-user-tie mr-2"></i>
      <span class="font-medium">Responsable : {{ category.owner || 'Non assigné' }}</span>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({ 
  category: {
    type: Object,
    required: true
  } 
})
defineEmits(['click'])

const borderClass = computed(() => {
  const colors = ['border-indigo-500', 'border-emerald-500', 'border-amber-500', 'border-rose-500', 'border-blue-500']
  // S'assure que l'ID est un nombre avant de faire le modulo
  const index = (Number(props.category.id) || 0) % colors.length
  return colors[index]
})
</script>