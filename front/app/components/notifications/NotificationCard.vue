<template>
  <div class="p-4 mb-2 rounded-lg border border-gray-100 bg-white shadow-sm hover:shadow-md transition-all duration-200 flex items-start gap-3">

    <div class="flex-shrink-0 mt-0.5">
      <label class="relative flex items-center justify-center cursor-pointer">
        <input
          type="checkbox"
          class="peer sr-only"
          @change="$emit('mark-as-read', notification.id)"
        >
        <div class="w-5 h-5 rounded-full border-2 border-gray-300 peer-checked:border-[#00DC82] peer-checked:bg-[#00DC82] transition-all duration-200 flex items-center justify-center shadow-sm">
          <svg
            class="w-3.5 h-3.5 text-white opacity-0 peer-checked:opacity-100 transition-opacity duration-200"
            fill="none"
            viewBox="0 0 24 24"
            stroke="currentColor"
            stroke-width="3"
          >
            <path stroke-linecap="round" stroke-linejoin="round" d="M4.5 12.75l6 6 9-13.5" />
          </svg>
        </div>
      </label>
    </div>

    <div class="flex-1 min-w-0">
      <h4 class="text-sm font-semibold text-gray-800 truncate">
         Notification
      </h4>
      <p class="text-sm text-gray-600 mt-1 break-words leading-snug">
        {{ notification.content }}
      </p>
    </div>
  </div>
</template>

<script setup>
const props = defineProps({
  notification: {
    type: Object,
    required: true
  }
})

defineEmits(['mark-as-read'])
</script>
