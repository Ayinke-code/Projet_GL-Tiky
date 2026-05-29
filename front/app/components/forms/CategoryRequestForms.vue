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
    <UForm ref="form" :state="state" class="space-y-4" @submit="onSubmit">
      <UFormField label="Nom" name="name">
        <UInput id="categorie-name-student" v-model="state.name" required />
      </UFormField>

      <UFormField label="Sujet" name="subject">
        <UInput id="categorie-subject-student" v-model="state.subject" required />
      </UFormField>

      <UFormField label="Description (optionnel)" name="description">
        <UInput id="categorie-description-student" v-model="state.description" />
      </UFormField>
    </UForm>

    <div class="flex justify-center">
      <UButton id="validate-categorie" @click="form?.submit()" color="secondary" variant="soft" class="cursor-pointer">
        Envoyer
      </UButton>
    </div>
  </div>
</template>
