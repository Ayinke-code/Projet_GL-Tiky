<template>
  <div class="relative mb-4">
    <div v-if="showMergeHeader" class="flex items-center gap-2 mb-3 mt-6">
      <div class="h-px bg-gray-200 flex-1"></div>
      <div class="text-[10px] font-bold uppercase tracking-wider px-3 py-1 rounded-full flex items-center gap-1.5 shadow-sm" :class="mergeColorClasses.badge">
        <UIcon name="i-lucide-history" class="w-3 h-3" />
        Historique du ticket #{{ originalIssueId }}
      </div>
      <div class="h-px bg-gray-200 flex-1"></div>
    </div>

    <article
      class="rounded-2xl border p-4 shadow-sm transition-all duration-200"
      :class="[
        !isMerged && isMine ? 'ml-auto max-w-[85%] border-indigo-200 bg-indigo-50' : '',

        !isMerged && !isMine ? 'border-gray-200 bg-white' : '',

        isMerged ? mergeColorClasses.card : '',

        isMerged && isMine ? 'ml-auto max-w-[85%]' : ''
      ]"
    >
      <div
        v-if="replyTo"
        class="mb-3 rounded-xl border-l-4 px-3 py-2 text-xs"
        :class="isMerged ? mergeColorClasses.reply : 'border-indigo-300 bg-indigo-50/50 text-indigo-700'"
      >
        <p class="font-semibold">
          Réponse à {{ replyTo.auteur }}
        </p>
        <p class="mt-1 line-clamp-2 whitespace-pre-line opacity-80">
          {{ replyTo.contenu }}
        </p>
      </div>

      <div class="mb-2 flex items-start justify-between gap-4">
        <div>
          <p class="text-sm font-semibold" :class="isMerged ? mergeColorClasses.textDark : (isMine ? 'text-indigo-900' : 'text-gray-900')">
            {{ displayAuthor }}
          </p>
          <p class="text-xs" :class="isMerged ? mergeColorClasses.textMuted : (isMine ? 'text-indigo-600' : 'text-gray-500')">
            {{ role }}
          </p>
        </div>
        <time class="text-xs" :class="isMerged ? mergeColorClasses.textMuted : (isMine ? 'text-indigo-500' : 'text-gray-400')">
          {{ formattedDate }}
        </time>
      </div>
      <p
        :id="'issue-message-'+contenu"
        class="whitespace-pre-line text-sm leading-6"
        :class="isMerged ? mergeColorClasses.textBody : (isMine ? 'text-gray-800' : 'text-gray-700')"
        style="overflow-wrap: anywhere; word-break: break-word; hyphens: auto;"
      >
        {{ contenu }}
      </p>

      <div v-if="files.length" class="mt-4 pt-3 border-t" :class="isMerged ? 'border-black/10' : 'border-gray-100'">
        <div class="text-xs font-semibold mb-2" :class="isMerged ? mergeColorClasses.textMuted : 'text-gray-500'">
          Documents
        </div>

        <div class="space-y-2">
          <div v-for="doc in files" :key="doc.id"
               class="flex items-center justify-between gap-3 p-2.5 rounded-lg transition-colors bg-white/40 hover:bg-white/60">
            <div class="flex items-center gap-3 min-w-0">
              <UIcon :name="getFileIcon(doc.contentType)" class="text-lg shrink-0" :class="isMerged ? mergeColorClasses.textMuted : 'text-gray-500'" />

              <div class="min-w-0">
                <p class="text-xs font-medium truncate" :class="isMerged ? mergeColorClasses.textDark : 'text-gray-900'">
                  {{ doc.originalName }}
                </p>

                <p class="text-[10px]" :class="isMerged ? mergeColorClasses.textMuted : 'text-gray-500'">
                  {{ (doc.size / 1024).toFixed(1) }} KB
                </p>
              </div>
            </div>

            <div class="flex items-center gap-1 shrink-0">
              <UButton icon="i-lucide-download" size="xs" variant="ghost" class="shrink-0 cursor-pointer" :color="isMerged ? 'gray' : 'info'"
                       v-on:click="downloadDoc(doc)" />
            </div>
          </div>
        </div>
      </div>

      <div v-if="!isMerged" class="mt-3 flex justify-end">
        <button
          type="button"
          class="text-xs font-medium text-indigo-600 transition-colors hover:text-indigo-800"
          @click="emit('reply')"
        >
          Répondre
        </button>
      </div>
    </article>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  auteur: { type: String, default: 'Anonyme' },
  role: { type: String, default: '' },
  date: { type: [String, Date], default: '' },
  contenu: { type: String, default: '' },
  files: { type: Array, default: () => [] },
  issue: { type: Object, default: () => ({}) },
  user: { type: Object, default: () => ({}) },
  isMine: { type: Boolean, default: false },
  replyTo: { type: Object, default: null },

  // Nouvelles props pour la fusion
  isMerged: { type: Boolean, default: false },
  originalIssueId: { type: Number, default: null },
  showMergeHeader: { type: Boolean, default: false },
  colorIndex: { type: Number, default: 0 } // Pour alterner les couleurs
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

// Gestion des palettes de couleurs pour les tickets fusionnés
const colorPalettes = [
  // 0 : Vert clair
  {
    badge: 'bg-emerald-100 text-emerald-700 border border-emerald-200',
    card: 'bg-emerald-50 border-emerald-200 opacity-90',
    reply: 'border-emerald-300 bg-emerald-100 text-emerald-800',
    textDark: 'text-emerald-900',
    textBody: 'text-emerald-800',
    textMuted: 'text-emerald-600'
  },
  // 1 : Bleu clair
  {
    badge: 'bg-sky-100 text-sky-700 border border-sky-200',
    card: 'bg-sky-50 border-sky-200 opacity-90',
    reply: 'border-sky-300 bg-sky-100 text-sky-800',
    textDark: 'text-sky-900',
    textBody: 'text-sky-800',
    textMuted: 'text-sky-600'
  },
  // 2 : Orange clair
  {
    badge: 'bg-amber-100 text-amber-700 border border-amber-200',
    card: 'bg-amber-50 border-amber-200 opacity-90',
    reply: 'border-amber-300 bg-amber-100 text-amber-800',
    textDark: 'text-amber-900',
    textBody: 'text-amber-800',
    textMuted: 'text-amber-600'
  }
]

const mergeColorClasses = computed(() => {
  return colorPalettes[props.colorIndex % colorPalettes.length]
})
</script>
