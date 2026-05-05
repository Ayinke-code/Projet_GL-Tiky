<template>
  <UAuthForm
    title="Connexion"
    description="Saisissez vos identifiants"
    icon="i-lucide-user"
    :fields="fields"
    @submit="onSubmit"
    class="max-w-md"
  />
</template>

<script setup>
import { ref } from 'vue'

const emit = defineEmits(['submit'])

const fields = ref([
  { name: 'email', type: 'text', label: 'Email' },
  { name: 'password', type: 'password', label: 'Mot de passe' }
])

const onSubmit = (values) => {
  emit('submit', values)
}
</script>