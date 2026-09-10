<template>
  <div class="min-h-screen bg-background flex items-center justify-center p-4 font-sans">
    <div class="w-full max-w-md bg-white rounded-2xl shadow-lg border border-outline-variant/20 overflow-hidden">

      <!-- Header azul -->
      <RouterLink
        to="/"
        class="flex items-center gap-2 shrink-0 px-6 py-5 bg-primary transition-colors duration-200 hover:bg-secondary text-headline-md font-bold text-white"
        aria-label="Kinetic Pulse — Ir al inicio"
      >
        <span class="material-symbols-outlined text-[28px]" :style="{ fontVariationSettings: '\'FILL\' 1' }">graphic_eq</span>
        Kinetic Pulse
      </RouterLink>

      <div class="p-8">
        <!-- Paso 1: Email -->
        <template v-if="paso === 1">
          <h2 class="text-title-lg font-semibold text-on-surface mb-2">Recuperar contraseña</h2>
          <p class="text-body-sm text-on-surface-variant mb-6">Ingresa tu correo y te enviaremos un código de recuperación.</p>

          <div v-if="errorMsg" class="mb-4 p-3 rounded-lg bg-error-container text-error text-body-sm flex items-center gap-2">
            <span class="material-symbols-outlined text-[18px]">error</span>
            {{ errorMsg }}
          </div>

          <form class="space-y-4" @submit.prevent="enviarCodigo">
            <div>
              <label class="block text-label-lg text-on-surface mb-1">Correo electrónico *</label>
              <input
                v-model="email" type="email" required placeholder="correo@ejemplo.com" :disabled="loading"
                class="w-full px-4 py-3 rounded-lg border border-outline-variant focus:outline-none focus:border-primary focus:ring-2 focus:ring-primary/20 text-body-md bg-white disabled:opacity-60"
              />
            </div>
            <AppButton type="submit" size="lg" class="w-full" :loading="loading" icon="mail">
              {{ loading ? 'Enviando...' : 'Enviar código' }}
            </AppButton>
          </form>
        </template>

        <!-- Paso 2: Código -->
        <template v-if="paso === 2">
          <h2 class="text-title-lg font-semibold text-on-surface mb-2">Ingresa el código</h2>
          <p class="text-body-sm text-on-surface-variant mb-6">Revisa tu correo de recuperación. Si no lo encuentras, revisa la bandeja de spam.</p>

          <div v-if="errorMsg" class="mb-4 p-3 rounded-lg bg-error-container text-error text-body-sm flex items-center gap-2">
            <span class="material-symbols-outlined text-[18px]">error</span>
            {{ errorMsg }}
          </div>

          <div v-if="successMsg" class="mb-4 p-3 rounded-lg bg-success-container text-success text-body-sm flex items-center gap-2">
            <span class="material-symbols-outlined text-[18px]">check_circle</span>
            {{ successMsg }}
          </div>

          <form class="space-y-4" @submit.prevent="verificarCodigo">
            <div>
              <label class="block text-label-lg text-on-surface mb-1">Código de 6 dígitos *</label>
              <input
                v-model="codigo" type="text" inputmode="numeric" maxlength="6" required
                placeholder="000000" :disabled="loading"
                class="w-full px-4 py-3 rounded-lg border border-outline-variant focus:outline-none focus:border-primary focus:ring-2 focus:ring-primary/20 text-body-md bg-white disabled:opacity-60 text-center font-mono text-[26px] tracking-[0.3em]"
                @input="codigo = codigo.replace(/[^0-9]/g, '').slice(0, 6)"
              />
            </div>
            <AppButton type="submit" size="lg" class="w-full" :loading="loading" icon="check">
              {{ loading ? 'Verificando...' : 'Verificar código' }}
            </AppButton>
          </form>

          <p class="mt-4 text-center text-body-sm text-on-surface-variant">
            <button class="text-primary font-semibold hover:underline" @click="paso = 1; errorMsg = ''; successMsg = ''">
              Volver a enviar código
            </button>
          </p>
        </template>

        <!-- Paso 3: Nueva contraseña -->
        <template v-if="paso === 3">
          <h2 class="text-title-lg font-semibold text-on-surface mb-2">Nueva contraseña</h2>
          <p class="text-body-sm text-on-surface-variant mb-6">Crea una contraseña nueva con al menos 8 caracteres.</p>

          <div v-if="errorMsg" class="mb-4 p-3 rounded-lg bg-error-container text-error text-body-sm flex items-center gap-2">
            <span class="material-symbols-outlined text-[18px]">error</span>
            {{ errorMsg }}
          </div>

          <form class="space-y-4" @submit.prevent="restablecer">
            <div>
              <label class="block text-label-lg text-on-surface mb-1">Nueva contraseña *</label>
              <div class="relative">
                <input
                  v-model="nuevaPassword"
                  :type="mostrarPassword ? 'text' : 'password'"
                  required placeholder="••••••••" minlength="8" :disabled="loading"
                  class="w-full px-4 py-3 pr-12 rounded-lg border border-outline-variant focus:outline-none focus:ring-2 focus:ring-primary/20 text-body-md disabled:opacity-60"
                  :class="nuevaPassword && !esValida ? 'border-error focus:border-error' : 'focus:border-primary'"
                />
                <button
                  type="button" tabindex="-1"
                  class="absolute right-3 top-1/2 -translate-y-1/2 text-on-surface-variant hover:text-primary transition-colors"
                  @click="mostrarPassword = !mostrarPassword"
                >
                  <span class="material-symbols-outlined text-[22px]">{{ mostrarPassword ? 'visibility_off' : 'visibility' }}</span>
                </button>
              </div>

              <template v-if="nuevaPassword">
                <div class="flex gap-1 mt-2" aria-hidden="true">
                  <div
                    v-for="i in 5" :key="i"
                    class="h-1.5 flex-1 rounded-full transition-colors duration-300"
                    :class="i <= fortaleza.nivel ? fortaleza.color : 'bg-surface-container-highest'"
                  ></div>
                </div>
                <p class="text-label-sm mt-1 font-medium" :class="fortaleza.textoClase">
                  Fortaleza: {{ fortaleza.texto }}
                </p>
                <ul class="mt-2 grid grid-cols-1 sm:grid-cols-2 gap-x-4 gap-y-1">
                  <li
                    v-for="r in reglas" :key="r.key"
                    class="flex items-center gap-1.5 text-label-sm"
                    :class="r.cumple ? 'text-success' : 'text-on-surface-variant'"
                  >
                    <span class="material-symbols-outlined text-[16px]" style="font-variation-settings:'FILL' 1">
                      {{ r.cumple ? 'check_circle' : 'radio_button_unchecked' }}
                    </span>
                    {{ r.label }}
                  </li>
                </ul>
              </template>
            </div>

            <div>
              <label class="block text-label-lg text-on-surface mb-1">Confirmar contraseña *</label>
              <input
                v-model="confirmarPassword"
                :type="mostrarPassword ? 'text' : 'password'"
                required placeholder="••••••••" :disabled="loading"
                class="w-full px-4 py-3 rounded-lg border border-outline-variant focus:outline-none focus:ring-2 focus:ring-primary/20 text-body-md disabled:opacity-60"
                :class="confirmarPassword && !coinciden ? 'border-error focus:border-error' : (confirmarPassword && coinciden ? 'border-success focus:border-success' : 'focus:border-primary')"
              />
              <p v-if="confirmarPassword && !coinciden" class="mt-1 text-label-sm text-error flex items-center gap-1">
                <span class="material-symbols-outlined text-[15px]" style="font-variation-settings:'FILL' 1">error</span>
                Las contraseñas no coinciden
              </p>
            </div>

            <AppButton type="submit" size="lg" class="w-full" :loading="loading" icon="lock_reset">
              {{ loading ? 'Guardando...' : 'Restablecer contraseña' }}
            </AppButton>
          </form>
        </template>

        <p class="mt-6 text-center text-body-sm text-on-surface-variant">
          <RouterLink to="/login" class="text-primary font-semibold hover:underline ml-1">Volver al inicio de sesión</RouterLink>
        </p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, toRef } from 'vue'
