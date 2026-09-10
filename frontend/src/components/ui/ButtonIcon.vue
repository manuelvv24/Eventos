<template>
  <component
    :is="to ? 'router-link' : 'button'"
    :to="to"
    :type="to ? undefined : 'button'"
    :title="title"
    :aria-label="title"
    :disabled="disabled"
    :class="[base, variantMap[variant] ?? variantMap.ghost]"
    v-bind="$attrs"
  >
    <span class="material-symbols-outlined text-[18px]">{{ icon }}</span>
  </component>
</template>

<!--
  Botón de solo icono (acciones de tabla, navegación, cerrar).
  Estandariza el patrón w-8 h-8 usado en todas las vistas.
-->
<script setup>
defineProps({
  /** Nombre del ícono de Material Symbols */
  icon: { type: String, required: true },
  /** Texto para tooltip y accesibilidad (obligatorio) */
  title: { type: String, required: true },
  /** ghost = hover neutro con tinte primario · danger = hover rojo (eliminar/bloquear) */
  variant: { type: String, default: 'ghost' },
  disabled: { type: Boolean, default: false },
  /** Si se define, renderiza como RouterLink */
  to: { type: [String, Object], default: null },
})

const base =
  'w-8 h-8 rounded-lg inline-flex items-center justify-center transition-base select-none focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary disabled:opacity-40 disabled:pointer-events-none'

const variantMap = {
  ghost: 'text-on-surface-variant hover:bg-surface-container hover:text-primary',
  danger: 'text-on-surface-variant hover:bg-error-container/30 hover:text-error',
}
</script>
