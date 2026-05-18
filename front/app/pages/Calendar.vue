<template>
    <FullCalendar :options="calendarOptions" />
</template>

<script setup>
import FullCalendar from '@fullcalendar/vue3'

import dayGridPlugin from '@fullcalendar/daygrid'
import timeGridPlugin from '@fullcalendar/timegrid'
import interactionPlugin from '@fullcalendar/interaction'
import listPlugin from '@fullcalendar/list'
import multiMonthPlugin from '@fullcalendar/multimonth'

const calendarOptions = {
    plugins: [
        dayGridPlugin,
        timeGridPlugin,
        interactionPlugin,
        listPlugin,
        multiMonthPlugin
    ],

    initialView: 'dayGridMonth',

    editable: true,
    selectable: true,

    headerToolbar: {
        left: 'prev,next today',
        center: 'title',
        right: 'dayGridMonth,timeGridWeek,timeGridDay,listWeek'
    },

    events: [
        {
            title: 'Réunion',
            start: '2026-05-18'
        },
        {
            title: 'Projet Horizon',
            start: '2026-05-20',
            end: '2026-05-22'
        }
    ],

    dateClick: (info) => {
        alert(`Date sélectionnée : ${info.dateStr}`)
    }
}
</script>

<style>
@import '@fullcalendar/daygrid/index.css';
@import '@fullcalendar/timegrid/index.css';
@import '@fullcalendar/list/index.css';
</style>