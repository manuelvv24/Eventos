<template>
  <div class="min-h-screen bg-background font-sans">
    <!-- Header (CORREGIDO: justify-center y sin el enlace superior) -->
    <header class="sticky top-0 bg-white border-b border-outline-variant/30 px-6 py-4 flex items-center justify-center z-50">
      <!-- Marca Centrada -->
      <RouterLink
        to="/"
        class="flex items-center gap-2 shrink-0 text-headline-md font-bold text-primary transition-colors duration-200 hover:text-secondary"
        aria-label="Kinetic Pulse — Ir al inicio"
      >
        <span class="material-symbols-outlined text-[28px]" :style="{ fontVariationSettings: '\'FILL\' 1' }">graphic_eq</span>
        Kinetic Pulse
      </RouterLink>
    </header>

    <main class="flex flex-col items-center px-4 py-10">
      <div class="w-full max-w-2xl">
        <h1 class="text-headline-md font-semibold text-on-surface mb-2 text-center">Crear cuenta</h1>
        <p class="text-body-md text-on-surface-variant mb-8 text-center">Regístrate para gestionar eventos.</p>

        <!-- Error -->
        <div v-if="errorMsg" class="mb-6 p-4 rounded-xl bg-error-container text-error text-body-sm flex items-center gap-2">
          <span class="material-symbols-outlined text-[20px]">error</span>
          {{ errorMsg }}
        </div>

        <form class="space-y-4" @submit.prevent="handleRegister">
          <!-- Nombres -->
          <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label class="block text-label-lg text-on-surface mb-1">Primer nombre *</label>
              <input
                v-model="form.primerNombre" type="text" required placeholder="Ej. Juan" :disabled="loading"
                maxlength="30"
                class="w-full px-4 py-3 rounded-lg border border-outline-variant focus:outline-none focus:border-primary focus:ring-2 focus:ring-primary/20 text-body-md disabled:opacity-60"
                @input="form.primerNombre = limpiarNombre($event)"
              />
              <p v-if="errorNombreCorto(form.primerNombre)" class="mt-1 text-label-sm text-error flex items-center gap-1">
                <span class="material-symbols-outlined text-[15px]" style="font-variation-settings:'FILL' 1">error</span>
                {{ errorNombreCorto(form.primerNombre) }}
              </p>
            </div>
            <div>
              <label class="block text-label-lg text-on-surface mb-1">Segundo nombre</label>
              <input
                v-model="form.segundoNombre" type="text" placeholder="(Opcional)" :disabled="loading"
                maxlength="30"
                class="w-full px-4 py-3 rounded-lg border border-outline-variant focus:outline-none focus:border-primary focus:ring-2 focus:ring-primary/20 text-body-md disabled:opacity-60"
                @input="form.segundoNombre = limpiarNombre($event)"
              />
            </div>
          </div>

          <!-- Apellidos -->
          <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label class="block text-label-lg text-on-surface mb-1">Primer apellido *</label>
              <input
                v-model="form.primerApellido" type="text" required placeholder="Ej. Pérez" :disabled="loading"
                maxlength="30"
                class="w-full px-4 py-3 rounded-lg border border-outline-variant focus:outline-none focus:border-primary focus:ring-2 focus:ring-primary/20 text-body-md disabled:opacity-60"
                @input="form.primerApellido = limpiarNombre($event)"
              />
              <p v-if="errorNombreCorto(form.primerApellido)" class="mt-1 text-label-sm text-error flex items-center gap-1">
                <span class="material-symbols-outlined text-[15px]" style="font-variation-settings:'FILL' 1">error</span>
                {{ errorNombreCorto(form.primerApellido) }}
              </p>
            </div>
            <div>
              <label class="block text-label-lg text-on-surface mb-1">Segundo apellido</label>
              <input
                v-model="form.segundoApellido" type="text" placeholder="(Opcional)" :disabled="loading"
                maxlength="30"
                class="w-full px-4 py-3 rounded-lg border border-outline-variant focus:outline-none focus:border-primary focus:ring-2 focus:ring-primary/20 text-body-md disabled:opacity-60"
                @input="form.segundoApellido = limpiarNombre($event)"
              />
            </div>
          </div>

          <!-- Documento -->
          <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label class="block text-label-lg text-on-surface mb-1">Tipo de documento *</label>
              <select
                v-model="form.tipoDocumento" required :disabled="loading"
                class="w-full px-4 py-3 rounded-lg border border-outline-variant focus:outline-none focus:border-primary text-body-md disabled:opacity-60 bg-white"
              >
                <option value="" disabled>Seleccione...</option>
                <option value="CC">Cédula de Ciudadanía</option>
                <option value="CE">Cédula de Extranjería</option>
                <option value="TI">Tarjeta de Identidad</option>
                <option value="PA">Pasaporte</option>
              </select>
            </div>
            <div>
              <label class="block text-label-lg text-on-surface mb-1">
                Número de documento * <span class="text-label-sm text-on-surface-variant font-normal">(máx. 18)</span>
              </label>
              <input
                v-model="form.numeroDocumento" type="text" required placeholder="Ej. 1020304050" :disabled="loading"
                class="w-full px-4 py-3 rounded-lg border border-outline-variant focus:outline-none focus:ring-2 text-body-md disabled:opacity-60 uppercase"
                :class="fieldErrors.numeroDocumento ? 'border-error focus:border-error focus:ring-error/20' : 'focus:border-primary focus:ring-primary/20'"
                maxlength="18" inputmode="numeric" autocomplete="off"
                @input="onNumeroDocumento($event)"
                @blur="chequearDisponibilidad('numeroDocumento')"
              />
              <p v-if="errorDocumento" class="mt-1 text-label-sm text-error flex items-center gap-1">
                <span class="material-symbols-outlined text-[15px]" style="font-variation-settings:'FILL' 1">error</span>
                {{ errorDocumento }}
              </p>
              <p v-if="fieldErrors.numeroDocumento" class="mt-1 text-label-sm text-error flex items-center gap-1">
                <span class="material-symbols-outlined text-[15px]" style="font-variation-settings:'FILL' 1">error</span>
                {{ fieldErrors.numeroDocumento }}
              </p>
            </div>
          </div>

          <!-- Teléfono -->
          <div>
            <label class="block text-label-lg text-on-surface mb-1">Teléfono *</label>
            <input
              v-model="form.telefono" type="tel" required placeholder="300 123 4567" :disabled="loading"
              class="w-full px-4 py-3 rounded-lg border border-outline-variant focus:outline-none focus:border-primary focus:ring-2 focus:ring-primary/20 text-body-md disabled:opacity-60"
              maxlength="15" inputmode="numeric" autocomplete="tel"
              @input="form.telefono = limpiarTelefono($event)"
            />
          </div>

          <!-- Email -->
          <div>
            <label class="block text-label-lg text-on-surface mb-1">Correo electrónico *</label>
            <input
              v-model="form.email" type="email" required placeholder="correo@ejemplo.com" :disabled="loading"
              class="w-full px-4 py-3 rounded-lg border border-outline-variant focus:outline-none focus:ring-2 text-body-md disabled:opacity-60"
              :class="fieldErrors.email ? 'border-error focus:border-error focus:ring-error/20' : 'focus:border-primary focus:ring-primary/20'"
              maxlength="100" autocomplete="email"
              @input="onEmail($event)"
              @blur="chequearDisponibilidad('email')"
            />
            <p v-if="fieldErrors.email" class="mt-1 text-label-sm text-error flex items-center gap-1">
              <span class="material-symbols-outlined text-[15px]" style="font-variation-settings:'FILL' 1">error</span>
              {{ fieldErrors.email }}
            </p>
          </div>

          <!-- Correo de recuperación -->
          <div>
            <label class="block text-label-lg text-on-surface mb-1">Correo de recuperación <span class="text-label-sm text-on-surface-variant font-normal">(opcional)</span></label>
            <input
              v-model="form.emailRecuperacion" type="email" placeholder="correo.recuperacion@ejemplo.com" :disabled="loading"
              class="w-full px-4 py-3 rounded-lg border border-outline-variant focus:outline-none focus:border-primary focus:ring-2 focus:ring-primary/20 text-body-md disabled:opacity-60"
              maxlength="100" autocomplete="email"
              @input="form.emailRecuperacion = form.emailRecuperacion.trim()"
            />
            <p class="mt-1 text-label-sm text-on-surface-variant">Si la olvidas, el código de recuperación irá a este correo. Si lo dejas vacío, irá al correo principal.</p>
          </div>

          <!-- Contraseña -->
          <div>
            <label class="block text-label-lg text-on-surface mb-1">Contraseña *</label>
            <div class="relative">
              <input
                v-model="form.password"
                :type="mostrarPassword ? 'text' : 'password'"
                required
                placeholder="••••••••"
                minlength="8"
                :disabled="loading"
                class="w-full px-4 py-3 pr-12 rounded-lg border border-outline-variant focus:outline-none focus:ring-2 focus:ring-primary/20 text-body-md disabled:opacity-60"
                :class="form.password && !esValida ? 'border-error focus:border-error' : 'focus:border-primary'"
              />
              <button
                type="button"
                tabindex="-1"
                class="absolute right-3 top-1/2 -translate-y-1/2 text-on-surface-variant hover:text-primary transition-colors"
                :aria-label="mostrarPassword ? 'Ocultar contraseña' : 'Mostrar contraseña'"
                @click="mostrarPassword = !mostrarPassword"
              >
                <span class="material-symbols-outlined text-[22px]">{{ mostrarPassword ? 'visibility_off' : 'visibility' }}</span>
              </button>
            </div>

            <!-- Indicador de fortaleza -->
            <template v-if="form.password">
              <div class="flex gap-1 mt-2" aria-hidden="true">
                <div
                  v-for="i in 5"
                  :key="i"
                  class="h-1.5 flex-1 rounded-full transition-colors duration-300"
                  :class="i <= fortaleza.nivel ? fortaleza.color : 'bg-surface-container-highest'"
                ></div>
              </div>
              <p class="text-label-sm mt-1 font-medium" :class="fortaleza.textoClase">
                Fortaleza: {{ fortaleza.texto }}
              </p>

              <!-- Requisitos -->
              <ul class="mt-2 grid grid-cols-1 sm:grid-cols-2 gap-x-4 gap-y-1">
                <li
                  v-for="r in reglas"
                  :key="r.key"
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

          <!-- Confirmar contraseña -->
          <div>
            <label class="block text-label-lg text-on-surface mb-1">Confirmar contraseña *</label>
            <div class="relative">
              <input
                v-model="confirmarPassword"
                :type="mostrarPassword ? 'text' : 'password'"
                required
                placeholder="••••••••"
                :disabled="loading"
                class="w-full px-4 py-3 pr-12 rounded-lg border border-outline-variant focus:outline-none focus:ring-2 focus:ring-primary/20 text-body-md disabled:opacity-60"
                :class="confirmarPassword && !coinciden ? 'border-error focus:border-error' : (confirmarPassword && coinciden ? 'border-success focus:border-success' : 'focus:border-primary')"
              />
              <span
                v-if="confirmarPassword && coinciden"
                class="absolute right-3 top-1/2 -translate-y-1/2 material-symbols-outlined text-[22px] text-success"
                style="font-variation-settings:'FILL' 1"
              >check_circle</span>
            </div>
            <p v-if="confirmarPassword && !coinciden" class="mt-1 text-label-sm text-error flex items-center gap-1">
              <span class="material-symbols-outlined text-[15px]" style="font-variation-settings:'FILL' 1">error</span>
              Las contraseñas no coinciden
            </p>
          </div>

          <!-- Términos -->
          <label class="flex items-start gap-3 cursor-pointer">
            <input v-model="form.terms" type="checkbox" required class="w-5 h-5 mt-0.5 rounded border-outline-variant text-primary" />
            <span class="text-body-sm text-on-surface-variant">
              Acepto los <a href="#" class="text-primary font-semibold hover:underline">términos y condiciones</a>
            </span>
          </label>

          <!-- Botón -->
          <AppButton type="submit" size="lg" class="w-full" :loading="loading" icon="arrow_forward">
            {{ loading ? 'Registrando...' : 'Crear cuenta' }}
          </AppButton>
        </form>

        <!-- Enlace inferior (se mantiene intacto) -->
        <p class="mt-6 text-center text-body-sm text-on-surface-variant">
          ¿Ya tienes cuenta?
          <RouterLink to="/login" class="text-primary font-semibold hover:underline ml-1">Iniciar sesión</RouterLink>
        </p>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, reactive, computed, toRef } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../../stores/auth.js'
