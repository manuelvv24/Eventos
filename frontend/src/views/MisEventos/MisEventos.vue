<template>
  <div class="p-6 md:p-8 space-y-6 animate-fade-in">
    <header>
      <h1 class="text-headline-md text-on-surface font-semibold">Mis Eventos</h1>
      <p class="text-body-md text-on-surface-variant mt-1">
        Eventos en los que estás inscrito y tu código QR de acceso.
      </p>
    </header>

    <!-- Banners de Notificación -->
    <AppBanner v-model="exitoBanner" type="success" />
    <AppBanner v-model="errorBanner" type="error" />

    <div v-if="loading" class="flex flex-col items-center justify-center py-20 text-on-surface-variant">
      <span class="material-symbols-outlined text-[48px] text-primary animate-spin mb-2">progress_activity</span>
      <p class="text-body-md">Cargando tus eventos...</p>
    </div>

    <div v-else-if="error" class="rounded-xl bg-error-container/30 border border-error/20 text-error p-4 text-body-md">
      {{ error }}
    </div>

    <div v-else-if="eventos.length === 0" class="rounded-xl bg-surface-container-low border border-outline-variant/30 p-12 text-center">
      <span class="material-symbols-outlined text-[48px] text-on-surface-variant mb-2">event_busy</span>
      <p class="text-body-md text-on-surface-variant font-medium">Aún no estás inscrito en ningún evento activo.</p>
    </div>

    <div v-else class="grid gap-6 md:grid-cols-2 xl:grid-cols-3">
      <article
        v-for="ev in eventos"
        :key="ev.idRegistro"
        class="rounded-2xl bg-white border border-outline-variant/20 shadow-elevation-1 p-5 flex flex-col justify-between gap-4 hover:shadow-elevation-2 transition-all duration-300"
      >
        <div class="space-y-3">
          <div class="flex items-start justify-between gap-3">
            <div class="min-w-0">
              <h2 class="font-semibold text-title-md text-on-surface truncate">{{ ev.nombreEvento }}</h2>
              <p class="text-label-sm text-on-surface-variant mt-0.5">
                {{ ev.tipoEvento }} · {{ ev.modalidadEvento }}
              </p>
            </div>
            <span class="px-2.5 py-0.5 rounded-full text-label-sm font-semibold shrink-0" :class="estadoClasses(ev.estadoEvento)">
              {{ ev.estadoEvento }}
            </span>
          </div>

          <div class="text-label-md text-on-surface-variant space-y-1.5 pt-1">
            <p class="flex items-center gap-2">
              <span class="material-symbols-outlined text-primary text-[18px]">calendar_month</span>
              {{ formatDate(ev.fechaInicioEvento) }}
            </p>
            <p class="flex items-center gap-2">
              <span class="material-symbols-outlined text-primary text-[18px]">location_on</span>
              {{ ev.lugarEvento || 'Por definir' }}
            </p>
          </div>

          <div class="flex items-center justify-center bg-surface-container-lowest border border-outline-variant/20 rounded-xl p-4">
            <img
              v-if="ev.codigoQr"
              :src="qrSrc(ev.codigoQr)"
              alt="Código QR de acceso"
              class="w-40 h-40 object-contain"
            />
            <p v-else class="text-body-sm text-on-surface-variant">Sin código QR disponible</p>
          </div>
        </div>

        <div class="grid grid-cols-2 gap-2 pt-2 border-t border-outline-variant/20">
          <AppButton icon="download" @click="descargar(ev)">Descargar QR</AppButton>
          <AppButton variant="outlined" icon="event_busy" class="!text-error !border-error/30 hover:!bg-error-container/30" @click="solicitarCancelarReserva(ev)">
            Cancelar
          </AppButton>
        </div>
      </article>
    </div>

    <!-- Modal de confirmación para cancelar reserva -->
    <AppModal v-model="modalCancelar" title="¿Cancelar reserva?" size="sm">
      <div class="flex flex-col items-center text-center space-y-3 py-2">
        <div class="w-14 h-14 rounded-full bg-error-container text-error flex items-center justify-center shrink-0 shadow-sm">
          <span class="material-symbols-outlined text-[32px]">event_busy</span>
        </div>

        <div class="space-y-1">
          <p class="text-body-md text-on-surface leading-relaxed">
            ¿Estás seguro de que deseas cancelar tu inscripción al evento <strong class="text-on-surface font-semibold">"{{ eventoACancelar?.nombreEvento }}"</strong>?
          </p>
          <p class="text-label-sm text-amber-800 bg-amber-50 p-3 rounded-xl border border-amber-200 text-left mt-2 font-medium leading-relaxed">
            ⚠️ Tu código QR de ingreso quedará deshabilitado y tu cupo se liberará automáticamente en el aforo para otros participantes.
          </p>
        </div>
      </div>

      <template #footer>
        <AppButton variant="outlined" @click="modalCancelar = false">Mantener Reserva</AppButton>
        <AppButton variant="danger" :loading="cancelando" @click="confirmarCancelacion">
          Sí, Cancelar Reserva
        </AppButton>
      </template>
    </AppModal>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import asistenciaAPI from '../../services/asistenciaAPI.js'
