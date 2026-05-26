<template>
  <div class="min-h-screen flex flex-col md:flex-row">

    <!-- Colonne gauche : branding -->
    <div class="relative md:w-1/2 bg-slate-900 flex flex-col justify-between p-10 overflow-hidden">

      <!-- Motif décoratif -->
      <div class="absolute inset-0 pointer-events-none">
        <div class="absolute -top-32 -left-32 w-96 h-96 rounded-full bg-indigo-600/20 blur-3xl"></div>
        <div class="absolute bottom-0 right-0 w-80 h-80 rounded-full bg-emerald-500/10 blur-3xl"></div>
        <div class="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-64 h-64 rounded-full bg-indigo-500/5 blur-2xl"></div>
      </div>

      <!-- Logo -->
      <div class="relative z-10">
        <span class="text-indigo-400 font-extrabold text-3xl tracking-tighter">tk.</span>
        <span class="text-white font-bold text-2xl ml-1">Tiky</span>
      </div>

      <!-- Contenu central -->
      <div class="relative z-10 space-y-8">
        <div>
          <h1 class="text-4xl md:text-5xl font-extrabold text-white leading-tight">
            La plateforme de<br />
            <span class="text-indigo-400">gestion académique</span>
          </h1>
          <p class="mt-4 text-slate-400 text-lg leading-relaxed">
            Suivez vos ticket, organisez vos catégories et planifiez
            audits &amp; soutenances en un seul endroit.
          </p>
        </div>

        <!-- Features -->
        <div class="space-y-4">
          <div v-for="feat in features" :key="feat.label"
               class="flex items-center gap-4 bg-slate-800/60 rounded-xl px-5 py-4 border border-slate-700/50">
            <div class="w-10 h-10 rounded-lg flex items-center justify-center flex-shrink-0"
                 :class="feat.bg">
              <i :class="feat.icon + ' text-lg'" :style="{ color: feat.color }"></i>
            </div>
            <div>
              <p class="text-white font-semibold text-sm">{{ feat.label }}</p>
              <p class="text-slate-400 text-xs mt-0.5">{{ feat.desc }}</p>
            </div>
          </div>
        </div>
      </div>

      <!-- Footer gauche -->
      <p class="relative z-10 text-slate-600 text-xs">
        &copy; {{ new Date().getFullYear() }} Tiky &mdash; Tous droits réservés
      </p>
    </div>

    <!-- Colonne droite : bienvenue + CTA -->
    <div class="md:w-1/2 flex items-center justify-center bg-gray-50 p-10">
      <div class="w-full max-w-md space-y-8">

        <!-- En-tête -->
        <div>
          <h2 class="text-3xl font-extrabold text-gray-900">Bienvenue</h2>
          <p class="mt-2 text-gray-500 text-base">
            Connectez-vous pour accéder à votre espace et retrouver
            toutes vos activités académiques.
          </p>
        </div>

        <!-- CTA -->
        <div class="space-y-3">
          <NuxtLink to="/login"
            class="flex items-center justify-center w-full px-6 py-3.5 rounded-xl bg-indigo-600 hover:bg-indigo-700 text-white font-semibold text-base transition-colors shadow-md">
            <i class="fa-solid fa-arrow-right-to-bracket mr-2"></i>
            Se connecter
          </NuxtLink>
          <p class="text-center text-gray-400 text-xs">
            Accès réservé aux membres de l'établissement
          </p>
        </div>

      </div>
    </div>
  </div>
</template>

<script setup>
definePageMeta({ ssr: false })

const features = [
  {
    label: 'Ticket & suivi',
    desc:  'Déposez et suivez l\'avancement de vos problématiques.',
    icon:  'fa-solid fa-circle-exclamation',
    bg:    'bg-rose-500/10',
    color: '#f43f5e'
  },
  {
    label: 'Catégories',
    desc:  'Organisez vos sujets et abonnez-vous à ceux qui vous intéressent.',
    icon:  'fa-solid fa-folder',
    bg:    'bg-amber-500/10',
    color: '#f59e0b'
  },
  {
    label: 'Audits & Soutenances',
    desc:  'Planifiez et gérez les événements académiques.',
    icon:  'fa-solid fa-calendar-days',
    bg:    'bg-indigo-500/10',
    color: '#6366f1'
  }
]
</script>
