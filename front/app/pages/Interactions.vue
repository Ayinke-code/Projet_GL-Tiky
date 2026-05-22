<template>
  <div class="max-w-7xl mx-auto p-6 space-y-6">

    <!-- ONGLETS -->
    <div class="flex items-center space-x-8 border-b border-gray-200 pb-4 mb-8">
      <button class="text-gray-500 hover:text-gray-700 flex items-center space-x-2 font-medium">
        <UIcon name="i-heroicons-users" class="w-5 h-5" />
        <span>Rendez-vous (1-to-1)</span>
      </button>
      <button class="text-blue-600 border-b-2 border-blue-600 pb-4 -mb-[17px] flex items-center space-x-2 font-medium">
        <UIcon name="i-heroicons-clipboard-document-list" class="w-5 h-5" />
        <span>Audits & Oraux (Responsable)</span>
      </button>
    </div>

    <!-- EN-TÊTE DE PAGE -->
    <div class="flex justify-between items-center">
      <div>
        <h1 class="text-2xl font-bold text-gray-900">Plannings & Rencontres</h1>
        <p class="text-gray-500 text-sm mt-1">Planifiez des sessions d'examens impliquant plusieurs professeurs et
          groupes d'élèves.</p>
      </div>

      <UButton icon="i-heroicons-plus" size="lg" color="black" label="Créer une session d'Audit"
        @click="isModalOpen = true" />
    </div>

 
    <!-- dernière session créé -->
    <div v-if="pending" class="animate-pulse space-y-4 mt-8">
      <div class="h-64 bg-gray-100 rounded-xl w-full border border-gray-200"></div>
    </div>

    <div v-else-if="currentAudit"
      class="bg-white rounded-xl shadow-sm border border-gray-200 overflow-hidden relative mt-8 transition-all hover:shadow-md">
      <!-- Liseré de couleur dynamique -->
      <div class="absolute left-0 top-0 bottom-0 w-1.5" :style="{ backgroundColor: currentAudit.color || '#4f46e5' }">
      </div>

      <div class="p-6 pl-8">
        <div class="flex justify-between items-start mb-6">
          <div>
            <h2 class="text-2xl font-bold text-gray-900 mb-3">{{ currentAudit.title }}</h2>
            <div class="flex flex-wrap gap-3 text-sm mt-2">
              <span
                class="bg-blue-50 text-blue-700 px-3 py-1 rounded-md font-medium flex items-center border border-blue-100">
                <UIcon name="i-heroicons-academic-cap" class="w-4 h-4 mr-1.5" />
                Sujet : {{ currentAudit.details.sujet || 'Non défini' }}
              </span>
              <span
                class="bg-purple-50 text-purple-700 px-3 py-1 rounded-md font-medium flex items-center border border-purple-100">
                <UIcon name="i-heroicons-map-pin" class="w-4 h-4 mr-1.5" />
                Salle : {{ currentAudit.details.salle || 'À définir' }}
              </span>
              <span
                class="flex items-center text-gray-600 bg-gray-50 px-3 py-1 rounded-md border border-gray-200 font-medium">
                <UIcon name="i-heroicons-calendar" class="w-4 h-4 mr-1.5 text-gray-500" />
                Période : {{ periodeFormattee }}
              </span>
            </div>
          </div>

          <UBadge :color="currentAudit.statut === 'EN PRÉPARATION' ? 'blue' : 'emerald'" variant="subtle"
            class="uppercase font-bold tracking-wider text-[11px] px-3 py-1.5 rounded-md">
            {{ currentAudit.statut || 'EN PRÉPARATION' }}
          </UBadge>
        </div>

        <div class="grid grid-cols-1 md:grid-cols-2 gap-6 mt-8">
          <!-- Jury -->
          <div class="rounded-xl border border-gray-200 p-5 bg-gray-50/30">
            <h3 class="font-semibold text-gray-900 mb-4 flex items-center">
              <UIcon name="i-heroicons-users" class="w-5 h-5 mr-2 text-gray-500" />
              Jury (Professeurs)
            </h3>
            <ul class="space-y-3">
              <li v-for="prof in currentAudit.details.jury" :key="prof"
                class="flex justify-between items-center bg-white p-2.5 rounded-lg border border-gray-100 shadow-sm">
                <span class="text-gray-700 text-sm font-medium">{{ prof }}</span>
                <span
                  class="flex items-center text-[11px] font-medium text-amber-700 bg-amber-50 px-2 py-1 rounded-md border border-amber-100">
                  <UIcon name="i-heroicons-clock" class="w-3 h-3 mr-1" />
                  En attente
                </span>
              </li>
              <li v-if="!currentAudit.details.jury?.length" class="text-sm text-gray-400 italic">Aucun professeur
                assigné</li>
            </ul>
          </div>

          <!-- Groupes -->
          <div class="rounded-xl border border-gray-200 p-5 bg-gray-50/30">
            <h3 class="font-semibold text-gray-900 mb-4 flex items-center">
              <UIcon name="i-heroicons-user-group" class="w-5 h-5 mr-2 text-gray-500" />
              Candidats (Groupes)
            </h3>
            <div class="flex flex-wrap gap-2">
              <span v-for="groupe in currentAudit.details.groupes" :key="groupe"
                class="inline-flex items-center px-3 py-1.5 rounded-lg text-sm font-medium bg-white border border-gray-200 text-gray-700 shadow-sm">
                {{ groupe }}
              </span>
              <span v-if="!currentAudit.details.groupes?.length" class="text-sm text-gray-400 italic">Aucun groupe
                assigné</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div v-else class="text-center py-16 bg-gray-50 rounded-xl border-2 border-dashed border-gray-200 mt-8">
      <UIcon name="i-heroicons-calendar" class="w-12 h-12 text-gray-300 mx-auto mb-3" />
      <h3 class="text-lg font-medium text-gray-900">Aucun audit planifié</h3>
      <p class="text-gray-500 text-sm mt-1">Commencez par créer une nouvelle session.</p>
      <UButton color="gray" variant="outline" class="mt-4" @click="isModalOpen = true">
        Créer maintenant
      </UButton>
    </div>
