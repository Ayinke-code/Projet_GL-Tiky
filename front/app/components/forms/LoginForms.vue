<template>
  <!-- Header -->
  <div class="text-center mb-8">
    <div class="text-4xl font-bold text-indigo-600 mb-2">
      <span>Tiky</span>
    </div>
    <p class="text-gray-600">
      {{ t('login_page.connect_to_account') }}
    </p>
  </div>

  <!-- Form -->
  <form
    class="space-y-4"
    @submit.prevent="onSubmit"
  >
    <!-- Email/Username -->
    <div>
      <label class="block text-sm font-medium text-gray-700 mb-1">
        {{ t('login_page.email_label') }}
      </label>
      <input
        v-model="form.email"
        type="text"
        id="input-email"
        :placeholder="t('login_page.email_placeholder')"
        class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-transparent outline-none transition"
        required
      >
    </div>

    <!-- Password -->
    <div>
      <label class="block text-sm font-medium text-gray-700 mb-1">
        {{ t('login_page.password_label') }}
      </label>
      <input
        v-model="form.password"
        type="password"
        id="input-password"
        :placeholder="t('login_page.password_label')"
        class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-transparent outline-none transition"
        required
      >
    </div>

    <!-- Error Message -->
    <div
      v-if="error"
      class="p-3 bg-red-50 border border-red-200 rounded-lg text-sm text-red-700"
    >
      {{ error }}
    </div>

    <!-- Submit Button -->
    <button
      type="submit"
      id="btn-connexion"
      :disabled="!form.email || !form.password"
      class="w-full px-4 py-2.5 bg-indigo-600 hover:bg-indigo-700 disabled:bg-gray-400 text-white rounded-lg font-medium transition duration-200 cursor-pointer"
    >
      {{ t('login_page.login_button') }}
    </button>
  </form>
</template>

<script setup>
import { ref } from 'vue'
import { useLoginI18n } from '~/composables/useLoginI18n'

const { error } = defineProps({
  error: {
    type: String,
    default: null
  }
})

const { t } = useLoginI18n()

const emit = defineEmits(['submit'])

const form = ref({
  email: '',
  password: ''
})

const onSubmit = () => {
  emit('submit', { ...form.value })
}
</script>
