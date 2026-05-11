<template>
  <div class="min-h-screen bg-gradient-to-br from-indigo-600 to-slate-900 flex items-center justify-center p-4">
    <div class="w-full max-w-md bg-white rounded-xl shadow-2xl p-8">
      <!-- Header -->
      <div class="text-center mb-8">
        <div class="text-4xl font-bold text-indigo-600 mb-2">
          <span>tk.</span> Tiky
        </div>
        <p class="text-gray-600">Connectez-vous à votre compte</p>
      </div>

      <!-- Form -->
      <form @submit.prevent="handleLogin" class="space-y-4">
        <!-- Email/Username -->
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1">Email ou nom d'utilisateur</label>
          <input
            v-model="form.email"
            type="text"
            placeholder="exemple@email.com"
            class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-transparent outline-none transition"
            required
          />
        </div>

        <!-- Password -->
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1">Mot de passe</label>
          <input
            v-model="form.password"
            type="password"
            placeholder="••••••••"
            class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-transparent outline-none transition"
            required
          />
        </div>

        <!-- Error Message -->
        <div v-if="error" class="p-3 bg-red-50 border border-red-200 rounded-lg text-sm text-red-700">
          {{ error }}
        </div>

        <!-- Submit Button -->
        <button
          type="submit"
          :disabled="loading"
          class="w-full px-4 py-2.5 bg-indigo-600 hover:bg-indigo-700 disabled:bg-gray-400 text-white rounded-lg font-medium transition duration-200"
        >
          {{ loading ? 'Connexion en cours...' : 'Se connecter' }}
        </button>
      </form>

      <!-- Test Users -->
      <div class="mt-8 pt-8 border-t border-gray-200">
        <p class="text-xs text-gray-500 text-center mb-3">Admin de test:</p>
        <div class="space-y-2 text-xs text-gray-600 bg-gray-50 p-3 rounded-lg">
          <p><strong>Email:</strong> admin@admin.fr</p>
          <p><strong>Mot de passe:</strong> password</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import type { User } from '~/types'

definePageMeta({
  layout: 'blank'
})

const router = useRouter()
const { syncSession } = useAuth()

type LoginResponse = {
  id: string | number
  name: string
  email: string
  role: User['role']
  token: string
}

const form = ref({
  email: 'admin@admin.fr',
  password: 'password'
})

const loading = ref(false)
const error = ref<string | null>(null)

const handleLogin = async () => {
  loading.value = true
  error.value = null

  try {
    const response = await $fetch<LoginResponse>('http://localhost:8080/api/auth/login', {
      method: 'POST',
      body: {
        email: form.value.email,
        password: form.value.password
      }
    })

    if (response.token) {
      // Store token in cookie using nuxt composable
      const token = useCookie('auth_token')
      token.value = response.token
      syncSession({
        id: response.id,
        name: response.name,
        email: response.email,
        role: response.role
      })
      console.log('Connexion réussie! Token:', response.token.substring(0, 20) + '...')

      // Redirige vers l'espace le plus cohérent avec le rôle métier
      await router.push((response.role === 'STUDENT' || response.role === 'USER') ? '/issues' : '/categorie')
    }
  } catch (err: unknown) {
    console.error('Login error:', err)
    error.value = err instanceof Error ? err.message : 'Email ou mot de passe incorrect'
  } finally {
    loading.value = false
  }
}
</script>
