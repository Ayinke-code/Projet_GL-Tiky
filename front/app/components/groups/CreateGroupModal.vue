<template>
  <!--
    Popup de sélection du type de groupe.
    Au clic sur une card, le modal se ferme et on navigue
    vers la page de formulaire dédiée /groups/create/[type].
  -->
  <Modal v-model:open="open" title="Créer un groupe">
    <div class="space-y-4">
      <p class="text-sm text-gray-500">Choisissez le type de groupe :</p>

      <div class="grid grid-cols-2 gap-4">

        <!-- Card OPTION -->
        <button
          @click="goToCreate('OPTION')"
          class="flex flex-col items-center gap-3 p-6 border-2 rounded-xl transition-all cursor-pointer
                 border-amber-200 hover:border-amber-500 hover:bg-amber-50"
        >
          <i class="fa-solid fa-code-branch text-3xl text-amber-500"></i>
          <span class="font-bold text-gray-800">Option</span>
          <span class="text-xs text-gray-400 text-center">LD, CSS, SE, DISA, IA…</span>
        </button>

        <!-- Card PROJET -->
        <button
          @click="goToCreate('PROJECT')"
          class="flex flex-col items-center gap-3 p-6 border-2 rounded-xl transition-all cursor-pointer
                 border-emerald-200 hover:border-emerald-500 hover:bg-emerald-50"
        >
          <i class="fa-solid fa-rocket text-3xl text-emerald-500"></i>
          <span class="font-bold text-gray-800">Projet</span>
          <span class="text-xs text-gray-400 text-center">Horizon, Challenger, Sirena…</span>
        </button>

        <!-- Card PROMOTION (pleine largeur) -->
        <button
          @click="goToCreate('promotion')"
          class="col-span-2 flex flex-col items-center gap-3 p-5 border-2 rounded-xl transition-all cursor-pointer
                 border-indigo-200 hover:border-indigo-500 hover:bg-indigo-50"
        >
          <i class="fa-solid fa-users text-3xl text-indigo-500"></i>
          <span class="font-bold text-gray-800">Promotion</span>
          <span class="text-xs text-gray-400 text-center">IR 2026, RT 2025… — import CSV/Excel</span>
        </button>

      </div>
    </div>
  </Modal>
</template>

<script setup>
const open = defineModel('open')
const router = useRouter()

import Modal from '~/components/ui/Modal.vue'

const goToCreate = (type) => {
  open.value = false
  router.push(`/groups/create/${type}`)
}
</script>
