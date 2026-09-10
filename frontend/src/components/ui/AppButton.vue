<template>
  <component
    :is="to ? 'router-link' : 'button'"
    :to="to"
    :type="to ? undefined : type"
    :disabled="disabled || loading"
    :class="classes"
    v-bind="$attrs"
  >
    <AppSpinner v-if="loading" size="sm" />
    <span v-if="icon && !loading" class="material-symbols-outlined text-[18px]">{{ icon }}</span>
    <span v-if="$slots.default"><slot /></span>
    <span v-if="iconRight && !loading" class="material-symbols-outlined text-[18px]">{{ iconRight }}</span>
  </component>
</template>

<script setup>
import { computed } from 'vue'
import AppSpinner from './AppSpinner.vue'

const props = defineProps({
  variant: { type: String, default: 'primary' }, // primary | secondary | outlined | ghost | danger | success
  size:    { type: String, default: 'md' },       // sm | md | lg
  icon:    { type: String, default: null },
  iconRight: { type: String, default: null },
  loading: { type: Boolean, default: false },
  disabled:{ type: Boolean, default: false },
  type:    { type: String, default: 'button' },
  to:      { type: [String, Object], default: null },
})

const base = 'inline-flex items-center justify-center gap-2 font-medium rounded-lg transition-all duration-200 active:scale-95 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary disabled:opacity-60 disabled:cursor-not-allowed disabled:active:scale-100 select-none'

const variantMap = {
  primary:   'bg-primary text-on-primary hover:bg-primary/90 shadow-sm',
  secondary: 'bg-secondary text-on-secondary hover:bg-secondary/90 shadow-sm',
  tonal:     'bg-primary-container text-on-primary-container hover:bg-primary-container/80',
  outlined:  'border border-outline-variant text-on-surface hover:bg-surface-container-low',
  ghost:     'text-on-surface hover:bg-surface-container',
  danger:    'bg-error text-on-error hover:bg-error/90 shadow-sm',
  success:   'bg-success text-white hover:bg-success/90 shadow-sm',
}

const sizeMap = {
  sm: 'px-3 py-1.5 text-label-sm',
  md: 'px-4 py-2.5 text-label-md',
  lg: 'px-6 py-3 text-label-lg',
}

const classes = computed(() => [
  base,
  variantMap[props.variant] ?? variantMap.primary,
  sizeMap[props.size]       ?? sizeMap.md,
])
</script>
