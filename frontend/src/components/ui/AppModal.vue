<template>
  <Teleport to="body">
    <Transition name="modal">
      <div
        v-if="modelValue"
        class="fixed inset-0 z-50 flex items-center justify-center p-4"
        role="dialog"
        :aria-modal="true"
        :aria-labelledby="titleId"
      >
        <!-- Backdrop -->
        <div
          class="absolute inset-0 bg-black/50 backdrop-blur-sm"
          @click="onBackdropClick"
        />

        <!-- Panel -->
        <div
          class="relative w-full bg-surface rounded-2xl shadow-elevation-3 flex flex-col max-h-[90vh] animate-slide-up"
          :class="widthClass"
        >
          <!-- Header -->
          <div class="flex items-center justify-between px-6 py-4 border-b border-outline-variant/30 shrink-0">
            <div>
              <h2 :id="titleId" class="text-title-lg text-on-surface font-semibold">{{ title }}</h2>
              <p v-if="subtitle" class="text-body-sm text-on-surface-variant mt-0.5">{{ subtitle }}</p>
            </div>
            <button
              v-if="closable"
              class="w-8 h-8 flex items-center justify-center rounded-full text-on-surface-variant hover:bg-surface-container transition-colors"
              aria-label="Cerrar"
              @click="$emit('update:modelValue', false)"
            >
              <span class="material-symbols-outlined text-[20px]">close</span>
            </button>
          </div>

          <!-- Body -->
          <div class="overflow-y-auto flex-1 custom-scrollbar px-6 py-5">
            <slot />
          </div>

          <!-- Footer -->
          <div v-if="$slots.footer" class="px-6 py-4 border-t border-outline-variant/30 bg-surface-container-low/30 shrink-0 flex justify-end gap-3">
            <slot name="footer" />
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  modelValue:   { type: Boolean, default: false },
  title:        { type: String, required: true },
  subtitle:     { type: String, default: null },
  size:         { type: String, default: 'md' }, // sm | md | lg | xl
  closable:     { type: Boolean, default: true },
  closeOnBackdrop: { type: Boolean, default: true },
})
const emit = defineEmits(['update:modelValue'])

const titleId  = `modal-title-${Math.random().toString(36).slice(2)}`

const widthClass = computed(() => ({
  sm: 'max-w-sm',
  md: 'max-w-lg',
  lg: 'max-w-2xl',
  xl: 'max-w-4xl',
}[props.size] ?? 'max-w-lg'))

function onBackdropClick() {
  if (props.closeOnBackdrop) emit('update:modelValue', false)
}
</script>

<style scoped>
.modal-enter-active, .modal-leave-active { transition: opacity 0.2s ease; }
.modal-enter-active .relative, .modal-leave-active .relative { transition: transform 0.25s ease, opacity 0.2s ease; }
.modal-enter-from, .modal-leave-to { opacity: 0; }
.modal-enter-from .relative, .modal-leave-to .relative { transform: translateY(16px); opacity: 0; }
</style>
