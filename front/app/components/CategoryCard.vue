<template>
  <div class="bg-white border-l-4 rounded-lg shadow-sm p-5 hover:shadow-lg transition-all cursor-pointer group border-gray-200" 
       :class="borderClass"
       @click="$emit('click')">
    
    <!-- Header -->
    <div class="flex justify-between items-start mb-2">
      <div>
        <p class="text-[10px] font-bold text-indigo-600 uppercase tracking-widest">{{ category.name }}</p>
        <h3 class="text-xl font-bold text-gray-900 mt-1">{{ category.subject }}</h3>
      </div>
      <div class="text-gray-300 group-hover:text-indigo-600 transition">
        <i class="fa-solid fa-arrow-right"></i>
      </div>
    </div>

    <!-- Info Prof -->
    <div class="mt-4 pt-4 border-t border-gray-50 flex items-center text-xs text-gray-400">
      <i class="fa-solid fa-user-tie mr-2"></i>
      <span class="font-medium">Responsable : {{ category.teacher || 'Non assigné' }}</span>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({ category: Object })
defineEmits(['click'])

const colors = ['border-indigo-500', 'border-emerald-500', 'border-amber-500', 'border-rose-500', 'border-blue-500']
const borderClass = computed(() => colors[props.category.id % colors.length])
</script>