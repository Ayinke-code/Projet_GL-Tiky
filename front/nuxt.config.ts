// https://nuxt.com/docs/api/configuration/nuxt-config
export default defineNuxtConfig({
  // ssr: false global cassé en Nuxt 4.4.5 (bug No entry found in rollupOptions.input)
  // → SSR désactivé page par page via definePageMeta({ ssr: false })

  modules: [
    '@nuxt/eslint',
    '@nuxt/ui',
    '@pinia/nuxt'
  ],

  // Désactive le préfixage des composants par sous-dossier (Nuxt 4 change le défaut)
  // Sans ça : components/events/EventCard.vue → <EventsEventCard> (casse tout)
  components: {
    dirs: [{ path: '~/components', pathPrefix: false }]
  },

  runtimeConfig: {
    public: {
      apiBase: 'http://localhost:8080/api'
    }
  },

  devtools: {
    enabled: true
  },

  css: ['~/assets/css/main.css'],

  compatibilityDate: '2025-01-15',

  eslint: {
    config: {
      stylistic: {
        commaDangle: 'never',
        braceStyle: '1tbs'
      }
    }
  }
})
