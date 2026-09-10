<template>
  <Transition name="app-banner-fade">
    <div
      v-if="modelValue"
      role="alert"
      class="p-4 border-l-4 rounded-xl flex items-center justify-between gap-3 shadow-sm"
      :class="styles.container"
    >
      <div class="flex items-center gap-3">
        <span class="material-symbols-outlined shrink-0" :class="styles.icon">{{ styles.iconName }}</span>
        <p class="text-body-sm font-medium" :class="styles.text">
          <slot>{{ modelValue }}</slot>
        </p>
      </div>
      <button
        type="button"
        class="hover:opacity-75"
        :class="styles.icon"
        aria-label="Cerrar aviso"
        @click="$emit('update:modelValue', '')"
      >
        <span class="material-symbols-outlined text-[18px]">close</span>
      </button>
    </div>
  </Transition>
</template>

<!--
  Banner de notificación descartable (éxito / error / info).
  Reemplaza el markup copiado en cada vista.

  Uso: <AppBanner v-model="mensaje" type="success" />
-->
<script setup>
import { computed } from 'vue'

const props = defineProps({
  /** Mensaje a mostrar; cadena vacía oculta el banner (v-model) */
  modelValue: { type: String, default: '' },
  /** success | error | info | warning */
  type: { type: String, default: 'info' },
})

defineEmits(['update:modelValue'])

const VARIANTS = {
  success: {
    container: 'bg-success-container border-success',
    icon: 'text-success',
    text: 'text-on-surface',
    iconName: 'check_circle',
  },
  error: {
    container: 'bg-error-container border-error',
    icon: 'text-error',
    text: 'text-error',
    iconName: 'error',
  },
  warning: {
    container: 'bg-warning-container border-warning',
    icon: 'text-warning',
    text: 'text-on-surface',
    iconName: 'warning',
  },
  info: {
    container: 'bg-primary-container border-primary',
    icon: 'text-primary',
    text: 'text-on-primary-container',
    iconName: 'info',
  },
}

const styles = computed(() => VARIANTS[props.type] ?? VARIANTS.info)
</script>

<style scoped>
.app-banner-fade-enter-active,
.app-banner-fade-leave-active {
  transition: opacity 0.25s;
}
.app-banner-fade-enter-from,
.app-banner-fade-leave-to {
  opacity: 0;
}
</style>
