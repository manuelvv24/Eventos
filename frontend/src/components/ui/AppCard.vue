<template>
  <div :class="classes">
    <div v-if="$slots.header || title" class="px-6 py-4 border-b border-outline-variant/30 flex items-center justify-between gap-4">
      <div>
        <h3 v-if="title" class="text-title-md text-on-surface font-semibold">{{ title }}</h3>
        <p v-if="subtitle" class="text-body-sm text-on-surface-variant mt-0.5">{{ subtitle }}</p>
      </div>
      <slot name="header-action" />
    </div>

    <div :class="bodyClass">
      <slot />
    </div>

    <div v-if="$slots.footer" class="px-6 py-4 border-t border-outline-variant/30 bg-surface-container-low/30 rounded-b-xl">
      <slot name="footer" />
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  title:    { type: String, default: null },
  subtitle: { type: String, default: null },
  padding:  { type: String, default: 'normal' }, // none | normal | lg
  shadow:   { type: Boolean, default: true },
})

const classes = computed(() => [
  'bg-white rounded-xl border border-outline-variant/20 overflow-hidden',
  props.shadow ? 'shadow-elevation-1' : '',
])

const bodyClass = computed(() => ({
  none:   '',
  normal: 'p-6',
  lg:     'p-8',
}[props.padding] ?? 'p-6'))
</script>
