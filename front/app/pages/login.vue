<template>
  <div class="min-h-screen flex items-center justify-center bg-default">

    <!-- ALERT FLOTTANT -->
    <Transition name="fade">
      <div
        v-if="error"
        class="fixed top-4 left-1/2 -translate-x-1/2 z-50 w-full max-w-md px-4"
      >
        <Alert
          color="error"
          title="Erreur"
          :description="error"
          icon="i-lucide-alert-circle"
        />
      </div>
    </Transition>

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
const error = ref(false)

const handleLogin = async (values) => {
  error.value = false

  try {
    await login(values.data.email, values.data.password)
    await navigateTo('/')
  } catch (e) {
    error.value = true
    console.error('login failed', e)
  }
}
</script>