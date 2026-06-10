<template>
  <div class="flex space-x-1 bg-gray-200/80 p-1.5 rounded-xl w-fit mb-8">
    <button
      @click="$emit('switch', 'pending')"
      :class="[
        activeTab === 'pending' ? 'bg-white text-indigo-700 shadow-sm' : 'text-gray-600 hover:text-gray-800 hover:bg-gray-200',
        'px-5 py-2.5 rounded-lg text-sm font-semibold transition-all duration-200 flex items-center gap-2'
      ]"
    >
      <span class="i-lucide-mail"></span> Mes invitations
      <span v-if="pendingCount > 0" class="ml-2 bg-red-100 text-red-600 py-0.5 px-2 rounded-full text-xs">
        {{ pendingCount }}
      </span>
    </button>

    <button
      v-if="isTeacher"
      @click="$emit('switch', 'tracking')"
      :class="[
        activeTab === 'tracking' ? 'bg-white text-indigo-700 shadow-sm' : 'text-gray-600 hover:text-gray-800 hover:bg-gray-200',
        'px-5 py-2.5 rounded-lg text-sm font-semibold transition-all duration-200 flex items-center gap-2'
      ]"
    >
      <span class="i-lucide-users"></span> Mes rendez-vous
    </button>
  </div>
</template>

<script setup>
defineProps({
  activeTab: {
    type: String,
    required: true
  },
  isTeacher: {
    type: Boolean,
    default: false
  },
  pendingCount: {
    type: Number,
    default: 0
  }
})

defineEmits(['switch'])
</script>
