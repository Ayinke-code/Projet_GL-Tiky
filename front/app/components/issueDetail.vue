<script setup lang="ts">
import { ref, onMounted } from 'vue';
import {
  ArrowLeft,
  Check,
  Lock,
  Calendar,
  MoreHorizontal,
  Bold,
  Italic,
  Link,
  Image as ImageIcon,
  Table,
  Circle,
  X
} from 'lucide-react';
import { useApi, type Issue, type Message } from '../hooks/useApi';
import { format } from 'date-fns';
import { fr } from 'date-fns/locale';

const props = defineProps<{
  issueId: number;
}>();

const { getIssueById, getIssueMessages, updateIssueStatus, loading, error } = useApi();
const issue = ref<Issue | null>(null);
const messages = ref<Message[]>([]);
const comment = ref('');
const isResolveModalOpen = ref(false);

const handleResolve = async () => {
  if (!issue.value) return;
  try {
    await updateIssueStatus(issue.value.id, 'Résolu');
    issue.value.statut = 'Résolu';
    isResolveModalOpen.value = false;
  } catch (err) {
    console.error("Erreur lors de la résolution", err);
  }
};

onMounted(async () => {
  try {
    issue.value = await getIssueById(props.issueId);
    messages.value = await getIssueMessages(props.issueId);
  } catch (err) {
    console.error("Failed to load data", err);
  }
});
</script>

