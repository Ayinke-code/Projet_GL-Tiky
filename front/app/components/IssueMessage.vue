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
      :id="'issue-message-'+contenu"
      class="whitespace-pre-line text-sm leading-6"
      :class="isMine ? 'text-gray-800' : 'text-gray-700'"
      style="overflow-wrap: anywhere; word-break: break-word; hyphens: auto;"
    >
      {{ contenu }}
    </p>

    <!-- Affichage des fichiers, fonction permettant des les télécharger -->
    <div v-if="files.length" class="mt-6">
      <div class="text-sm font-medium text-gray-500 mb-2">
        Documents
      </div>

      <div class="space-y-2">
        <div v-for="doc in files" :key="doc.id"
          class="flex items-center justify-between gap-3 p-3 border rounded-lg hover:bg-gray-50 transition">
          <!-- gauche -->
          <div class="flex items-center gap-3 min-w-0">
            <UIcon :name="getFileIcon(doc.contentType)" class="text-gray-500 text-lg shrink-0" />

            <div class="min-w-0">
              <p class="font-medium text-gray-900 truncate">
                {{ doc.originalName }}
              </p>

              <p class="text-xs text-gray-500">
                {{ (doc.size / 1024).toFixed(1) }} KB
              </p>
            </div>
          </div>

          <div class="flex items-center gap-1 shrink-0">
            <!-- Télécharge -->
            <UButton icon="i-lucide-download" size="sm" variant="ghost" class="shrink-0 cursor-pointer" color="info"
              v-on:click="downloadDoc(doc)" />
          </div>
        </div>
      </div>
    </div>

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
  files: { type: Array, default: () => [] },
  issue: { type: Object, defaut: {} },
  user: { type: Object, defaut: {} },
  isMine: { type: Boolean, default: false },
  replyTo: { type: Object, default: null }
})

const emit = defineEmits(['reply', 'download'])

const downloadDoc = (doc) => {
  emit('download', doc)
}

const formattedDate = computed(() => {
  if (!props.date) return ''
  const value = new Date(props.date)
  return Number.isNaN(value.getTime()) ? String(props.date) : value.toLocaleString('fr-FR')
})

const displayAuthor = computed(() => props.isMine ? 'Vous' : props.auteur)
</script>
