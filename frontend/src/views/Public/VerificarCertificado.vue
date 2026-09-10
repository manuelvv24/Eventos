<template>
  <div class="min-h-screen bg-background flex items-center justify-center p-6 font-sans">
    <div class="w-full max-w-lg space-y-6">
      <!-- Logo -->
      <div class="text-center">
        <div class="w-14 h-14 bg-primary rounded-2xl flex items-center justify-center mx-auto mb-4">
          <span class="material-symbols-outlined text-on-primary text-[32px]" style="font-variation-settings:'FILL' 1">workspace_premium</span>
        </div>
        <h1 class="text-headline-md text-on-surface font-semibold">Verificación de Certificado</h1>
        <p class="text-body-md text-on-surface-variant mt-1">Ingresa el código único para verificar la autenticidad.</p>
      </div>

      <!-- Form -->
      <AppCard padding="normal">
        <form class="space-y-4" @submit.prevent="verificar">
          <AppInput
            v-model="codigo"
            label="Código de verificación"
            prefix-icon="qr_code"
            placeholder="Ej: QR-ABCD1234"
            required
            :error="errorForm"
          />
          <AppButton type="submit" :loading="loading" icon="verified" class="w-full" size="lg">
            Verificar certificado
          </AppButton>
        </form>
      </AppCard>

      <!-- Resultado válido -->
      <Transition name="slide">
        <AppCard v-if="cert" padding="normal" class="border-success/30 ring-1 ring-success/20">
          <div class="flex items-center gap-3 mb-5">
            <div class="w-12 h-12 rounded-full bg-success-container flex items-center justify-center">
              <span class="material-symbols-outlined text-success text-[28px]" style="font-variation-settings:'FILL' 1">verified</span>
            </div>
            <div>
              <p class="text-title-md font-bold text-success">Certificado Válido</p>
              <p class="text-body-sm text-on-surface-variant">Código: {{ cert.codigoVerificacion }}</p>
            </div>
          </div>

          <dl class="space-y-3">
            <div class="flex justify-between py-2 border-b border-outline-variant/20">
              <dt class="text-body-sm text-on-surface-variant">Participante</dt>
              <dd class="text-label-lg text-on-surface font-semibold">{{ cert.nombreParticipante }}</dd>
            </div>
            <div class="flex justify-between py-2 border-b border-outline-variant/20">
              <dt class="text-body-sm text-on-surface-variant">Evento / Curso</dt>
              <dd class="text-label-lg text-on-surface">{{ cert.nombreEvento }}</dd>
            </div>
            <div class="flex justify-between py-2 border-b border-outline-variant/20">
              <dt class="text-body-sm text-on-surface-variant">Duración</dt>
              <dd class="text-label-lg text-on-surface">{{ cert.duracionEvento }}</dd>
            </div>
            <div class="flex justify-between py-2 border-b border-outline-variant/20">
              <dt class="text-body-sm text-on-surface-variant">Tipo / Modalidad</dt>
              <dd class="text-label-lg text-on-surface">{{ cert.tipoEvento }} · {{ cert.modalidadEvento }}</dd>
            </div>
            <div class="flex justify-between py-2">
              <dt class="text-body-sm text-on-surface-variant">Fecha de emisión</dt>
              <dd class="text-label-lg text-on-surface">{{ formatFecha(cert.fechaEmisionCertificado) }}</dd>
            </div>
          </dl>

          <div class="mt-5 p-3 rounded-lg bg-success-container/50 flex items-center gap-2">
            <span class="material-symbols-outlined text-success text-[18px]">check_circle</span>
            <span class="text-body-sm text-success font-medium">Estado: {{ cert.estadoCertificado }}</span>
          </div>
        </AppCard>
      </Transition>

      <!-- No encontrado -->
      <Transition name="slide">
        <AppCard v-if="notFound" padding="normal" class="border-error/30 ring-1 ring-error/20">
          <div class="text-center py-4">
            <span class="material-symbols-outlined text-error text-[48px] block mb-2" style="font-variation-settings:'FILL' 1">cancel</span>
            <p class="text-title-sm font-semibold text-error mb-1">Certificado no encontrado</p>
            <p class="text-body-sm text-on-surface-variant">El código ingresado no corresponde a ningún certificado válido.</p>
          </div>
        </AppCard>
      </Transition>

      <p class="text-center text-body-sm text-on-surface-variant">
        <RouterLink to="/login" class="text-primary hover:underline">Ir al sistema</RouterLink>
      </p>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import certificadoAPI from '../../services/certificadoAPI.js'
import AppCard   from '../../components/ui/AppCard.vue'
import AppInput  from '../../components/ui/AppInput.vue'
import AppButton from '../../components/ui/AppButton.vue'

const route    = useRoute()
const codigo   = ref(route.query.codigo || route.params.codigo || '')
const loading  = ref(false)
const errorForm= ref('')
const cert     = ref(null)
const notFound = ref(false)

async function verificar() {
  if (!codigo.value.trim()) { errorForm.value = 'El código es obligatorio'; return }
  errorForm.value = ''; cert.value = null; notFound.value = false; loading.value = true
  try {
    const { data } = await certificadoAPI.verificar(codigo.value.trim())
    cert.value = data
  } catch (e) {
    if (e.response?.status === 404) notFound.value = true
    else errorForm.value = e.response?.data?.error || 'Error al verificar'
  } finally { loading.value = false }
}

function formatFecha(f) {
  return f ? new Date(f).toLocaleDateString('es-ES', { day:'2-digit', month:'long', year:'numeric' }) : '—'
}

// Si vino con código en la URL, verificar automáticamente
onMounted(() => { if (codigo.value) verificar() })
</script>

<style scoped>
.slide-enter-active, .slide-leave-active { transition: opacity 0.3s, transform 0.3s; }
.slide-enter-from, .slide-leave-to { opacity: 0; transform: translateY(8px); }
</style>