<template>
  <div v-if="loading && !issue" class="flex items-center justify-center min-h-[400px]">
    <div class="w-8 h-8 border-2 border-indigo-600 border-t-transparent rounded-full animate-spin"></div>
  </div>

  <div v-else-if="error && !issue" class="p-8 text-center">
    <div class="bg-red-50 text-red-600 p-4 rounded-lg border border-red-100 inline-block">
      {{ error }}
    </div>
  </div>

  <div v-else-if="issue" class="p-6 md:p-10 font-sans">
    <div class="max-w-7xl mx-auto space-y-8">

      <!-- Top Action Bar -->
      <div class="flex flex-col md:flex-row md:items-center justify-between gap-6">
        <div class="space-y-4">
          <div class="flex items-center gap-2 text-sm text-slate-500 font-medium">
            <button class="flex items-center gap-1.5 hover:text-slate-900 transition-colors">
              <ArrowLeft class="w-4 h-4" /> Retour aux tickets
            </button>
            <span class="text-slate-300">/</span>
            <span class="text-slate-400">Ticket #{{ issue.id }}</span>
          </div>

          <h1 class="text-3xl font-extrabold text-slate-900 tracking-tight">
            {{ issue.titre }}
          </h1>

          <div class="flex flex-wrap gap-2.5">
            <span v-if="issue.categorie" class="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-semibold bg-emerald-100 text-emerald-700">
              {{ issue.categorie.nom }}
            </span>
            <span v-else class="px-3 py-1 bg-green-100 text-green-700 rounded-lg text-xs font-bold">Général</span>

            <span class="px-3 py-1 bg-indigo-100 text-indigo-700 rounded-lg text-xs font-bold">{{ issue.statut }}</span>
            <span class="px-3 py-1 bg-slate-100 text-slate-500 rounded-lg text-xs font-bold flex items-center gap-1.5 border border-slate-200/50">
              <Circle class="w-3 h-3 fill-slate-400 text-slate-400" /> Public
            </span>
          </div>
        </div>

        <div class="flex items-center gap-3">
          <button
            @click="isResolveModalOpen = true"
            class="flex items-center gap-2 px-5 py-2.5 bg-white border border-slate-200 text-green-600 rounded-xl text-sm font-bold hover:bg-green-50 hover:border-green-200 transition-all shadow-sm"
          >
            <Check class="w-4 h-4" /> Marquer Résolu
          </button>
          <button class="flex items-center gap-2 px-5 py-2.5 bg-white border border-slate-200 text-slate-700 rounded-xl text-sm font-bold hover:bg-slate-50 hover:border-slate-300 transition-all shadow-sm">
            <Lock class="w-4 h-4" /> Rendre Privé
          </button>
        </div>
      </div>

      <!-- Main Layout -->
      <div class="grid grid-cols-1 lg:grid-cols-12 gap-10">

        <!-- Conversation -->
        <div class="lg:col-span-8 space-y-8">
          <div class="space-y-8">
            <div
              v-for="msg in messages"
              :key="msg.id"
              :class="[
                'rounded-3xl p-8 transition-all',
                msg.role === 'ENSEIGNANT'
                  ? 'bg-[#EEF2FF] border border-indigo-100/50'
                  : 'bg-white border border-slate-100 shadow-sm'
              ]"
            >
              <div class="flex gap-5 items-start mb-6">
                <!-- Avatar -->
                <div :class="[
                  'w-12 h-12 rounded-full flex items-center justify-center text-sm font-black flex-shrink-0 shadow-inner',
                  msg.role === 'ENSEIGNANT' ? 'bg-indigo-600 text-white' : 'bg-blue-100 text-blue-600'
                ]">
                  {{ msg.role === 'ENSEIGNANT' ? 'JD' : msg.auteur.substring(0, 2).toUpperCase() }}
                </div>

                <div class="flex-1 min-w-0">
                  <div class="flex items-center gap-3 mb-1">
                    <span class="font-bold text-slate-900 text-lg">{{ msg.auteur }}</span>
                    <span v-if="msg.role === 'ENSEIGNANT'" class="px-2 py-0.5 bg-indigo-200 text-indigo-700 rounded text-[10px] font-black uppercase tracking-widest">
                      Enseignant
                    </span>
                  </div>
                  <span class="text-xs font-semibold text-slate-400">
                    {{ msg.role === 'ENSEIGNANT' ? 'Répondu le' : 'Posté le' }}
                    {{ format(new Date(msg.date), 'd MMMM à HH:mm', { locale: fr }) }}
                  </span>
                </div>

                <button class="text-slate-300 hover:text-slate-600 transition-colors">
                  <MoreHorizontal class="w-5 h-5" />
                </button>
              </div>

              <div class="prose prose-slate max-w-none text-slate-700 whitespace-pre-wrap leading-relaxed text-[15px]">
                {{ msg.contenu }}
              </div>
            </div>
          </div>

          <!-- Bottom Editor -->
          <div class="bg-white rounded-3xl shadow-lg shadow-slate-200/50 border border-slate-100 overflow-hidden group focus-within:border-indigo-300 transition-all">
            <div class="flex items-center gap-1 p-3 border-b border-slate-50 bg-[#FBFDFF]">
              <button v-for="Icon in [Bold, Italic, Link, ImageIcon, Table]" :key="Icon"
                class="p-2.5 hover:bg-white rounded-lg hover:shadow-sm text-slate-400 hover:text-indigo-600 transition-all">
                <component :is="Icon" class="w-4 h-4" />
              </button>
            </div>
            <textarea
              v-model="comment"
              placeholder="Ajouter un commentaire ou une réponse..."
              class="w-full min-h-[180px] p-8 focus:outline-none text-slate-700 resize-none placeholder:text-slate-300 text-[15px]"
            ></textarea>
            <div class="flex justify-end p-5 bg-[#FBFDFF] border-t border-slate-50">
              <button class="flex items-center gap-2 px-7 py-3 bg-indigo-600 text-white rounded-2xl font-bold hover:bg-indigo-700 transition-all shadow-xl shadow-indigo-200 scale-100 active:scale-95 leading-none">
                Envoyer la réponse
              </button>
            </div>
          </div>
        </div>

        <!-- Right Side Stats -->
        <div class="lg:col-span-4 space-y-8">

          <!-- Info Card -->
          <div class="bg-white rounded-3xl shadow-sm border border-slate-100 p-8">
            <h2 class="text-[11px] font-black text-slate-400 uppercase tracking-[0.2em] mb-8">Informations</h2>
            <div class="space-y-6">
              <div class="flex justify-between items-center text-sm md:text-base">
                <span class="text-slate-400 font-medium tracking-tight">Auteur</span>
                <span class="font-bold text-slate-800">{{ issue.auteur }}</span>
              </div>
              <div class="flex justify-between items-center text-sm md:text-base">
                <span class="text-slate-400 font-medium tracking-tight">Enseignant en charge</span>
                <span class="font-bold text-slate-800">{{ issue.professeur }}</span>
              </div>
              <div class="flex justify-between items-center text-sm md:text-base">
                <span class="text-slate-400 font-medium tracking-tight">Abonnés</span>
                <span class="font-bold text-slate-800">{{ issue.abonnes }}</span>
              </div>
            </div>
          </div>

          <!-- RDV Card -->
          <div class="bg-[#EEF2FF] rounded-3xl shadow-sm border border-indigo-100 p-8 overflow-hidden">
            <div class="flex items-center gap-3.5 mb-8">
              <div class="p-2.5 bg-white rounded-xl text-indigo-600 shadow-sm">
                <Calendar class="w-5 h-5 font-bold" />
              </div>
              <h2 class="font-black text-indigo-950 tracking-tight">Besoin d'un RDV ?</h2>
            </div>

            <div class="bg-[#FFFDF0] border border-[#FEF3C7] rounded-2xl p-5 space-y-4 shadow-sm">
              <div class="flex items-center justify-between">
                <h3 class="text-[10px] font-black text-orange-600 uppercase tracking-[0.15em]">Nouvelle demande</h3>
              </div>
              <p class="text-[13px] text-slate-700 font-medium leading-normal">
                <strong class="text-slate-900">Léo</strong> propose un point le :
              </p>
              <div class="bg-white py-3 px-4 rounded-xl text-center font-extrabold text-slate-800 shadow-sm border border-orange-100">
                Jeudi 15 Oct. à 10h15
              </div>
              <div class="flex gap-3 pt-2">
                <button class="flex-1 py-3 bg-[#10A354] text-white rounded-xl text-[13px] font-bold hover:bg-green-700 transition-all shadow-md shadow-green-100">
                  Valider
                </button>
                <button class="flex-1 py-3 bg-white border border-slate-200 text-slate-600 rounded-xl text-[13px] font-bold hover:bg-red-50 hover:text-red-600 hover:border-red-100 transition-all">
                  Refuser
                </button>
              </div>
            </div>
          </div>
        </div>

      </div>
    </div>

    <!-- MODALE CONFIRMATION RÉSOLUTION -->
    <div v-if="isResolveModalOpen" class="fixed inset-0 bg-black/40 flex items-center justify-center z-[100] p-4" @click.self="isResolveModalOpen = false">
      <div class="bg-white rounded-2xl shadow-2xl w-full max-w-md p-0 overflow-hidden animate-in fade-in zoom-in duration-200">
        <div class="p-6 border-b border-slate-100 flex justify-between items-center bg-slate-50">
          <h2 class="text-lg font-bold text-slate-800">Confirmer la résolution</h2>
          <button @click="isResolveModalOpen = false" class="text-slate-400 hover:text-gray-600">
             <X class="w-5 h-5" />
          </button>
        </div>
        <div class="p-8 space-y-6">
          <p class="text-slate-600 leading-relaxed">
            Êtes-vous sûr de vouloir marquer ce ticket comme <span class="font-bold text-indigo-600">résolu</span> ?
            Cette action informera l'auteur que le problème a été traité.
          </p>
          <div class="flex gap-4 pt-2">
            <button
              @click="isResolveModalOpen = false"
              class="flex-1 py-3 px-4 border border-slate-200 text-slate-600 rounded-xl font-bold hover:bg-slate-50 transition-all"
            >
              Annuler
            </button>
            <button
              @click="handleResolve"
              :disabled="loading"
              class="flex-1 py-3 px-4 bg-green-600 text-white rounded-xl font-bold hover:bg-green-700 transition-all shadow-lg shadow-green-100 disabled:opacity-50"
            >
              {{ loading ? 'En cours...' : 'Confirmer' }}
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
