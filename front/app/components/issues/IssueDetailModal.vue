<template>
  <UModal v-model:open="isOpen" :title="$t('issue_modal.title')">
    <template #body>
      <div v-if="loading" class="flex justify-center py-8">
        <UIcon name="i-heroicons-arrow-path" class="animate-spin text-gray-400 text-2xl"/>
      </div>

      <div v-else-if="issueDetail" class="space-y-4">
        <div class="space-y-1">
          <p class="text-xs font-semibold text-gray-400 uppercase tracking-wide">
            {{ $t('issue_modal.field_title') }}
          </p>
          <p class="text-gray-900 font-medium">
            {{ issueDetail.title }}
          </p>
        </div>
        <div class="border-t border-gray-100" />
        <div class="space-y-1">
          <p class="text-xs font-semibold text-gray-400 uppercase tracking-wide">
            {{ $t('issue_modal.created_by') }}
          </p>
          <p class="text-gray-700 text-sm">
            {{ issueDetail.createdName }}
          </p>
        </div>
        <div class="border-t border-gray-100" />
        <div class="space-y-1">
          <p class="text-xs font-semibold text-gray-400 uppercase tracking-wide">
            {{ $t('issue_modal.category') }}
          </p>
          <UBadge color="primary" variant="soft">
            {{ issueDetail.categoryName }}
          </UBadge>
        </div>
        <div class="border-t border-gray-100" />
        <div class="space-y-1">
          <p class="text-xs font-semibold text-gray-400 uppercase tracking-wide">
            {{ $t('issue_modal.description') }}
          </p>
          <p class="text-gray-700 text-sm leading-relaxed whitespace-pre-wrap">
            {{ issueDetail.description }}
          </p>
        </div>
      </div>

      <div v-else class="text-center py-8 text-gray-400 italic">
        {{ $t('issue_modal.error_loading') }}
      </div>
    </template>

    <template #footer>
      <div class="flex justify-end">
        <UButton color="neutral" variant="ghost" :label="$t('issue_modal.close')" @click="isOpen = false"/>
      </div>
    </template>
  </UModal>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  open: { type: Boolean, required: true },
  issueDetail: { type: Object, default: null },
  loading: { type: Boolean, default: false }
})

const emit = defineEmits(['update:open'])

const isOpen = computed({
  get: () => props.open,
  set: value => emit('update:open', value)
})
</script>
