<template>
  <div class="min-h-screen flex items-center justify-center bg-default">

    <div class="w-full max-w-md p-6 rounded-lg bg-elevated shadow-lg border border-default">
      <LoginForms @submit="handleLogin" />
    </div>

  </div>
</template>

<script setup>
import LoginForms from '~/components/forms/LoginForms.vue'
import { useAuth } from '~/composables/useAuth'

const { login } = useAuth()

const handleLogin = async (values) => {
  const email = values.data.email;
  const password = values.data.password;
  try {
    await login(email, password)
    await navigateTo('/')
  } catch (e) {
    console.error('login failed', e)
  }
}
</script>