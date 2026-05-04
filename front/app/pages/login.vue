<template>
  <div class="min-h-screen flex items-center justify-center bg-default">

    <div v-if="error" class="fixed top-4 left-1/2 -translate-x-1/2 z-50 w-full max-w-md px-4">
      <Alert color="error" title="Erreur" :description="error" icon="i-lucide-alert-circle" />
    </div>

    <div class="w-full max-w-md p-6 space-y-4 rounded-xl bg-elevated shadow-lg border border-default">
      <LoginForms @submit="handleLogin" />
    </div>

  </div>
</template>

<script setup>
import LoginForms from '~/components/forms/LoginForms.vue'
import Alert from '~/components/ui/Alert.vue';
import { useAuth } from '~/composables/useAuth'

const { login } = useAuth()
const error = ref(null)
let errorTimer = null

// Affiche un message d’erreur temporaire : annule le timer précédent après 4s
const showError = (message) => {
  if (errorTimer) clearTimeout(errorTimer)
  error.value = message
  errorTimer = setTimeout(() => { error.value = null }, 4000)
}

const handleLogin = async (values) => {
  error.value = false

  // Gestion des erreurs des champs emails et password
  const dataError = entriesFormat(values.data?.email, values.data?.password);
  if (dataError) {
    // error.value = dataError
    showError(dataError)
    return
  }

  // Envoie de la requête de login
  try {
    await login(values.data.email, values.data.password)
    await navigateTo('/')
  } catch (e) {
    // error.value = "Email ou mot de passe incorrect"
    showError('Email ou mot de passe incorrect.')
    console.error('login failed', e)
  }
}

// Fonction qui vérifie le format des champs password et email
function entriesFormat(email, password) {
  if (!email || !password) return 'Veuillez remplir tous les champs.'

  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
  if (!emailRegex.test(email)) return 'Adresse email invalide.'

  if (password.length < 8) return 'Le mot de passe doit contenir au moins 8 caractères.'

  return null // pas d'erreur
}

// Timer de l'erreur
onUnmounted(() => {
  if (errorTimer) clearTimeout(errorTimer)
})

</script>