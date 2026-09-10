<template>
  <header class="sticky top-0 z-40 h-14 bg-surface border-b border-outline-variant/30 flex items-center px-4 gap-3 shrink-0">
    <!-- Mobile menu toggle -->
    <button
      class="md:hidden w-9 h-9 flex items-center justify-center rounded-full text-on-surface-variant hover:bg-surface-container transition-base"
      aria-label="Menú"
      @click="$emit('toggle-sidebar')"
    >
      <span class="material-symbols-outlined">menu</span>
    </button>

    <!-- Page title (mobile) -->
    <span class="md:hidden text-title-sm font-bold text-primary flex-1">Kinetic Pulse</span>

    

    <!-- Botones del lado derecho (notificaciones y usuario) -->
    <div class="flex items-center gap-1 md:ml-auto">
      <!-- 🔔 Campanita de notificaciones -->
      <AppNotificaciones />

      <!-- Avatar de usuario -->
      <div class="relative">
        <button
          class="w-9 h-9 rounded-full bg-primary text-on-primary font-semibold text-label-md flex items-center justify-center hover:opacity-90 transition-base"
          :title="auth.nombreCompleto"
          @click="mostrarMenuUsuario = !mostrarMenuUsuario"
        >
          {{ initials }}
        </button>

        <!-- Dropdown de usuario -->
        <div
          v-if="mostrarMenuUsuario"
          class="absolute right-0 top-full mt-2 w-56 rounded-xl border border-outline-variant/20 bg-white shadow-lg z-50"
        >
          <div class="border-b border-outline-variant/10 px-4 py-3">
            <p class="text-label-lg font-semibold text-on-surface truncate">{{ auth.nombreCompleto }}</p>
            <p class="text-label-sm text-on-surface-variant truncate">{{ auth.user?.emailUsuario }}</p>
          </div>
          <template v-if="otrosRoles.length">
            <div class="border-t border-outline-variant/10 px-4 py-2">
              <p class="text-label-sm text-on-surface-variant mb-1 flex items-center gap-1">
                <span class="material-symbols-outlined text-[14px]">swap_horiz</span>
                Cambiar de rol
              </p>
              <div class="space-y-1">
                <button
                  v-for="rol in otrosRoles"
                  :key="rol"
                  class="w-full text-left px-2 py-1.5 rounded-lg text-body-sm text-on-surface hover:bg-surface-container transition flex items-center justify-between gap-2"
                  @click="cambiarRol(rol)"
                >
                  <span class="truncate">{{ rol }}</span>
                  <span class="material-symbols-outlined text-[16px] text-primary">swap_horiz</span>
                </button>
              </div>
            </div>
          </template>
          <button
            class="w-full text-left px-4 py-2.5 text-body-sm text-on-surface hover:bg-surface-container transition flex items-center gap-3"
            @click="logout"
          >
            <span class="material-symbols-outlined text-[18px]">logout</span>
            Cerrar sesión
          </button>
        </div>
      </div>
    </div>
  </header>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../../stores/auth.js'
import { useConfirm } from '../../composables/useConfirm.js'
import AppNotificaciones from '../AppNotificaciones.vue'

defineEmits(['toggle-sidebar'])

const router = useRouter()
const auth = useAuthStore()
const { confirm } = useConfirm()
const mostrarMenuUsuario = ref(false)

const otrosRoles = computed(() => {
  const roles = Array.isArray(auth.user?.roles) ? auth.user.roles : []
  return roles.filter(r => r !== auth.rol)
})

const initials = computed(() => {
  const name = auth.nombreCompleto || ''
  return name.split(' ').slice(0, 2).map(w => w[0]?.toUpperCase() ?? '').join('')
})

async function cambiarRol(rol) {
  mostrarMenuUsuario.value = false
  try {
    await auth.cambiarRol(rol)
    router.push(auth.homeRoute)
  } catch (err) {
    alert(err?.message || 'No se pudo cambiar de rol')
  }
}

async function logout() {
  const ok = await confirm({
    title: 'Cerrar sesión',
    message: `¿Seguro que quieres cerrar sesión, ${auth.nombreCompleto || 'usuario'}?`,
    confirmText: 'Sí, cerrar sesión',
    cancelText: 'Cancelar',
    danger: false,
  })
  if (!ok) return
  auth.logout()
  mostrarMenuUsuario.value = false
  router.push('/login')
}

function cerrarMenuSiClickFuera(e) {
  const dropdown = e.target.closest('.relative')
  if (mostrarMenuUsuario.value && !dropdown) {
    mostrarMenuUsuario.value = false
  }
}

onMounted(() => {
  document.addEventListener('click', cerrarMenuSiClickFuera)
})

onUnmounted(() => {
  document.removeEventListener('click', cerrarMenuSiClickFuera)
})
</script>