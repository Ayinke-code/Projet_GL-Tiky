<template>
  <!--
    Popup de sélection du type d'événement.
    Au clic sur une card, le modal se ferme et on navigue
    vers la page de formulaire dédiée /events/create/[type].
  -->
  <Modal v-model:open="open" title="Créer un événement">
    <div class="space-y-4">
      <p class="text-sm text-gray-500">Choisissez le type d'événement :</p>

      <div class="grid grid-cols-2 gap-4">

        <!-- Card AUDIT -->
        <button
          @click="goToCreate('AUDIT')"
          class="flex flex-col items-center gap-3 p-6 border-2 rounded-xl transition-all cursor-pointer
                 border-indigo-200 hover:border-indigo-500 hover:bg-indigo-50"
        >
          <i class="fa-solid fa-magnifying-glass text-3xl text-indigo-500"></i>
          <span class="font-bold text-gray-800">Audit</span>
          <span class="text-xs text-gray-400 text-center">Revue de code, CI/CD, qualité…</span>
        </button>

        <!-- Card SOUTENANCE -->
        <button
          @click="goToCreate('DEFENSE')"
          class="flex flex-col items-center gap-3 p-6 border-2 rounded-xl transition-all cursor-pointer
                 border-emerald-200 hover:border-emerald-500 hover:bg-emerald-50"
        >
          <i class="fa-solid fa-graduation-cap text-3xl text-emerald-500"></i>
          <span class="font-bold text-gray-800">Soutenance</span>
          <span class="text-xs text-gray-400 text-center">PFE, Sprint, Stage…</span>
        </button>

        <!-- Card Rendez-vous -->
        <button
        @click="goToCreate('RDV')"
          class="flex flex-col items-center gap-3 p-6 border-2 rounded-xl transition-all cursor-pointer 
       border-amber-200 hover:border-amber-500 hover:bg-amber-50"
        >
          <i class="fa-solid fa-graduation-cap text-3xl text-violet-500"></i>
          <span class="font-bold text-gray-800">Rendez-vous</span>
          <span class="text-xs text-gray-400 text-center"> Rendez-vous lié à une Issue ou autres</span>
        </button>

      </div>
    </div>
  </Modal>
</template>

<script setup>
// Prop liée au v-model:open du parent
const open = defineModel('open')
const router = useRouter()

import Modal from '~/components/ui/Modal.vue'

/**
 * Ferme le popup et navigue vers la page de formulaire du type choisi.
 */
const goToCreate = (type) => {
  open.value = false
  router.push(`/events/create/${type}`)
}
</script>