import { useRouter } from 'vue-router'
import authAPI from '../../services/authAPI.js'
import { usePasswordStrength } from '../../composables/usePasswordStrength.js'
import AppButton from '../../components/ui/AppButton.vue'

const router = useRouter()

const paso      = ref(1)
const email     = ref('')
const codigo    = ref('')
const token     = ref('')
const nuevaPassword = ref('')
const confirmarPassword = ref('')
const mostrarPassword   = ref(false)

const loading  = ref(false)
const errorMsg = ref('')
const successMsg = ref('')

const { reglas, fortaleza, esValida } = usePasswordStrength(toRef(() => nuevaPassword.value))
const coinciden = computed(() =>
  nuevaPassword.value.length > 0 && nuevaPassword.value === confirmarPassword.value
)

function handleError(err) {
  errorMsg.value = err?.response?.data?.error || err?.message || 'Ocurrió un error inesperado'
}

async function enviarCodigo() {
  errorMsg.value   = ''
  loading.value    = true
  try {
    await authAPI.recuperar(email.value)
    successMsg.value = 'Si el correo está registrado, recibirás un código de recuperación.'
    errorMsg.value = ''
    paso.value = 2
  } catch (err) {
    handleError(err)
  } finally {
    loading.value = false
  }
}

async function verificarCodigo() {
  errorMsg.value = ''
  loading.value  = true
  try {
    const { data } = await authAPI.verificarCodigo(email.value, codigo.value)
    token.value   = data.token
    successMsg.value = ''
    errorMsg.value   = ''
    paso.value = 3
  } catch (err) {
    handleError(err)
  } finally {
    loading.value = false
  }
}

async function restablecer() {
  if (!esValida.value) { errorMsg.value = 'La contraseña no cumple los requisitos de seguridad'; return }
  if (!coinciden.value) { errorMsg.value = 'Las contraseñas no coinciden'; return }
  errorMsg.value  = ''
  loading.value   = true
  try {
    await authAPI.restablecer(token.value, nuevaPassword.value)
    router.push('/login')
  } catch (err) {
    handleError(err)
  } finally {
    loading.value = false
  }
}
</script>