<!-- POPUP FORMULAIRE -->
<Teleport to="body">
  <transition name="pop">
    <div v-if="isModalOpen" class="fixed inset-0 z-[999] flex items-center justify-center bg-slate-900/60 backdrop-blur-md p-4" @click.self="isModalOpen = false">
      
      <div class="w-full max-w-2xl bg-white rounded-[32px] shadow-[0_30px_100px_rgba(0,0,0,0.4)] overflow-hidden flex flex-col border border-white/20">
        
        <!-- HEADER -->
        <div class="px-8 py-6 bg-slate-50 border-b border-slate-100 flex justify-between items-center shrink-0">
          <div class="text-left">
            <h2 class="text-2xl font-black text-slate-900">Nouvelle session</h2>
            <p class="text-[10px] font-bold text-indigo-600 uppercase tracking-[0.2em] mt-1 flex items-center">
              <span class="w-2 h-2 bg-indigo-500 rounded-full mr-2"></span>
              Configuration Audit
            </p>
          </div>
          <button @click.stop="isModalOpen = false" class="p-2 rounded-full bg-white shadow-sm border border-slate-200 hover:bg-red-50 hover:text-red-500 transition-all cursor-pointer z-50">
            <UIcon name="i-heroicons-x-mark-20-solid" class="w-5 h-5" />
          </button>
        </div>

        <!-- FORMULAIRE (Scrollable) -->
        <form @submit.prevent="submitForm" class="flex-1 overflow-y-auto max-h-[70vh] bg-white custom-scrollbar">
          <div class="p-8 space-y-10">
            
            <!-- SECTION 1 : GÉNÉRAL -->
            <div class="space-y-6">
              <div class="flex items-center space-x-2 text-slate-400 border-b border-slate-50 pb-2">
                <UIcon name="i-heroicons-identification" class="w-4 h-4" />
                <span class="text-[10px] font-extrabold uppercase tracking-widest">Informations Générales</span>
              </div>
              
              <div class="flex flex-col space-y-2">
                <label class="text-[11px] font-black text-slate-500 ml-1">TITRE DE LA SESSION</label>
                <UInput v-model="form.title" size="xl" placeholder="Soutenance de fin d'étude..." 
                  :ui="{ base: 'h-12 bg-slate-50 ring-1 ring-slate-200 rounded-xl focus:ring-2 focus:ring-indigo-500' }" />
              </div>

              <div class="grid grid-cols-2 gap-6">
                <div class="flex flex-col space-y-2">
                  <label class="text-[11px] font-black text-slate-500 ml-1 uppercase">Sujet / Thème</label>
                  <UInput v-model="form.sujet" size="lg" icon="i-heroicons-tag" placeholder="Ex: Informatique"
                    :ui="{ base: 'h-11 bg-slate-50 ring-1 ring-slate-200 rounded-xl' }" />
                </div>
                <div class="flex flex-col space-y-2">
                  <label class="text-[11px] font-black text-slate-500 ml-1 uppercase">Couleur</label>
                  <div class="flex items-center justify-around h-11 bg-slate-50 px-2 rounded-xl ring-1 ring-slate-200">
                    <button v-for="color in colors" :key="color" type="button" @click.stop="form.color = color"
                      class="w-5 h-5 rounded-full transition-all hover:scale-125 cursor-pointer"
                      :class="form.color === color ? 'ring-2 ring-offset-2 ring-indigo-500 scale-110' : ''"
                      :style="{ backgroundColor: color }" />
                  </div>
                </div>
              </div>
            </div>

            <!-- SECTION 2 : PLANNING -->
            <div class="p-6 rounded-[24px] bg-indigo-50/40 border border-indigo-100 space-y-6">
              <div class="flex items-center space-x-2 text-indigo-400 pb-2">
                <UIcon name="i-heroicons-calendar-days" class="w-4 h-4" />
                <span class="text-[10px] font-extrabold uppercase tracking-widest">Date & Localisation</span>
              </div>

              <div class="grid grid-cols-2 gap-6">
                <div class="flex flex-col space-y-2">
                  <label class="text-[10px] font-black text-indigo-900/40 uppercase ml-1">Début</label>
                  <UInput v-model="form.start" type="datetime-local" size="lg" 
                    :ui="{ base: 'bg-white ring-1 ring-indigo-100 rounded-xl h-11' }" />
                </div>
                <div class="flex flex-col space-y-2">
                  <label class="text-[10px] font-black text-indigo-900/40 uppercase ml-1">Fin</label>
                  <UInput v-model="form.end" type="datetime-local" size="lg" 
                    :ui="{ base: 'bg-white ring-1 ring-indigo-100 rounded-xl h-11' }" />
                </div>
              </div>

              <div class="flex flex-col space-y-2">
                <label class="text-[10px] font-black text-indigo-900/40 uppercase ml-1">Salle ou lien de visioconférence</label>
                <UInput v-model="form.salle" size="lg" icon="i-heroicons-map-pin" placeholder="Amphi B, Zoom, Teams..."
                  :ui="{ base: 'bg-white ring-1 ring-indigo-100 rounded-xl h-11' }" />
              </div>
            </div>

            <!-- SECTION 3 : PARTICIPANTS -->
            <div class="space-y-6">
              <div class="flex items-center space-x-2 text-slate-400 border-b border-slate-50 pb-2">
                <UIcon name="i-heroicons-users" class="w-4 h-4" />
                <span class="text-[10px] font-extrabold uppercase tracking-widest">Intervenants</span>
              </div>
              
              <div class="grid grid-cols-2 gap-6">
                <div class="flex flex-col space-y-2">
                  <label class="text-[11px] font-black text-slate-500 ml-1 uppercase">Jury (Profs)</label>
                  <USelectMenu v-model="form.jury" :options="listeProfs" multiple size="lg" 
                    :ui="{ base: 'bg-slate-50 rounded-xl ring-1 ring-slate-200 min-h-[44px]' }"
                    :popper="{ strategy: 'fixed' }" />
                </div>
                <div class="flex flex-col space-y-2">
                  <label class="text-[11px] font-black text-slate-500 ml-1 uppercase">Candidats (Groupes)</label>
                  <USelectMenu v-model="form.groupes" :options="listeGroupes" multiple size="lg" 
                    :ui="{ base: 'bg-slate-50 rounded-xl ring-1 ring-slate-200 min-h-[44px]' }"
                    :popper="{ strategy: 'fixed' }" />
                </div>
              </div>
            </div>
          </div>
        </form>

        <!-- FOOTER -->
        <div class="px-8 py-6 bg-slate-50 border-t border-slate-100 flex items-center justify-between shrink-0">
          <div class="hidden sm:flex items-center bg-white px-3 py-1.5 rounded-full border border-slate-200 shadow-sm">
            <span class="h-2 w-2 rounded-full bg-green-500 animate-pulse mr-2"></span>
            <span class="text-[9px] font-bold text-slate-500 uppercase tracking-tight">Système prêt</span>
          </div>
          
          <div class="flex items-center space-x-4 ml-auto">
            <button @click.stop="isModalOpen = false" type="button" class="text-xs font-bold text-slate-400 hover:text-slate-600 transition-colors cursor-pointer px-2 py-2">
              Annuler
            </button>
            <UButton type="submit" size="xl"
              class="px-8 py-3 rounded-2xl bg-indigo-600 hover:bg-indigo-700 text-white font-bold shadow-xl shadow-indigo-200 transition-all hover:scale-[1.02] active:scale-[0.98] cursor-pointer"
              @click="submitForm"
            >
              Générer la session
            </UButton>
          </div>
        </div>

      </div>
    </div>
  </transition>
