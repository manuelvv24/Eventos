<template>
  <!-- Desktop sidebar -->
  <aside class="hidden md:flex flex-col w-64 h-full shrink-0 bg-surface-container-low border-r border-outline-variant/30">
    <!-- Logo (ACTUALIZADO) -->
    <RouterLink 
      to="/" 
      class="px-6 py-5 flex items-center gap-2 shrink-0 transition-colors duration-200 hover:bg-surface-container-high"
    >
      <span class="material-symbols-outlined text-[28px] text-primary" :style="{ fontVariationSettings: '\'FILL\' 1' }">graphic_eq</span>
      <div class="flex flex-col leading-tight">
        <span class="text-title-sm font-bold text-primary">Kinetic Pulse</span>
        <span class="text-label-sm text-on-surface-variant font-normal">Event Platform</span>
      </div>
    </RouterLink>

    <!-- User card -->
    <div class="mx-4 mb-4 p-3 rounded-xl bg-surface-container flex items-center gap-3 shrink-0">
      <div class="w-9 h-9 rounded-full bg-primary flex items-center justify-center text-on-primary font-bold text-body-md shrink-0">
        {{ initials }}
      </div>
      <div class="flex-1 min-w-0">
        <p class="text-label-lg text-on-surface font-semibold truncate">{{ auth.nombreCompleto }}</p>
        <AppBadge :variant="roleBadgeVariant" class="mt-0.5">{{ auth.rol }}</AppBadge>
      </div>
    </div>

    <!-- Nav -->
    <nav class="flex-1 px-3 space-y-0.5 overflow-y-auto custom-scrollbar">
      <RouterLink
        v-for="item in auth.navItems"
        :key="item.route"
        v-slot="{ isActive, navigate }"
        :to="item.route"
        custom
      >
        <button
          class="w-full flex items-center gap-3 px-3 py-2.5 rounded-lg text-body-md transition-base"
          :class="isActive
            ? 'bg-primary text-on-primary font-semibold'
            : 'text-on-surface-variant hover:bg-surface-container-high hover:text-on-surface'"
          @click="navigate(); $emit('close')"
        >
          <span
            class="material-symbols-outlined text-[22px] shrink-0"
            :style="isActive ? 'font-variation-settings:\'FILL\' 1' : ''"
          >{{ item.icon }}</span>
          <span class="text-label-lg">{{ item.label }}</span>
        </button>
      </RouterLink>
    </nav>
  </aside>

  <!-- Mobile drawer overlay -->
  <Transition name="drawer">
    <div v-if="mobileOpen" class="md:hidden fixed inset-0 z-50 flex">
      <div class="absolute inset-0 bg-black/50" @click="$emit('close')" />
      <aside class="relative w-72 h-full bg-surface-container-low flex flex-col shadow-elevation-3">
        
        <!-- Logo Mobile (ACTUALIZADO) -->
        <div class="px-6 py-5 flex items-center gap-2 shrink-0">
          <RouterLink to="/" class="flex items-center gap-2 flex-1" @click="$emit('close')">
            <span class="material-symbols-outlined text-[28px] text-primary" :style="{ fontVariationSettings: '\'FILL\' 1' }">graphic_eq</span>
            <p class="text-title-sm font-bold text-primary">Kinetic Pulse</p>
          </RouterLink>
          <button class="text-on-surface-variant p-1 rounded-full hover:bg-surface-container-high" @click="$emit('close')">
            <span class="material-symbols-outlined">close</span>
          </button>
        </div>

        <div class="mx-4 mb-4 p-3 rounded-xl bg-surface-container flex items-center gap-3">
          <div class="w-9 h-9 rounded-full bg-primary flex items-center justify-center text-on-primary font-bold text-body-md">
            {{ initials }}
          </div>
          <div class="flex-1 min-w-0">
            <p class="text-label-lg text-on-surface font-semibold truncate">{{ auth.nombreCompleto }}</p>
            <AppBadge :variant="roleBadgeVariant">{{ auth.rol }}</AppBadge>
          </div>
        </div>

        <nav class="flex-1 px-3 space-y-0.5 overflow-y-auto custom-scrollbar">
          <RouterLink
            v-for="item in auth.navItems"
            :key="item.route"
            v-slot="{ isActive, navigate }"
            :to="item.route"
            custom
          >
            <button
              class="w-full flex items-center gap-3 px-3 py-2.5 rounded-lg text-body-md transition-base"
              :class="isActive
                ? 'bg-primary text-on-primary font-semibold'
                : 'text-on-surface-variant hover:bg-surface-container-high hover:text-on-surface'"
              @click="navigate(); $emit('close')"
            >
              <span class="material-symbols-outlined text-[22px] shrink-0" :style="isActive ? 'font-variation-settings:\'FILL\' 1' : ''">{{ item.icon }}</span>
              <span class="text-label-lg">{{ item.label }}</span>
            </button>
          </RouterLink>
        </nav>

        <div class="p-3 border-t border-outline-variant/30 space-y-0.5">
          <button class="w-full flex items-center gap-3 px-3 py-2.5 rounded-lg text-error hover:bg-error-container/30 transition-base text-label-lg" @click="handleLogout">
            <span class="material-symbols-outlined text-[22px]">logout</span>
            Cerrar sesión
          </button>
        </div>
      </aside>
    </div>
  </Transition>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../../stores/auth.js'
import { useConfirm } from '../../composables/useConfirm.js'
import AppBadge from '../ui/AppBadge.vue'

defineProps({
  mobileOpen: { type: Boolean, default: false },
})
defineEmits(['close'])

const auth   = useAuthStore()
const router = useRouter()
const { confirm } = useConfirm()

const initials = computed(() => {
  const name = auth.nombreCompleto || ''
  return name.split(' ').slice(0, 2).map(w => w[0]?.toUpperCase() ?? '').join('')
})

const roleBadgeVariant = computed(() => ({
  'Super Administrador': 'error',
  Administrador:         'warning',
  Operador:              'info',
  Monitor:              'neutral',
  Invitado:             'neutral',
}[auth.rol] ?? 'neutral'))

async function handleLogout() {
  const ok = await confirm({
    title: 'Cerrar sesión',
    message: `¿Seguro que quieres cerrar sesión, ${auth.nombreCompleto || 'usuario'}?`,
    confirmText: 'Sí, cerrar sesión',
    cancelText: 'Cancelar',
    danger: false,
  })
  if (!ok) return
  auth.logout()
  router.push('/login')
}
</script>

<style scoped>
.drawer-enter-active, .drawer-leave-active { transition: opacity 0.25s ease; }
.drawer-enter-active aside, .drawer-leave-active aside { transition: transform 0.25s ease; }
.drawer-enter-from, .drawer-leave-to { opacity: 0; }
.drawer-enter-from aside, .drawer-leave-to aside { transform: translateX(-100%); }
</style>