import { usePasswordStrength } from '../../composables/usePasswordStrength.js'
import authAPI from '../../services/authAPI.js'
import AppButton from '../../components/ui/AppButton.vue'

const auth   = useAuthStore()
const router = useRouter()

const loading  = ref(false)
const errorMsg = ref('')

const fieldErrors = reactive({ email: '', numeroDocumento: '' })

const form = reactive({
  primerNombre: '', segundoNombre: '',
  primerApellido: '', segundoApellido: '',
  tipoDocumento: '', numeroDocumento: '',
  telefono: '', email: '', emailRecuperacion: '',
  password: '',
  terms: false,
})

const confirmarPassword = ref('')
const mostrarPassword   = ref(false)

const { reglas, fortaleza, esValida } = usePasswordStrength(toRef(form, 'password'))
const coinciden = computed(() =>
  form.password.length > 0 && form.password === confirmarPassword.value
)

// ── Sanitización de entradas ─────────────────────────────────────────────────

function limpiarNombre(e) {
  const input = e.target
  const limpio = input.value
    .replace(/[^A-Za-zÁÉÍÓÚÜÑáéíóúüñ\s]/g, '')
    .replace(/\s+/g, ' ')
    .slice(0, 30)
  input.value = limpio
  return limpio
}

function errorNombreCorto(valor) {
  const v = (valor || '').trim()
  if (!v) return ''
  if (v.length < 2) return 'Debe tener al menos 2 caracteres'
  return ''
}

