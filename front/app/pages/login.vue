<template>
  <div class="min-h-screen flex items-center justify-center bg-default">

    <div class="w-full max-w-md p-6 space-y-4 rounded-xl bg-elevated shadow-lg border border-default">
      <NuxtLink to="/" class="inline-flex items-center justify-center w-8 h-8 rounded-full bg-indigo-100 text-indigo-600 hover:bg-indigo-200 transition-all mb-2">
        <svg xmlns="http://www.w3.org/2000/svg" class="w-4 h-4" viewBox="0 0 20 20" fill="currentColor">
          <path fill-rule="evenodd" d="M9.707 16.707a1 1 0 01-1.414 0l-6-6a1 1 0 010-1.414l6-6a1 1 0 011.414 1.414L5.414 9H17a1 1 0 110 2H5.414l4.293 4.293a1 1 0 010 1.414z" clip-rule="evenodd" />
        </svg>
      </NuxtLink>
      <LoginForms @submit="handleLogin" :error="error"/>
    </div>

  </div>
</template>

<script setup>
import LoginForms from '~/components/forms/LoginForms.vue'
import { useAuth } from '~/composables/useAuth'

const { login } = useAuth()

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
  errorTimer = setTimeout(() => { error.value = null }, 4000)
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
  const dataError = entriesFormat(values?.email, values?.password);
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
    showError('Email ou mot de passe incorrect.')
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
  if (!email || !password) return 'Veuillez remplir tous les champs.'

  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
  if (!emailRegex.test(email)) return 'Adresse email invalide.'

  if (password.length < 8) return 'Le mot de passe doit contenir au moins 8 caractères.'

  return null // pas d'erreur
}

/**
 * Nettoyage du timer lors de la destruction du composant.
 */
onUnmounted(() => {
  if (errorTimer) clearTimeout(errorTimer)
})

</script>