</Teleport>
  </div>
</template>

<script setup>
/**
 * @file ManagementAudits.vue
 * @description Composant de gestion des plannings d'audits et d'examens.
 * Ce composant permet de configurer des sessions d'examens impliquant
 * des jurys (professeurs) et des candidats (groupes d'élèves).
 * 
 * @requires useEvents - Hook personnalisé pour la gestion des événements
 * @requires useToast - Hook Nuxt UI pour les notifications
 */

import { computed, onMounted, ref, reactive } from 'vue'

// --- ÉTATS (STATE) ---

/** @type {Object} Hook de gestion des événements */
const { events, fetchEvents } = useEvents()

/** @type {Ref<boolean>} Indicateur de chargement global */
const pending = ref(true)

/** @type {Ref<boolean>} État de visibilité de la modale de création */
const isModalOpen = ref(false)

/** @type {Ref<boolean>} État de soumission du formulaire */
const isSubmitting = ref(false)

/** @type {Object} Instance de notification */
const toast = useToast()

/** @type {Ref<Array>} Liste des professeurs récupérée du backend */
const listeProfs = ref([])

/** @type {Ref<Array>} Liste des noms de groupes d'élèves */
const listeGroupes = ref([])

/** @type {Array<string>} Palette de couleurs disponibles pour les sessions */
const colors = ['#4f46e5', '#0ea5e9', '#10b981', '#f59e0b', '#ec4899', '#8b5cf6']

