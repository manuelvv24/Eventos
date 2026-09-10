<template>
  <div class="flex flex-col gap-1">
    <!-- Label -->
    <label v-if="label" :for="inputId" class="text-label-lg text-on-surface font-medium">
      {{ label }}<span v-if="required" class="text-error ml-0.5">*</span>
    </label>

    <!-- Input wrapper -->
    <div
      class="relative flex items-center rounded-lg border bg-surface-container-lowest transition-all duration-200"
      :class="wrapperClass"
    >
      <span v-if="prefixIcon" class="material-symbols-outlined text-outline text-[20px] ml-3 shrink-0">
        {{ prefixIcon }}
      </span>

      <component
        :is="type === 'textarea' ? 'textarea' : 'input'"
        :id="inputId"
        :type="type === 'textarea' ? undefined : innerType"
        :value="modelValue"
        :placeholder="placeholder"
        :disabled="disabled"
        :required="required"
        :min="min"
        :max="max"
        :rows="rows"
        class="flex-1 bg-transparent px-3 py-2.5 text-body-md text-on-surface placeholder:text-outline focus:outline-none disabled:cursor-not-allowed resize-none"
        :class="{ 'resize-y': type === 'textarea' }"
        v-bind="$attrs"
        @input="$emit('update:modelValue', $event.target.value)"
      />

      <!-- Toggle password -->
      <button
        v-if="type === 'password'"
        type="button"
        class="mr-2 text-outline hover:text-on-surface transition-colors p-1 rounded-full"
        @click="showPass = !showPass"
      >
        <span class="material-symbols-outlined text-[20px]">{{ showPass ? 'visibility_off' : 'visibility' }}</span>
      </button>

      <span v-if="suffixIcon && type !== 'password'" class="material-symbols-outlined text-outline text-[20px] mr-3 shrink-0">
        {{ suffixIcon }}
      </span>
    </div>

    <!-- Error / hint -->
    <p v-if="error" class="flex items-center gap-1 text-body-sm text-error">
      <span class="material-symbols-outlined text-[14px]">error</span>{{ error }}
    </p>
    <p v-else-if="hint" class="text-body-sm text-on-surface-variant">{{ hint }}</p>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'

defineOptions({ inheritAttrs: false })

const props = defineProps({
  modelValue:  { type: [String, Number], default: '' },
  label:       { type: String, default: null },
  type:        { type: String, default: 'text' },
  placeholder: { type: String, default: '' },
  prefixIcon:  { type: String, default: null },
  suffixIcon:  { type: String, default: null },
  error:       { type: String, default: null },
  hint:        { type: String, default: null },
  disabled:    { type: Boolean, default: false },
  required:    { type: Boolean, default: false },
  min:         { type: [String, Number], default: null },
  max:         { type: [String, Number], default: null },
  rows:        { type: Number, default: 3 },
})
defineEmits(['update:modelValue'])

const showPass  = ref(false)
const inputId   = `input-${Math.random().toString(36).slice(2)}`
const innerType = computed(() => props.type === 'password' && showPass.value ? 'text' : props.type)

const wrapperClass = computed(() => {
  if (props.disabled) return 'border-outline-variant/50 bg-surface-container opacity-60 cursor-not-allowed'
  if (props.error)    return 'border-error ring-1 ring-error'
  return 'border-outline-variant focus-within:border-primary focus-within:ring-2 focus-within:ring-primary/20'
})
</script>
