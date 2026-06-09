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
    @submit.prevent="onSubmit"
    class="space-y-4"
  >
    <!-- Email/Username -->
    <div>
      <label class="block text-sm font-medium text-gray-700 mb-1">
        {{ t('login_page.email_label') }}
      </label>
      <input
        v-model="form.email"
        type="text"
        :placeholder="t('login_page.email_placeholder')"
        id="input-email"
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
        :placeholder="t('login_page.password_label')"
        id="input-password"
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
      :disabled="!form.email || !form.password"
      id="btn-connexion"
      class="w-full px-4 py-2.5 bg-indigo-600 hover:bg-indigo-700 disabled:bg-gray-400 text-white rounded-lg font-medium transition duration-200 cursor-pointer"
    >
      {{ t('login_page.login_button') }}
    </button>
  </form>
</template>

<script setup>
import { ref } from 'vue'
import { useI18n } from 'vue-i18n'

const { error } = defineProps({
  error: {
    type: String,
    default: null
  }
})

const { t } = useI18n()

const emit = defineEmits(['submit'])

const form = ref({
  email: '',
  password: ''
})

const onSubmit = () => {
  emit('submit', { ...form.value })
}
</script>
