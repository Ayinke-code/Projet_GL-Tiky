<template>
    <div>
        <ClientOnly>
            <!--
                Le composant FullCalendar est enveloppé dans ClientOnly car il dépend du DOM (côté navigateur).
                La prop :key force le rafraîchissement du calendrier quand le nombre d'événements change.
            -->
            <FullCalendar
            :key="mappedEvents.length"
            :options="calendarOptions"
             />
        </ClientOnly>
    </div>
</template>

<script setup>
/**
 * @file Component de gestion de calendrier utilisant FullCalendar v6.
 * @description Ce composant récupère des événements via un composable, les formate pour FullCalendar
 * et gère l'affichage interactif (vues mois, semaine, jour, liste).
 */

import { computed, onMounted, reactive } from 'vue'
import FullCalendar from '@fullcalendar/vue3'
import dayGridPlugin from '@fullcalendar/daygrid'
import timeGridPlugin from '@fullcalendar/timegrid'
import interactionPlugin from '@fullcalendar/interaction'
import listPlugin from '@fullcalendar/list'
import multiMonthPlugin from '@fullcalendar/multimonth'
import { useEvents } from '../composables/useEvents'

/**
 * @typedef {Object} RawEvent - Structure de l'événement provenant de l'API
 * @property {number|string} id_evt - Identifiant unique
 * @property {string} title - Titre de l'événement
 * @property {string} start - Date de début (format ISO ou datetime)
 * @property {string} end - Date de fin
 * @property {string} [color] - Code couleur hexadécimal
 * @property {boolean} [allDay] - Flag journée entière
 * @property {string} [description] - Détails supplémentaires
 * @property {string} [statut] - État de l'événement
 */

const {
    events,
    fetchEvents,
} = useEvents()

/**
 * @computed mappedEvents
 * @description Transforme les données brutes du composable en objets compatibles avec l'API FullCalendar.
 * Effectue un nettoyage des formats de date (remplace l'espace par 'T' pour la conformité ISO).
 * @returns {Array<Object>} Liste des événements formatés pour l'affichage
 */
const mappedEvents = computed(() => {
    return events.value.map(evt => {
        /**
         * Formate la chaîne de date pour la rendre compatible avec FullCalendar/ISO8601.
         * @param {string} dateStr - La date brute
         * @returns {string|null} La date formatée (ex: 2023-10-01T10:00:00)
         */
        const formatPath = (dateStr) => {
            if (!dateStr) return null;
            return dateStr.replace(' ', 'T').split('.')[0];
        };

        return {
            id: String(evt.id),
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
 * @constant calendarOptions
 * @type {Object}
 * @description Configuration principale de FullCalendar.
 * Définit les plugins, la barre d'outils, le comportement d'édition et les callbacks d'interaction.
 */
const calendarOptions = reactive({
    plugins: [
        dayGridPlugin,
        timeGridPlugin,
        interactionPlugin,
        listPlugin,
        multiMonthPlugin
    ],
    events: mappedEvents,
    initialView: 'dayGridMonth',
    editable: true,
    selectable: true,
    eventDisplay: 'block',
    headerToolbar: {
        left: 'prev,next today',
        center: 'title',
        right: 'dayGridMonth,timeGridWeek,timeGridDay,listWeek'
    },
    /**
     * Callback déclenché lors d'un clic sur une date vide.
     * @param {Object} info - Informations sur la date cliquée
     */
    dateClick: (info) => {
        alert(`Date sélectionnée : ${info.dateStr}`)
    }
})

/**
 * @lifecycle onMounted
 * @description Charge les événements au montage du composant et effectue un diagnostic console
 * pour vérifier l'intégrité des données transformées.
 */
onMounted(async () => {
    try {
        await fetchEvents()

        console.log("--- DIAGNOSTIC DES TYPES D'ÉVÉNEMENTS ---")

        mappedEvents.value.forEach((evt, index) => {
            console.log(`Événement n°${index} (${evt.title}) :`)

            Object.keys(evt).forEach(key => {
                const value = evt[key];
                const type = typeof value;

                let detail = type;
                if (type === 'object' && value instanceof Date) detail = 'Date Object';
                if (value === null) detail = 'null';

                console.log(`   - ${key}: ${detail} (valeur: ${value})`);
            });

            console.log(`   - extendedProps statut: ${typeof evt.extendedProps.statut}`);
        })

    } catch (e) {
        console.error('Erreur lors du chargement des événements :', e)
    }
})
</script>

<style scoped>
/**
 * Styles personnalisés pour l'intégration visuelle de FullCalendar.
 * Utilise :deep() pour cibler les classes générées dynamiquement par la librairie.
 */

.fc {
    max-width: 1100px;
    margin: 20px auto;
    background: white;
    padding: 20px;
    border-radius: 8px;
    color: black;
}

/* Optimisation de l'apparence des blocs événements */
:deep(.fc-event) {
    border: none !important;
    margin: 2px !important;
    cursor: pointer;
}

:deep(.fc-event-title) {
    color: white !important;
    font-weight: 600 !important;
    padding: 2px 4px !important;
    font-size: 0.85em;
}

:deep(.fc-event-time) {
    color: rgba(255, 255, 255, 0.9) !important;
    padding: 2px 4px !important;
    font-weight: 400;
}

/* Visibilité des numéros de jours */
:deep(.fc-daygrid-day-number) {
    color: #374151 !important;
    text-decoration: none !important;
    padding: 4px !important;
}
</style>
