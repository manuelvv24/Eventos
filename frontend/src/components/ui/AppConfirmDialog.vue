<template>
  <AppModal
    :model-value="state.open"
    :title="state.title"
    size="sm"
    @update:model-value="alCerrarSinResponder"
  >
    <div class="flex items-start gap-4">
      <div
        class="w-11 h-11 rounded-full flex items-center justify-center shrink-0"
        :class="state.danger ? 'bg-error-container text-on-error-container' : 'bg-primary-container text-on-primary-container'"
      >
        <span class="material-symbols-outlined text-[24px]" :style="state.danger ? 'font-variation-settings:\'FILL\' 1' : ''">
          {{ state.danger ? 'delete_forever' : 'help' }}
        </span>
      </div>
      <p class="text-body-md text-on-surface-variant pt-1.5">{{ state.message }}</p>
    </div>

    <template #footer>
      <AppButton variant="outlined" @click="responder(false)">{{ state.cancelText }}</AppButton>
      <AppButton :variant="state.danger ? 'danger' : 'primary'" @click="responder(true)">
        {{ state.confirmText }}
      </AppButton>
    </template>
  </AppModal>
</template>

<script setup>
import AppModal from './AppModal.vue'
import AppButton from './AppButton.vue'
import { useConfirm } from '../../composables/useConfirm.js'

const { state, responder, alCerrarSinResponder } = useConfirm()
</script>
