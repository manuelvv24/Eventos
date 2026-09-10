<template>
  <span
    class="material-symbols-outlined animate-spin select-none"
    :class="sizeClass"
    aria-label="Cargando"
  >progress_activity</span>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  size: { type: String, default: 'md' }, // sm | md | lg
})

const sizeClass = computed(() => ({
  sm: 'text-[18px]',
  md: 'text-[24px]',
  lg: 'text-[36px]',
}[props.size] ?? 'text-[24px]'))
</script>