/** 
 * @constant {Object} form
 * @description Modèle de données réactif pour la création d'une session
 */
const form = reactive({
  title: '',
  start: '',
  end: '',
  sujet: '',
  salle: '',
  jury: [],
  groupes: [],
  color: '#4f46e5'
})

// --- MÉTHODES (LOGIQUE) ---

/**
 * @function loadData
 * @async
 * @description Charge les données initiales (professeurs et élèves) depuis l'API Java.
 * @throws {Error} Si l'appel API échoue
 */
const loadData = async () => {
  try {
    const [profs, eleves] = await Promise.all([
      $fetch('http://localhost:8080/api/users/teachers'),
      $fetch('http://localhost:8080/api/users/student')
    ])
    
    listeProfs.value = profs
    listeGroupes.value = eleves.map(e => e.nom || e.name)
  } catch (error) {
    console.error("Erreur lors du chargement des données:", error)
    toast.add({ 
      title: 'Erreur de synchronisation', 
      description: 'Impossible de récupérer la liste des intervenants.', 
      color: 'red' 
    })
  } finally {
    pending.value = false
  }
}

/**
 * @function resetForm
 * @description Réinitialise les champs du formulaire à leurs valeurs par défaut.
 */
const resetForm = () => {
  Object.assign(form, {
    title: '', start: '', end: '', sujet: '', salle: '', jury: [], groupes: [], color: '#4f46e5'
  })
}

