<template>
  <div class="min-h-screen bg-background flex items-center justify-center p-4 font-sans">
    <div class="w-full max-w-md bg-white rounded-2xl shadow-lg border border-outline-variant/20 overflow-hidden">
      
      <!-- Header azul -->
      <div class="bg-primary p-6 text-white">
        <!-- Marca -->
        <RouterLink
          to="/"
          class="flex items-center gap-2 shrink-0 text-headline-md font-bold text-white transition-colors duration-200 hover:text-secondary"
          aria-label="Kinetic Pulse — Ir al inicio"
        >
          <span class="material-symbols-outlined text-[28px]" :style="{ fontVariationSettings: '\'FILL\' 1' }">graphic_eq</span>
          Kinetic Pulse
        </RouterLink>

        <p class="text-white/80 text-body-sm mt-1">Sistema de Gestión de Eventos</p>
      </div>

      <!-- Formulario -->
      <div class="p-8">
        <h2 class="text-title-lg font-semibold text-on-surface mb-6">Iniciar Sesión</h2>

        <!-- Error -->
        <div v-if="errorMsg" class="mb-4 p-3 rounded-lg bg-error-container text-error text-body-sm flex items-center gap-2">
          <span class="material-symbols-outlined text-[18px]">error</span>
          {{ errorMsg }}
        </div>

        <!-- Selector de rol (cuenta con varios roles) -->
        <div v-if="mostrarSelector" class="space-y-4">
          <p class="text-body-sm text-on-surface-variant">
            La cuenta
            <span class="font-semibold text-on-surface">{{ email }}</span>
            tiene varios roles. Elige con cuál quieres entrar:
          </p>

          <div class="space-y-3">
            <button
              v-for="rol in rolesDisponibles"
              :key="rol"
              type="button"
              :disabled="loading"
              @click="elegirRol(rol)"
              class="w-full flex items-center justify-between p-4 rounded-xl border border-outline-variant hover:border-primary hover:bg-primary/5 transition-colors text-left disabled:opacity-60"
            >
              <span class="flex items-center gap-3">
                <span class="material-symbols-outlined text-primary">badge</span>
                <span>
                  <span class="block text-body-md font-semibold text-on-surface">{{ rol }}</span>
                  <span class="block text-body-sm text-on-surface-variant">{{ panelLabel(rol) }}</span>
                </span>
              </span>
              <span
                v-if="rol === auth.user?.rol"
                class="material-symbols-outlined text-primary"
                :style="{ fontVariationSettings: '\'FILL\' 1' }"
              >check_circle</span>
              <span v-else class="material-symbols-outlined text-on-surface-variant">chevron_right</span>
            </button>
          </div>

          <button
            type="button"
            class="text-body-sm text-primary font-semibold hover:underline flex items-center gap-1"
            :disabled="loading"
            @click="volverAlFormulario"
          >
            <span class="material-symbols-outlined text-[16px]">arrow_back</span>
            Volver
          </button>
        </div>

        <form v-else class="space-y-4" @submit.prevent="handleLogin">
          <!-- Email -->
          <div>
            <label class="block text-label-lg text-on-surface mb-1">Correo electrónico *</label>
            <input
              v-model="email"
              type="email"
              placeholder="correo@ejemplo.com"
              required
              :disabled="loading"
              class="w-full px-4 py-3 rounded-lg border border-outline-variant focus:outline-none focus:border-primary focus:ring-2 focus:ring-primary/20 text-body-md bg-white disabled:opacity-60"
            />
          </div>
          <div>
            <label class="block text-label-lg text-on-surface mb-1">Contraseña *</label>
            <input
              v-model="password"
              type="password"
              placeholder="••••••••"
              required
              :disabled="loading"
              autocomplete="off"
              @paste.prevent
              @copy.prevent
              @cut.prevent
              @drop.prevent
              @contextmenu.prevent
              class="w-full px-4 py-3 rounded-lg border border-outline-variant focus:outline-none focus:border-primary focus:ring-2 focus:ring-primary/20 text-body-md bg-white disabled:opacity-60"
            />
          </div>

          <!-- Submit -->
          <AppButton type="submit" size="lg" class="w-full" :loading="loading" icon="arrow_forward">
            {{ loading ? 'Iniciando sesión...' : 'Iniciar Sesión' }}
          </AppButton>
        </form>

        <div class="mt-4 text-center">
          <RouterLink to="/recuperar" class="text-label-md text-primary font-semibold hover:underline">
            ¿Olvidaste tu contraseña?
          </RouterLink>
        </div>

        <p class="mt-6 text-center text-body-sm text-on-surface-variant">
          ¿No tienes cuenta?
          <RouterLink to="/registro" class="text-primary font-semibold hover:underline ml-1">
            Crear cuenta
          </RouterLink>
        </p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '../../stores/auth.js'
import { panelLabel } from '../../constants/index.js'
import AppButton from '../../components/ui/AppButton.vue'

const auth     = useAuthStore()
const router   = useRouter()
const route    = useRoute()

const email    = ref('')
const password = ref('')
const loading  = ref(false)
const errorMsg = ref('')

const mostrarSelector  = ref(false)
const rolesDisponibles = ref([])

function irAlDestino() {
  // Prioridad: ?redirect= de la ruta; si no, home según el rol
  const destino = route.query.redirect ? String(route.query.redirect) : auth.homeRoute
  router.push(destino)
}

async function handleLogin() {
  if (!email.value || !password.value) {
    errorMsg.value = 'Completa todos los campos'
    return
  }

  errorMsg.value = ''
  loading.value  = true

  try {
    await auth.login(email.value, password.value)

    const roles = auth.user?.roles || []
    if (roles.length > 1) {
      rolesDisponibles.value = roles
      mostrarSelector.value  = true
      return
    }

    irAlDestino()

  } catch (err) {
    errorMsg.value = err?.message || 'Credenciales incorrectas'
  } finally {
    loading.value = false
  }
}

async function elegirRol(rol) {
  errorMsg.value = ''
  loading.value  = true

  try {
    await auth.cambiarRol(rol)
    irAlDestino()
  } catch (err) {
    errorMsg.value = err?.message || 'Error al cambiar de rol'
  } finally {
    loading.value = false
  }
}

function volverAlFormulario() {
  mostrarSelector.value = false
  errorMsg.value        = ''
}
</script>