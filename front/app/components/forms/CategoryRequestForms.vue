<script setup>
const emit = defineEmits(['submit', 'cancel'])

const state = reactive({
    name: undefined,
    subject: undefined,
    description: undefined // optionnel
})

const form = useTemplateRef('form')

function onSubmit(event) {
    emit('submit', event.data)
}
</script>

<template>
    <div class="space-y-4">
        <UForm ref="form" :state="state" class="space-y-4" @submit="onSubmit">
            <!-- Name -->
            <UFormField id="categorie-name-student" label="Nom" name="name">
                <UInput v-model="state.name" required />
            </UFormField>

            <!-- Subject -->
            <UFormField id="categorie-subject-student" label="Sujet" name="subject">
                <UInput v-model="state.subject" required />
            </UFormField>

            <!-- Name -->
            <UFormField id="categorie-description-student" label="Description (optionnel)" name="description">
                <UInput v-model="state.description" />
            </UFormField>
        </UForm>

        <div class="flex justify-center">
            <UButton id="validate-categorie" @click="form?.submit()" color="secondary" variant="soft" class="cursor-pointer">
                Envoyer
            </UButton>
        </div>
    </div>
</template>
