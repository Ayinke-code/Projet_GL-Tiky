<template>
  <article
    class="rounded-2xl border p-4 shadow-sm transition-colors"
    :class="isMine
      ? 'ml-auto max-w-[85%] border-indigo-200 bg-indigo-50'
      : 'border-gray-200 bg-white'"
  >
    <div
      v-if="replyTo"
      class="mb-3 rounded-xl border-l-4 border-indigo-300 bg-indigo-50 px-3 py-2 text-xs text-indigo-700"
    >
      <p class="font-semibold">
        Réponse à {{ replyTo.auteur }}
      </p>
      <p class="mt-1 line-clamp-2 whitespace-pre-line text-indigo-600">
        {{ replyTo.contenu }}
      </p>
    </div>

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
    <p
      class="whitespace-pre-line text-sm leading-6"
      :class="isMine ? 'text-gray-800' : 'text-gray-700'"
      style="overflow-wrap: anywhere; word-break: break-word; hyphens: auto;"
    >
      {{ contenu }}
    </p>

    <div class="mt-3 flex justify-end">
      <button
        type="button"
        class="text-xs font-medium text-indigo-600 transition-colors hover:text-indigo-800"
        @click="emit('reply')"
      >
        Répondre
      </button>
    </div>
  </article>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  auteur: { type: String, default: 'Anonyme' },
  role: { type: String, default: '' },
  date: { type: [String, Date], default: '' },
  contenu: { type: String, default: '' },
  isMine: { type: Boolean, default: false },
  replyTo: { type: Object, default: null }
})

const emit = defineEmits(['reply'])

const formattedDate = computed(() => {
  if (!props.date) return ''
  const value = new Date(props.date)
  return Number.isNaN(value.getTime()) ? String(props.date) : value.toLocaleString('fr-FR')
})

const displayAuthor = computed(() => props.isMine ? 'Vous' : props.auteur)
</script>
