<template>
  <span :class="classes">
    <span v-if="dot" class="w-1.5 h-1.5 rounded-full" :class="dotClass"></span>
    <slot />
  </span>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  variant: { type: String, default: 'info' }, // info | success | warning | error | neutral
  dot:     { type: Boolean, default: false },
})

const variantMap = {
  info:    'bg-primary-container text-on-primary-container',
  success: 'bg-success-container text-success',
  warning: 'bg-warning-container text-warning',
  error:   'bg-error-container text-error',
  neutral: 'bg-surface-container text-on-surface-variant',
}

const dotColorMap = {
  info:    'bg-primary',
  success: 'bg-success',
  warning: 'bg-warning',
  error:   'bg-error',
  neutral: 'bg-outline',
}

const base = 'inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-label-sm font-semibold'
const classes   = computed(() => [base, variantMap[props.variant]  ?? variantMap.info])
const dotClass  = computed(() =>       dotColorMap[props.variant]  ?? dotColorMap.info)
</script>
