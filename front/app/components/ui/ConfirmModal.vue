<script setup lang="js">
import { watch } from 'vue'

const open = defineModel('open')

const emit = defineEmits([
  'confirm',
  'cancel'
])

defineProps({
  title: String,
  description: String
})

watch(open, (value, oldValue) => {

  // modal fermée sans confirmation
  if (oldValue === true && value === false) {
    emit('cancel')
  }
})
</script>

<template>
  <UModal v-model:open="open" :title="title" :close="{
    color: 'neutral',
    variant: 'outline',
    class: 'rounded-full cursor-pointer'
  }">

    <template #body>
      {{ description }}
    </template>

    <template #footer>
      <div class="flex justify-end gap-2">

        <UButton color="neutral" variant="ghost" @click="open = false" class="cursor-pointer">
          Annuler
        </UButton>

        <UButton color="error" @click="emit('confirm'); open = false" class="cursor-pointer">
          Confirmer
        </UButton>

      </div>
    </template>

  </UModal>
</template>