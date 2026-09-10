<template>
  <div class="p-6 md:p-8 space-y-6 animate-fade-in">
    <div>
      <h1 class="text-headline-md text-on-surface font-semibold">Mis Certificados</h1>
      <p class="text-body-md text-on-surface-variant mt-1">Descarga los certificados de los eventos a los que has asistido.</p>
    </div>

    <!-- Loading -->
    <div v-if="loading" class="flex flex-col items-center justify-center py-24 gap-4 text-on-surface-variant">
      <span class="material-symbols-outlined text-[48px] text-primary animate-spin">progress_activity</span>
      <p class="text-body-md">Cargando certificados...</p>
    </div>

    <!-- Error -->
    <AppBanner v-model="error" type="error" />

    <!-- Sin certificados -->
    <div v-if="!loading && !error && certificados.length === 0" class="flex flex-col items-center justify-center py-24 gap-4 text-on-surface-variant">
      <span class="material-symbols-outlined text-[64px] text-outline-variant">workspace_premium</span>
      <p class="text-body-lg font-medium text-on-surface">Aún no tienes certificados</p>
      <p class="text-body-md text-on-surface-variant text-center max-w-md">
        Los certificados se generan cuando asistes a un evento y el organizador los emite. ¡Sigue participando!
      </p>
    </div>

    <!-- Grid de certificados -->
    <div v-if="!loading && certificados.length > 0" class="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-5">
      <div
        v-for="cert in certificados"
        :key="cert.idCertificado"
        class="bg-white rounded-2xl border border-outline-variant/20 shadow-elevation-1 overflow-hidden hover:shadow-elevation-2 transition-all duration-300 group flex flex-col justify-between"
      >
        <div>
          <!-- Header con gradiente -->
          <div class="relative bg-gradient-to-br from-primary via-primary/90 to-primary/70 px-5 py-4 text-on-primary">
            <div class="flex items-start justify-between">
              <div class="flex-1 min-w-0">
                <p class="text-title-sm font-bold truncate">{{ cert.nombreEvento }}</p>
                <div class="flex items-center gap-2 mt-1">
                  <span class="text-label-sm opacity-80">{{ cert.tipoEvento }}</span>
                  <span class="w-1 h-1 rounded-full bg-on-primary/40"></span>
                  <span class="text-label-sm opacity-80">{{ cert.modalidadEvento }}</span>
                </div>
              </div>
              <span class="material-symbols-outlined text-[32px] opacity-30 group-hover:opacity-50 transition-opacity" style="font-variation-settings:'FILL' 1">workspace_premium</span>
            </div>
          </div>

          <!-- Body -->
          <div class="p-5 space-y-3">
            <div class="flex items-center gap-2 text-body-sm text-on-surface-variant">
              <span class="material-symbols-outlined text-[16px] text-primary">calendar_today</span>
              <span>Emitido: {{ formatFecha(cert.fechaEmisionCertificado) }}</span>
            </div>

            <div v-if="cert.fechaInicioEvento" class="flex items-center gap-2 text-body-sm text-on-surface-variant">
              <span class="material-symbols-outlined text-[16px] text-primary">event</span>
              <span>Evento: {{ formatFecha(cert.fechaInicioEvento) }}</span>
            </div>

            <div v-if="cert.duracionEvento" class="flex items-center gap-2 text-body-sm text-on-surface-variant">
              <span class="material-symbols-outlined text-[16px] text-primary">schedule</span>
              <span>Duración: {{ cert.duracionEvento }}</span>
            </div>

            <div class="flex items-center gap-2">
              <span
                class="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-label-sm font-semibold"
                :class="String(cert.estadoCertificado ?? '').toUpperCase() === 'REVOCADO'
                  ? 'bg-error-container text-error'
                  : 'bg-success-container text-success'"
              >
                <span class="w-1.5 h-1.5 rounded-full" :class="String(cert.estadoCertificado ?? '').toUpperCase() === 'REVOCADO' ? 'bg-error' : 'bg-success'"></span>
                {{ cert.estadoCertificado || 'EMITIDO' }}
              </span>
            </div>

            <!-- Selector de plantilla eliminado: el usuario final solo descarga el PDF emitido -->

            <div class="pt-2 border-t border-outline-variant/20">
              <p class="text-label-sm text-on-surface-variant font-mono">{{ cert.codigoVerificacion }}</p>
            </div>
          </div>
        </div>

        <!-- Footer con acciones -->
        <div class="px-5 pb-4">
          <AppButton
            icon="download"
            class="w-full"
            :loading="descargando === cert.idCertificado"
            @click="descargar(cert)"
          >
            {{ descargando === cert.idCertificado ? 'Descargando...' : 'Descargar PDF' }}
          </AppButton>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import certificadoAPI        from '../../services/certificadoAPI.js'
import AppButton             from '../../components/ui/AppButton.vue'
import AppBanner             from '../../components/ui/AppBanner.vue'

const certificados = ref([])
const loading      = ref(true)
const error        = ref('')
const descargando  = ref(null)

function formatFecha(f) {
  if (!f) return '—'
  return new Date(f).toLocaleDateString('es-ES', {
    day: '2-digit', month: 'long', year: 'numeric',
  })
}

async function descargar(cert) {
  descargando.value = cert.idCertificado
  try {
    const resp = await certificadoAPI.descargarPorId(cert.idCertificado)
    const url = URL.createObjectURL(new Blob([resp.data], { type: 'application/pdf' }))
    const a = document.createElement('a')
    a.href = url
    a.download = `certificado-${cert.codigoVerificacion || cert.idCertificado}.pdf`
    a.click()
    URL.revokeObjectURL(url)
  } catch (e) {
    error.value = e.response?.data?.error || 'Error al descargar el certificado'
  } finally {
    descargando.value = null
  }
}

onMounted(async () => {
  try {
    const res = await certificadoAPI.misCertificados()
    certificados.value = res.data || []
  } catch (e) {
    error.value = e.response?.data?.error || 'Error al cargar tus certificados'
  } finally {
    loading.value = false
  }
})
</script>

