<template>
    <div class="p-4 relative">
        <ClientOnly>
            <!-- Calendrier -->
            <FullCalendar 
                :key="mappedEvents.length" 
                :options="calendarOptions" 
            />
        </ClientOnly>

        <!-- POPUP FLOTTANT (Glassmorphism & Draggable) -->
        <transition name="pop">
            <div 
                v-if="selectedEvent" 
                ref="popupRef"
                :style="style"
                class="fixed z-50 w-[400px] rounded-2xl bg-white/70 backdrop-blur-xl border border-white/50 shadow-[0_8px_30px_rgb(0,0,0,0.12)] overflow-hidden flex flex-col"
            >
                <!-- Zone de déplacement (En-tête du popup) -->
                <div class="px-6 py-4 border-b border-white/40 cursor-move flex justify-between items-center bg-white/30 hover:bg-white/40 transition-colors">
                    <h2 class="text-xl font-bold text-gray-800 select-none truncate pr-4">
                        {{ selectedEvent.title }}
                    </h2>
                    
                    <!-- Bouton fermer -->
                    <UButton
                        color="gray"
                        variant="ghost"
                        icon="i-heroicons-x-mark-20-solid"
                        class="cursor-pointer pointer-events-auto shrink-0"
                        @click="selectedEvent = null"
                    />
                </div>
                
                <!-- Contenu du popup -->
                <div class="p-6 space-y-5">
                    <div class="grid grid-cols-2 gap-4">
                        <div>
                            <span class="block text-[10px] font-bold uppercase tracking-widest text-gray-400 mb-1">Statut</span>
                            <span class="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-blue-100 text-blue-800">
                                {{ selectedEvent.statut || 'N/A' }}
                            </span>
                        </div>
                    </div>

                    <div>
                        <span class="block text-[10px] font-bold uppercase tracking-widest text-gray-400 mb-1">Description</span>
                        <p class="text-sm text-gray-600 leading-relaxed">{{ selectedEvent.description || 'Aucune description fournie.' }}</p>
                    </div>

                    <div class="pt-4 border-t border-gray-200/50 space-y-3">
                        <div class="flex items-center space-x-3">
                            <UIcon name="i-heroicons-calendar" class="text-gray-400 w-5 h-5" />
                            <div>
                                <span class="block text-[10px] font-bold uppercase text-gray-400">Début</span>
                                <p class="text-sm text-gray-700 font-medium">{{ formatDate(selectedEvent.start) }}</p>
                            </div>
                        </div>

                        <div v-if="selectedEvent.end" class="flex items-center space-x-3">
                            <UIcon name="i-heroicons-clock" class="text-gray-400 w-5 h-5" />
                            <div>
                                <span class="block text-[10px] font-bold uppercase text-gray-400">Fin</span>
                                <p class="text-sm text-gray-700 font-medium">{{ formatDate(selectedEvent.end) }}</p>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </transition>
    </div>
</template>

<script setup>
/**
 * @file Composant de Calendrier Interactif
 * @description Affiche un calendrier via FullCalendar avec un système de popup 
 * flottante et déplaçable (Drag & Drop) pour afficher les détails des événements.
 */

import { computed, onMounted, reactive, ref } from 'vue' 
import FullCalendar from '@fullcalendar/vue3'
import dayGridPlugin from '@fullcalendar/daygrid'
import timeGridPlugin from '@fullcalendar/timegrid'
import interactionPlugin from '@fullcalendar/interaction'
import listPlugin from '@fullcalendar/list'
import multiMonthPlugin from '@fullcalendar/multimonth'
import { useEvents } from '../composables/useEvents'
import { useDraggable } from '@vueuse/core'

/**
 * Extraction de l'état et des méthodes depuis le composable `useEvents`.
 * @type {Object}
 * @property {import('vue').Ref<Array>} events - Liste brute des événements récupérés.
 * @property {Function} fetchEvents - Méthode pour charger les événements depuis l'API.
 */
const { events, fetchEvents } = useEvents()

/**
 * L'événement actuellement sélectionné pour affichage dans la popup.
 * @type {import('vue').Ref<Object|null>}
 */
const selectedEvent = ref(null)

/**
 * Référence au conteneur DOM de la popup pour la fonctionnalité de glisser-déposer.
 * @type {import('vue').Ref<HTMLElement|null>}
 */
