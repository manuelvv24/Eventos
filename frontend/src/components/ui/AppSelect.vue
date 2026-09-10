<template>
  <div class="flex flex-col gap-1">
    <label v-if="label" :for="selectId" class="text-label-lg text-on-surface font-medium">
      {{ label }}<span v-if="required" class="text-error ml-0.5">*</span>
    </label>

    <div
      class="relative flex items-center rounded-lg border bg-surface-container-lowest transition-all duration-200"
      :class="wrapperClass"
    >
      <span v-if="prefixIcon" class="material-symbols-outlined text-outline text-[20px] ml-3 shrink-0">
        {{ prefixIcon }}
      </span>

      <select
        :id="selectId"
        :value="modelValue"
        :disabled="disabled"
        :required="required"
        class="flex-1 bg-transparent py-2.5 pr-10 pl-3 text-body-md text-on-surface appearance-none focus:outline-none disabled:cursor-not-allowed cursor-pointer"
        v-bind="$attrs"
        @change="$emit('update:modelValue', $event.target.value)"
      >
        <option v-if="placeholder" value="" disabled :selected="!modelValue">{{ placeholder }}</option>
        <option
          v-for="opt in options"
          :key="opt.value ?? opt"
          :value="opt.value ?? opt"
        >
          {{ opt.label ?? opt }}
        </option>
      </select>

      <span class="absolute right-3 material-symbols-outlined text-outline text-[20px] pointer-events-none">
        expand_more
      </span>
    </div>

    <p v-if="error" class="flex items-center gap-1 text-body-sm text-error">
      <span class="material-symbols-outlined text-[14px]">error</span>{{ error }}
    </p>
  </div>
</template>

<script setup>
import { computed } from 'vue'

defineOptions({ inheritAttrs: false })

const props = defineProps({
  modelValue:  { type: [String, Number], default: '' },
  label:       { type: String, default: null },
  placeholder: { type: String, default: 'Seleccione...' },
  options:     { type: Array, default: () => [] },
  prefixIcon:  { type: String, default: null },
  error:       { type: String, default: null },
  disabled:    { type: Boolean, default: false },
  required:    { type: Boolean, default: false },
})
defineEmits(['update:modelValue'])

const selectId = `select-${Math.random().toString(36).slice(2)}`
const wrapperClass = computed(() => {
  if (props.disabled) return 'border-outline-variant/50 opacity-60 cursor-not-allowed'
  if (props.error)    return 'border-error ring-1 ring-error'
  return 'border-outline-variant focus-within:border-primary focus-within:ring-2 focus-within:ring-primary/20'
})
</script>