import { API_BASE } from '../../services/api.js'
import AppModal      from '../../components/ui/AppModal.vue'
import AppButton     from '../../components/ui/AppButton.vue'
import AppBanner     from '../../components/ui/AppBanner.vue'

const eventos     = ref([])
const loading     = ref(true)
const error       = ref(null)
const exitoBanner = ref('')
const errorBanner = ref('')

const modalCancelar   = ref(false)
const cancelando      = ref(false)
const eventoACancelar = ref(null)

onMounted(async () => {
  try {
    const { data } = await asistenciaAPI.misEventos()
    eventos.value = data.filter(ev => String(ev.estadoAsistencia ?? '').toUpperCase() !== 'ASISTIO')
  } catch (e) {
    error.value =
      e.response?.data?.error || 'Ocurrió un error al cargar tus eventos. Intenta de nuevo.'
  } finally {
    loading.value = false
  }
})

/** URL del QR servido por el propio backend (sin exponer datos a terceros) */
function qrSrc(codigo) {
  if (!codigo) return ''
  return `${API_BASE}/public/qr/${encodeURIComponent(codigo)}`
}

async function descargar(ev) {
  try {
    const res  = await fetch(qrSrc(ev.codigoQr))
    if (!res.ok) throw new Error()
    const blob = await res.blob()
    const url  = URL.createObjectURL(blob)
    const a    = document.createElement('a')
    a.href     = url
    a.download = `QR-${ev.nombreEvento || ev.idRegistro}.png`
    a.click()
    URL.revokeObjectURL(url)
  } catch {
    errorBanner.value = 'No se pudo descargar la imagen del QR'
  }
}

function solicitarCancelarReserva(ev) {
  eventoACancelar.value = ev
  modalCancelar.value = true
}

async function confirmarCancelacion() {
  if (!eventoACancelar.value) return
  cancelando.value = true
  errorBanner.value = ''
  exitoBanner.value = ''
  try {
    await asistenciaAPI.cancelar(eventoACancelar.value.idRegistro)
    exitoBanner.value = `Tu inscripción a "${eventoACancelar.value.nombreEvento}" ha sido cancelada y el cupo ha sido liberado.`
    eventos.value = eventos.value.filter(e => e.idRegistro !== eventoACancelar.value.idRegistro)
    modalCancelar.value = false
  } catch (e) {
    errorBanner.value = e.response?.data?.error || 'No se pudo cancelar la reserva. Inténtalo de nuevo.'
  } finally {
    cancelando.value = false
  }
}

function formatDate(fecha) {
  if (!fecha) return 'Fecha por definir'
  return new Date(fecha).toLocaleDateString('es-CO', {
    day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit',
  })
}

function estadoClasses(estado) {
  const e = (estado || '').toUpperCase()
  if (e.includes('ACTIV')) return 'bg-green-100 text-green-700'
  if (e.includes('FINAL')) return 'bg-purple-100 text-purple-700'
  return 'bg-gray-100 text-gray-600'
}
</script>