const popupRef = ref(null)

/**
 * Configuration du glisser-déposer de la popup via VueUse.
 * @type {Object}
 * @property {import('vue').Ref<number>} x - Coordonnée X de la popup.
 * @property {import('vue').Ref<number>} y - Coordonnée Y de la popup.
 * @property {import('vue').ComputedRef<string>} style - Objet de style CSS généré automatiquement (left, top).
 */
const { x, y, style } = useDraggable(popupRef, {
  initialValue: { x: 0, y: 0 },
  preventWindowScroll: true
})

/**
 * Formate une date en chaîne de caractères lisible en français.
 * @param {string|Date} date - La date à formater.
 * @returns {string} La date formatée (ex: "15 janvier 2024 à 10:00") ou une chaîne vide si aucune date n'est fournie.
 */
const formatDate = (date) => {
    if (!date) return ''
    return new Date(date).toLocaleString('fr-FR', {
        dateStyle: 'long',
        timeStyle: 'short'
    })
}

/**
 * Propriété calculée transformant les événements bruts au format requis par FullCalendar.
 * Assure notamment le bon formatage des dates et l'injection des données étendues (extendedProps).
 * @type {import('vue').ComputedRef<Array<Object>>}
 */
const mappedEvents = computed(() => {
    return events.value.map(evt => {
        /**
         * Formate la chaîne de date pour être compatible avec la norme ISO 8601 attendue par FullCalendar.
         * @param {string} dateStr - Chaîne de date brute (ex: "2024-01-01 10:00:00.000").
         * @returns {string|null} Chaîne formatée (ex: "2024-01-01T10:00:00").
         */
        const formatPath = (dateStr) => {
            if (!dateStr) return null;
            return dateStr.replace(' ', 'T').split('.')[0];
        };

        return {
            id: String(evt.id_evt),
            title: evt.title,
            start: formatPath(evt.start), 
            end: formatPath(evt.end),
            color: evt.color || '#3b82f6',
            allDay: evt.allDay || false, 
            extendedProps: {
                description: evt.description,
                statut: evt.statut
            }
        };
    });
});

/**
 * Configuration réactive de l'instance FullCalendar.
 * Contient les plugins chargés, les vues, les données et les écouteurs d'événements.
 * @type {Object}
 */
const calendarOptions = reactive({
    plugins: [dayGridPlugin, timeGridPlugin, interactionPlugin, listPlugin, multiMonthPlugin],
    events: mappedEvents, 
    initialView: 'dayGridMonth',
    headerToolbar: {
        left: 'prev,next today',
        center: 'title',
        right: 'dayGridMonth,timeGridWeek,listWeek'
    },
    
    /**
     * Déclencheur lors du clic sur un événement dans le calendrier.
     * Positionne dynamiquement la popup à côté du curseur de la souris et hydrate ses données.
     * @param {Object} info - Objet natif FullCalendar contenant les informations de l'événement cliqué.
     */
    eventClick: (info) => {
        x.value = info.jsEvent.clientX + 20
        y.value = info.jsEvent.clientY + 20

        selectedEvent.value = {
            title: info.event.title,
            start: info.event.start,
            end: info.event.end,
            ...info.event.extendedProps
        }
    }
})

/**
 * Hook du cycle de vie déclenché au montage du composant.
 * Appelle l'API pour récupérer la liste initiale des événements.
 */
onMounted(async () => {
    try { 
        await fetchEvents() 
    } catch (e) { 
        console.error("Erreur lors de la récupération des événements du calendrier :", e) 
    }
})
</script>

<style scoped>
/* Styles de FullCalendar et animations du composant omis pour des raisons de concision mais identiques à l'original */
.fc {
    max-width: 1100px;
    margin: 0 auto;
    background: white;
    padding: 20px;
    border-radius: 8px;
    color: black;
}

/* Animation d'apparition chic façon "Pop" */
.pop-enter-active, .pop-leave-active {
    transition: all 0.3s cubic-bezier(0.175, 0.885, 0.32, 1.275);
}
.pop-enter-from, .pop-leave-to {
    opacity: 0;
    transform: scale(0.9) translateY(10px);
}

:deep(.fc-event) {
    border: none !important;
    cursor: pointer;
    transition: filter 0.2s;
}

:deep(.fc-event:hover) {
    filter: brightness(90%);
}
</style>