function limpiarDocumento(e) {
  const input = e.target
  const limpio = input.value.toUpperCase().replace(/[^A-Z0-9]/g, '').slice(0, 18)
  input.value = limpio
  return limpio
}

function limpiarTelefono(e) {
  const input = e.target
  const limpio = input.value.replace(/[^0-9]/g, '').slice(0, 15)
  input.value = limpio
  return limpio
}

function onEmail(e) {
  form.email = e.target.value.trim()
  fieldErrors.email = ''
}

function onNumeroDocumento(e) {
  form.numeroDocumento = limpiarDocumento(e)
  fieldErrors.numeroDocumento = ''
}

const MENSAJE_DISPONIBILIDAD = {
  email: 'Este correo ya está registrado',
  numeroDocumento: 'Este número de documento ya está registrado',
}

async function chequearDisponibilidad(campo) {
  const valor = campo === 'email' ? form.email.trim() : form.numeroDocumento.trim()
  if (!valor || loading.value) {
    fieldErrors[campo] = ''
    return
  }
  try {
    const params = campo === 'email' ? { email: valor } : { numeroDocumento: valor }
    const { data } = await authAPI.verificarDisponibilidad(params)
    const disponible = campo === 'email' ? data.email : data.numeroDocumento
    fieldErrors[campo] = disponible ? '' : MENSAJE_DISPONIBILIDAD[campo]
  } catch {
    // No bloquear el formulario si el chequeo en vivo falla
  }
}

