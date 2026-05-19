<template>
  <article
    class="rounded-2xl border p-4 shadow-sm transition-colors"
    :class="isMine
      ? 'ml-auto max-w-[85%] border-indigo-200 bg-indigo-50'
      : 'border-gray-200 bg-white'"
  >
    <div class="mb-2 flex items-start justify-between gap-4">
      <div>
        <p class="text-sm font-semibold" :class="isMine ? 'text-indigo-900' : 'text-gray-900'">
          {{ displayAuthor }}
        </p>
        <p class="text-xs" :class="isMine ? 'text-indigo-600' : 'text-gray-500'">
          {{ role }}
        </p>
      </div>
      <time class="text-xs" :class="isMine ? 'text-indigo-500' : 'text-gray-400'">
        {{ formattedDate }}
      </time>
    </div>
    <p class="whitespace-pre-line text-sm leading-6" :class="isMine ? 'text-gray-800' : 'text-gray-700'">
      {{ contenu }}
    </p>
  </article>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  auteur: { type: String, default: 'Anonyme' },
  role: { type: String, default: '' },
  date: { type: [String, Date], default: '' },
  contenu: { type: String, default: '' },
  isMine: { type: Boolean, default: false }
})

const formattedDate = computed(() => {
  if (!props.date) return ''
  const value = new Date(props.date)
  return Number.isNaN(value.getTime()) ? String(props.date) : value.toLocaleString('fr-FR')
})

const displayAuthor = computed(() => props.isMine ? 'Vous' : props.auteur)
</script>