/**
 * @function submitForm
 * @async
 * @description Valide et envoie les données du formulaire au backend Java.
 * Formate les dates au standard LocalDateTime attendu par Spring Boot.
 */
const submitForm = async () => {
  // Validation de base
  if (!form.title || !form.start || !form.end) {
    toast.add({ 
      title: 'Champs manquants', 
      description: 'Veuillez renseigner au minimum le titre et les dates.', 
      color: 'orange' 
    })
    return
  }

  isSubmitting.value = true

  try {
    // Encapsulation des métadonnées dans la description (JSON)
    const detailsAudit = {
      sujet: form.sujet,
      salle: form.salle,
      jury: form.jury,
      groupes: form.groupes
    }

    // Préparation de l'objet de données selon l'entité Java Event
    const eventPayload = {
      title: form.title,
      start: form.start.includes('T') ? `${form.start}:00` : `${form.start.replace(' ', 'T')}:00`,
      end: form.end.includes('T') ? `${form.end}:00` : `${form.end.replace(' ', 'T')}:00`,
      statut: "EN PRÉPARATION",
      color: form.color,
      description: JSON.stringify(detailsAudit)
    }

    await $fetch('http://localhost:8080/api/events', {
      method: 'POST',
      body: eventPayload
    })

    toast.add({ title: 'Succès', description: 'La session d\'audit a été planifiée.', color: 'green' })
    
    await fetchEvents()
    isModalOpen.value = false
    resetForm()
  } catch (error) {
    console.error("Erreur backend:", error)
    toast.add({ 
      title: 'Erreur de sauvegarde', 
      description: 'Le serveur n\'a pas pu enregistrer la session.', 
      color: 'red' 
    })
  } finally {
    isSubmitting.value = false
  }
}

// --- PROPRIÉTÉS CALCULÉES (COMPUTED) ---

/**
 * @computed currentAudit
 * @returns {Object|null} Le dernier audit créé avec ses détails parsés.
 */
const currentAudit = computed(() => {
  if (!events.value?.length) return null
  
  const lastEvent = events.value[events.value.length - 1]
  let details = { sujet: '', salle: '', jury: [], groupes: [] }
  
  try { 
    if (lastEvent.description) {
      details = JSON.parse(lastEvent.description)
    }
  } catch (e) {
    console.warn("Erreur de parsing de la description de l'événement")
  }
  
  return { ...lastEvent, details }
})

/**
 * @computed periodeFormattee
 * @returns {string} Date de début formatée pour l'affichage local (fr-FR).
 */
const periodeFormattee = computed(() => {
  if (!currentAudit.value?.start) return 'Non définie'
  return new Date(currentAudit.value.start).toLocaleDateString('fr-FR', {
    day: 'numeric',
    month: 'long',
    year: 'numeric'
  })
})

// --- CYCLE DE VIE (LIFECYCLE) ---

/**
 * Initialisation du composant : chargement des événements et des dictionnaires.
 */
onMounted(async () => {
  await Promise.all([
    fetchEvents(),
    loadData()
  ])
})
</script>
<style scoped>
.pop-enter-active,
.pop-leave-active {
  transition: all 0.4s cubic-bezier(0.175, 0.885, 0.32, 1.275);
}

.pop-enter-from,
.pop-leave-to {
  opacity: 0;
  transform: scale(0.9) translateY(20px);
}

/* Scrollbar stylisée pour le formulaire */
.custom-scrollbar::-webkit-scrollbar {
  width: 6px;
}

.custom-scrollbar::-webkit-scrollbar-track {
  background: transparent;
}

.custom-scrollbar::-webkit-scrollbar-thumb {
  background: rgba(0, 0, 0, 0.1);
  border-radius: 10px;
}
</style>