const errorDocumento = computed(() => {
  const d = form.numeroDocumento.trim()
  if (!d) return ''
  if (d.length < 5) return 'Debe tener al menos 5 caracteres'
  return ''
})

async function handleRegister() {
  if (!form.terms) { errorMsg.value = 'Debes aceptar los términos'; return }
  if (errorDocumento.value) {
    errorMsg.value = errorDocumento.value.replace('Debe', 'El número de documento debe')
    return
  }
  const nombreCorto = errorNombreCorto(form.primerNombre) || errorNombreCorto(form.primerApellido)
  if (nombreCorto) {
    errorMsg.value = `El nombre y el apellido son obligatorios: ${nombreCorto.toLowerCase()}`
    return
  }
  if (!esValida.value) {
    errorMsg.value = 'La contraseña no cumple los requisitos de seguridad marcados'
    return
  }
  if (!coinciden.value) {
    errorMsg.value = 'Las contraseñas no coinciden'
    return
  }

  errorMsg.value = ''
  loading.value  = true

  try {
    await auth.registro({
      primerNombre:    form.primerNombre,
      segundoNombre:   form.segundoNombre,
      primerApellido:  form.primerApellido,
      segundoApellido: form.segundoApellido,
      tipoDocumento:   form.tipoDocumento,
      numeroDocumento: form.numeroDocumento,
      telefono:        form.telefono,
      email:           form.email,
      emailRecuperacion: form.emailRecuperacion || null,
      password:        form.password,
    })

    router.push(auth.homeRoute)

  } catch (err) {
    errorMsg.value = err?.message || 'Error al crear la cuenta'
    if (err?.campo && ['email', 'numeroDocumento'].includes(err.campo)) {
      fieldErrors[err.campo] = errorMsg.value
    }
  } finally {
    loading.value = false
  }
}
</script>