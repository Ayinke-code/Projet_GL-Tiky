<template>
  <div class="min-h-screen flex items-center justify-center bg-default">
    <div class="w-full max-w-md">
      <div class="p-6 space-y-4 rounded-xl bg-elevated shadow-lg border border-default">
        <div class="flex items-center justify-between">
          <NuxtLink
            to="/"
            class="inline-flex items-center justify-center w-8 h-8 rounded-lg text-indigo-500 hover:text-indigo-700 hover:bg-indigo-50 transition-all"
          >
            <svg
              xmlns="http://www.w3.org/2000/svg"
              class="w-4 h-4"
              fill="none"
              viewBox="0 0 24 24"
              stroke="currentColor"
              stroke-width="2"
            >
              <path
                stroke-linecap="round"
                stroke-linejoin="round"
                d="M10 19l-7-7m0 0l7-7m-7 7h18"
              />
            </svg>
          </NuxtLink>

          <!-- Language switcher -->
          <div class="flex items-center gap-2">
            <button
              :class="{ 'font-bold': locale === 'fr' }"
              class="px-3 py-1 rounded text-sm border"
              @click="setLocale('fr')"
            >
              fr
            </button>
            <button
              :class="{ 'font-bold': locale === 'en' }"
              class="px-3 py-1 rounded text-sm border"
              @click="setLocale('en')"
            >
              en
            </button>
          </div>
        </div>

        <LoginForms
          :error="error"
          @submit="handleLogin"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import LoginForms from '~/components/forms/LoginForms.vue'
import { useAuth } from '~/composables/useAuth'
import { useLoginI18n } from '~/composables/useLoginI18n'

const { login } = useAuth()
const { t, locale, setLocale } = useLoginI18n()

// setLocale provided by composable

/**
 * Message d’erreur affiché à l’utilisateur (UI state).
 */
const error = ref(null)

/**
 * Timer utilisé pour supprimer automatiquement les messages d’erreur.
 */
let errorTimer = null

/**
 * Affiche un message d’erreur temporaire.
 * Le message est automatiquement supprimé après 4 secondes.
 *
 * @param message message à afficher
 */
const showError = (message) => {
  if (errorTimer) clearTimeout(errorTimer)
  error.value = message
  errorTimer = setTimeout(() => {
    error.value = null
  }, 4000)
}

/**
 * Gère la soumission du formulaire de login.
 *
 * Étapes :
 * - validation des champs (email / password)
 * - appel API via composable useAuth
 * - redirection si succès
 * - affichage d’erreur si échec
 *
 * @param values données du formulaire de connexion
 */
const handleLogin = async (values) => {
  error.value = null

  // Gestion des erreurs des champs emails et password
  const dataError = entriesFormat(values?.email, values?.password)
  if (dataError) {
    // error.value = dataError
    showError(dataError)
    return
  }

  // Envoie de la requête de login
  try {
    await login(values?.email, values?.password)
    await navigateTo('/issues')
  } catch (e) {
    // error.value = "Email ou mot de passe incorrect"
    showError(t('login_page.error_incorrect'))
    console.error('login failed', e)
  }
}

/**
 * Valide le format des champs de connexion.
 *
 * @param email email utilisateur
 * @param password mot de passe utilisateur
 * @return message d’erreur si invalide, sinon null
 */
function entriesFormat(email, password) {
  if (!email || !password) return t('login_page.error_fill_fields')

  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
  if (!emailRegex.test(email)) return t('login_page.error_invalid_email')

  if (password.length < 8) return t('login_page.error_password_length')

  return null // pas d'erreur
}

/**
 * Nettoyage du timer lors de la destruction du composant.
 */
onUnmounted(() => {
  if (errorTimer) clearTimeout(errorTimer)
})
</script>
