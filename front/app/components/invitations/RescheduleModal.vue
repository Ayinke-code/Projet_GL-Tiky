<template>
  <Modal v-model:open="openModel" :title="isTeacher ? 'Reprogrammer la réunion' : 'Modifier la date de la demande'">
    <div class="p-2 flex flex-col gap-5">
      <p class="text-sm text-gray-600">
        {{ isTeacher ? 'Modifiez la date de début ainsi que la durée estimée de votre événement.' : 'Proposez une nouvelle date et durée pour ce rendez-vous.' }}
      </p>

      <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
        <div class="flex flex-col gap-1.5">
          <label class="text-xs font-bold text-gray-700 uppercase tracking-wider">Date & heure de début</label>
          <input
            type="datetime-local"
            :value="rescheduleForm.startDate"
            :min="minStartDate"
            @input="updateForm('startDate', $event.target.value)"
            :class="['w-full border border-gray-300 rounded-xl p-2.5 text-sm outline-none focus:ring-2 bg-white', isTeacher ? 'focus:ring-indigo-500' : 'focus:ring-blue-500']"
          />
        </div>

        <div class="flex flex-col gap-1.5">
          <label class="text-xs font-bold text-gray-700 uppercase tracking-wider">Durée du rendez-vous</label>
          <select
            :value="rescheduleForm.duration"
            @change="updateForm('duration', parseInt($event.target.value))"
            :class="['w-full border border-gray-300 rounded-xl p-2.5 text-sm outline-none focus:ring-2 bg-white cursor-pointer', isTeacher ? 'focus:ring-indigo-500' : 'focus:ring-blue-500']"
          >
            <option :value="30">30 minutes</option>
            <option :value="45">45 minutes</option>
            <option :value="60">1 heure</option>
            <option :value="90">1 heure 30 mins</option>
            <option :value="120">2 heures</option>
            <option :value="180">3 heures</option>
          </select>
        </div>
      </div>

      <div class="flex flex-col gap-2 bg-gray-50 p-4 rounded-xl border border-gray-200">
        <label class="text-xs font-bold text-gray-700 uppercase tracking-wider mb-1">Gestion des participants</label>

        <label class="flex items-start gap-3 cursor-pointer p-1">
          <input type="radio" :value="true" :checked="rescheduleForm.addAll === true" @change="updateForm('addAll', true)" :class="['mt-1', isTeacher ? 'text-indigo-600 focus:ring-indigo-500' : 'text-blue-600 focus:ring-blue-500']" />
          <div class="text-sm">
            <span class="font-semibold text-gray-800">Réinviter tout le monde</span>
            <p class="text-xs text-gray-500">Renvoie une invitation à tous les participants d'origine (y compris ceux qui avaient décliné).</p>
          </div>
        </label>

        <label class="flex items-start gap-3 cursor-pointer p-1 mt-2">
          <input type="radio" :value="false" :checked="rescheduleForm.addAll === false" @change="updateForm('addAll', false)" :class="['mt-1', isTeacher ? 'text-indigo-600 focus:ring-indigo-500' : 'text-blue-600 focus:ring-blue-500']" />
          <div class="text-sm">
            <span class="font-semibold text-gray-800">Composer sans les absents</span>
            <p class="text-xs text-gray-500">Exclut définitivement les personnes indisponibles. Seuls les membres qui avaient accepté recevront la nouvelle date.</p>
          </div>
        </label>
      </div>

      <div class="flex justify-end gap-3 mt-4 border-t pt-4">
        <Button @click="$emit('update:open', false)" color="secondary" label="Annuler" variant="subtle" rounded />
        <Button @click="$emit('confirm')" color="primary" label="Enregistrer" variant="solid" rounded />
      </div>
    </div>
  </Modal>
</template>

<script setup>
import { computed } from 'vue'
import Button from '~/components/ui/Button.vue'
import Modal from '~/components/ui/Modal.vue'

const props = defineProps({
  open: {
    type: Boolean,
    required: true
  },
  isTeacher: {
    type: Boolean,
    default: false
  },
  rescheduleForm: {
    type: Object,
    required: true
  }
})

const emit = defineEmits(['update:open', 'update:rescheduleForm', 'confirm'])

const openModel = computed({
  get: () => props.open,
  set: (val) => emit('update:open', val)
})

const minStartDate = computed(() => {
  const d = new Date()
  d.setMinutes(d.getMinutes() + 30)
  const pad = (n) => n.toString().padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`
})

const updateForm = (key, value) => {
  emit('update:rescheduleForm', { ...props.rescheduleForm, [key]: value })
}
</script>
