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
            <UFormField label="Nom" name="name">
                <UInput v-model="state.name" required />
            </UFormField>

            <!-- Subject -->
            <UFormField label="Sujet" name="subject">
                <UInput v-model="state.subject" required />
            </UFormField>

            <!-- Name -->
            <UFormField label="Description (optionnel)" name="description">
                <UInput v-model="state.description" />
            </UFormField>
        </UForm>

        <UButton @click="form?.submit()" color="secondary" variant="soft" class="cursor-pointer">
            Submit
        </UButton>
    </div>
</template>
