<script setup>
const emit = defineEmits(['submit', 'cancel'])

const state = reactive({
  name: undefined,
  subject: undefined,
  description: undefined // facultatif
})

const form = useTemplateRef('form')

function onSubmit() {
  emit('submit', {
    name: state.name,
    subject: state.subject,
    description: state.description
  })
}
</script>

<template>
  <div class="space-y-4">
    <UForm :state="state" class="space-y-4" @submit="onSubmit">
      <UFormField label="Nom" name="name">
        <UInput id="categorie-name-student" v-model="state.name" required />
      </UFormField>

      <UFormField label="Sujet" name="subject">
        <UInput id="categorie-subject-student" v-model="state.subject" required />
      </UFormField>

      <UFormField label="Description (optionnel)" name="description">
        <UInput id="categorie-description-student" v-model="state.description" />
      </UFormField>

      <div class="flex justify-center mt-4">
        <UButton type="submit" id="validate-request" color="secondary" variant="soft" class="cursor-pointer">
          Envoyer
        </UButton>
      </div>
    </UForm>
  </div>
</template>
