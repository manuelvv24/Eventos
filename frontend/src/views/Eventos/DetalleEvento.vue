<template>
  <div class="p-6 md:p-8 animate-fade-in">
    <!-- Loading -->
    <div v-if="cargando" class="flex flex-col items-center justify-center py-24 gap-4 text-on-surface-variant">
      <span class="material-symbols-outlined text-[48px] text-primary animate-spin">progress_activity</span>
      <p class="text-body-md">Cargando evento...</p>
    </div>

    <!-- Evento -->
    <div v-else-if="evento" class="max-w-5xl mx-auto space-y-6">
      <!-- Banners de Notificación -->
      <AppBanner v-model="errorBanner" type="error" />
      <AppBanner v-model="exitoBanner" type="success" />

      <!-- Back + acciones -->
      <div class="flex items-center justify-between">
        <AppButton variant="ghost" icon="arrow_back" class="-ml-2 text-on-surface-variant" @click="router.back()">
          Volver
        </AppButton>
        <div class="flex gap-2">
          <AppButton
            v-if="canEdit"
            variant="outlined"
            icon="edit"
            :to="`/app/eventos/${evento.idEventos}/editar`"
          >
            Editar
          </AppButton>
          <AppButton
            v-if="canInhabilitar"
            variant="outlined"
            icon="block"
            class="!text-error !border-error/30 hover:!bg-error-container/30"
            @click="inhabilitar"
          >
            Inhabilitar
          </AppButton>
        </div>
      </div>

      <!-- Imagen de portada -->
      <div class="relative w-full h-64 md:h-80 rounded-2xl overflow-hidden bg-surface-container">
        <img
          v-if="evento.imagenUrl"
          :src="evento.imagenUrl"
          :alt="evento.nombreEvento"
          class="w-full h-full object-cover"
        />
        <div v-else class="w-full h-full flex items-center justify-center">
          <span class="material-symbols-outlined text-[80px] text-outline-variant">event</span>
        </div>
        <div class="absolute top-4 left-4 flex gap-2">
          <span class="bg-primary text-on-primary px-3 py-1 rounded-lg text-label-md font-semibold shadow">
            {{ evento.modalidadEvento }}
          </span>
          <span class="bg-white/90 text-on-surface px-3 py-1 rounded-lg text-label-md font-semibold shadow">
            {{ evento.tipoEvento || 'General' }}
          </span>
        </div>
      </div>

      <!-- Cuerpo -->
      <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <!-- Info principal -->
        <div class="lg:col-span-2 space-y-5">
          <div>
            <h1 class="text-headline-lg text-on-surface font-semibold mb-3">{{ evento.nombreEvento }}</h1>
            <div class="flex flex-wrap gap-3">
              <span class="flex items-center gap-1 bg-surface-container px-3 py-1 rounded-full text-label-md text-on-surface-variant">
                <span class="material-symbols-outlined text-primary text-[16px]">calendar_today</span>
                {{ formatearFecha(evento.fechaInicioEvento) }}
              </span>
              <span v-if="evento.lugarEvento" class="flex items-center gap-1 bg-surface-container px-3 py-1 rounded-full text-label-md text-on-surface-variant">
                <span class="material-symbols-outlined text-primary text-[16px]">location_on</span>
                {{ evento.lugarEvento }}
              </span>
              <span v-if="evento.duracionEvento" class="flex items-center gap-1 bg-surface-container px-3 py-1 rounded-full text-label-md text-on-surface-variant">
                <span class="material-symbols-outlined text-primary text-[16px]">schedule</span>
                {{ evento.duracionEvento }}h
              </span>
            </div>
          </div>

          <div class="bg-white rounded-xl border border-outline-variant/20 p-5">
            <h2 class="text-title-md font-semibold text-on-surface mb-3 flex items-center gap-2">
              <span class="material-symbols-outlined text-primary text-[20px]">description</span>
              Descripción
            </h2>
            <p class="text-body-md text-on-surface-variant leading-relaxed whitespace-pre-wrap">
              {{ evento.descripcionEvento || 'Sin descripción disponible.' }}
            </p>
          </div>
        </div>

        <!-- Sidebar de acción -->
        <div class="lg:col-span-1">
          <div class="bg-white rounded-xl border border-outline-variant/20 p-5 sticky top-6 space-y-4">
            <div class="flex justify-between text-body-sm">
              <span class="text-on-surface-variant">Estado</span>
              <span class="font-semibold" :class="String(evento.estadoEvento ?? '').toUpperCase() === 'ACTIVO' ? 'text-success' : 'text-on-surface'">
                {{ evento.estadoEvento }}
              </span>
            </div>
            <div class="flex justify-between text-body-sm">
              <span class="text-on-surface-variant">Aforo</span>
              <span class="font-semibold text-on-surface">
                {{ evento.aforoMaximoEvento ? `${evento.aforoMaximoEvento} personas` : 'Sin límite' }}
              </span>
            </div>

            <!-- Cupos disponibles -->
            <div v-if="cupos" class="space-y-2">
              <div class="flex justify-between text-body-sm">
                <span class="text-on-surface-variant">Registrados</span>
                <span class="font-semibold text-on-surface">{{ cupos.registrados }}</span>
              </div>
              <div class="flex justify-between text-body-sm">
                <span class="text-on-surface-variant">Disponibles</span>
                <span class="font-semibold" :class="cupos.disponibles === 0 ? 'text-error' : 'text-success'">
                  {{ cupos.sinLimite ? 'Sin límite' : cupos.disponibles }}
                </span>
              </div>
              <!-- Barra de progreso del aforo -->
              <div v-if="!cupos.sinLimite && cupos.aforoMaximo > 0" class="space-y-1">
                <div class="h-2 rounded-full bg-surface-container-high overflow-hidden">
                  <div
                    class="h-full rounded-full transition-all duration-500"
                    :class="pctAforo >= 100 ? 'bg-error' : pctAforo >= 80 ? 'bg-warning' : 'bg-success'"
                    :style="{ width: pctAforo + '%' }"
                  />
                </div>
                <p class="text-label-sm text-on-surface-variant text-right">{{ pctAforo }}% ocupado</p>
              </div>
            </div>

            <div class="flex justify-between text-body-sm">
              <span class="text-on-surface-variant">Finaliza</span>
              <span class="font-semibold text-on-surface">{{ formatearFecha(evento.fechaFinEvento) }}</span>
            </div>

            <hr class="border-outline-variant/30" />

            <!-- Resultado del registro -->
            <Transition name="fade">
              <div
                v-if="registroResultado" class="p-4 rounded-xl border"
                :class="registroResultado.ok
                  ? 'bg-success-container border-success/20'
                  : 'bg-error-container border-error/20'"
              >
                <div class="flex items-start gap-2">
                  <span
                    class="material-symbols-outlined text-[18px] mt-0.5" style="font-variation-settings:'FILL' 1"
                    :class="registroResultado.ok ? 'text-success' : 'text-error'"
                  >
                    {{ registroResultado.ok ? 'check_circle' : 'error' }}
                  </span>
                  <div class="flex-1">
                    <p class="font-semibold text-body-sm" :class="registroResultado.ok ? 'text-success' : 'text-error'">
                      {{ registroResultado.ok ? '¡Registro exitoso!' : 'No se pudo registrar' }}
                    </p>
                    <p class="text-body-sm text-on-surface-variant mt-0.5">{{ registroResultado.mensaje }}</p>
                  </div>
                </div>

                <!-- Imagen QR -->
                <div v-if="registroResultado.ok && registroResultado.codigoQr" class="mt-4 flex flex-col items-center gap-2">
                  <img
                    :src="`${API_BASE}/public/qr/${registroResultado.codigoQr}`"
                    :alt="`QR ${registroResultado.codigoQr}`"
                    class="w-44 h-44 rounded-xl border border-outline-variant/30 bg-white p-2"
                  />
                  <p class="text-label-sm text-on-surface-variant font-mono">{{ registroResultado.codigoQr }}</p>
                  <a
                    :href="`${API_BASE}/public/qr/${registroResultado.codigoQr}`"
                    :download="`QR-${registroResultado.codigoQr}.png`"
                    class="flex items-center gap-1 text-label-sm text-primary hover:underline"
                  >
                    <span class="material-symbols-outlined text-[16px]">download</span>
                    Descargar QR
                  </a>
                </div>
              </div>
            </Transition>

            <AppButton
              v-if="!registroResultado?.ok && puedeRegistrarse"
              size="lg"
              icon="confirmation_number"
              class="w-full"
              :disabled="eventoLleno || !eventoRegistrable"
              :loading="registrando"
              @click="registrarse"
            >
              {{ eventoLleno ? 'Sin cupos disponibles' : !eventoRegistrable ? 'Evento no disponible' : 'Registrarse' }}
            </AppButton>

            <p v-if="!registroResultado?.ok && auth.isAuthenticated && !puedeRegistrarse" class="text-label-sm text-center text-on-surface-variant py-2">
              Los roles de organización no participan como asistentes de los eventos.
            </p>

            <p v-if="!registroResultado?.ok && puedeRegistrarse && !eventoLleno && eventoRegistrable" class="text-label-sm text-center text-on-surface-variant">
              Recibirás un código QR de confirmación.
            </p>
          </div>
        </div>
      </div>

      <!-- Regeneración masiva de certificados (solo admin/superadmin) -->
      <AppCard
        v-if="canRegenerarCertificados"
        title="Regenerar certificados del evento"
        subtitle="Reemplaza el PDF de todos los certificados emitidos de este evento por el de la plantilla seleccionada. El código de verificación de cada uno se mantiene."
      >
        <div class="flex flex-col sm:flex-row gap-3 items-end">
          <AppSelect
            v-model="plantillaRegenMasiva"
            label="Plantilla"
            placeholder="Predeterminada del Sistema"
            prefix-icon="palette"
            :options="opcionesPlantillasMasiva"
            class="flex-1"
          />
          <AppButton
            icon="refresh"
            :loading="regenerandoMasiva"
            :disabled="regenerandoMasiva"
            @click="regenerarMasiva"
          >
            Regenerar certificados
          </AppButton>
        </div>
      </AppCard>
    </div>

    <!-- Error -->
    <div v-else class="flex flex-col items-center justify-center py-24 gap-4 text-on-surface-variant">
      <span class="material-symbols-outlined text-[56px] text-error">error_outline</span>
      <p class="text-body-lg font-medium text-on-surface">Evento no encontrado</p>
      <RouterLink to="/" class="text-primary hover:underline text-label-lg">Volver al inicio</RouterLink>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../../stores/auth.js'
import { ROLES } from '../../constants/index.js'
import eventoAPI    from '../../services/eventoAPI.js'
import asistenciaAPI from '../../services/asistenciaAPI.js'
import certificadoAPI from '../../services/certificadoAPI.js'
import plantillaCertificadoAPI from '../../services/plantillaCertificadoAPI.js'
import { API_BASE } from '../../services/api.js'
import AppButton from '../../components/ui/AppButton.vue'
import AppBanner from '../../components/ui/AppBanner.vue'
import AppCard   from '../../components/ui/AppCard.vue'
import AppSelect from '../../components/ui/AppSelect.vue'
import { useConfirm } from '../../composables/useConfirm.js'

const route  = useRoute()
const router = useRouter()
const auth   = useAuthStore()
const { confirm } = useConfirm()

const cargando        = ref(true)
const evento          = ref(null)
const cupos           = ref(null)
const registrando     = ref(false)
const registroResultado = ref(null)

// Regeneración masiva de certificados (solo admin/superadmin)
const plantillasMasiva   = ref([])
const plantillaRegenMasiva = ref('')
const regenerandoMasiva  = ref(false)

const canRegenerarCertificados = computed(() => auth.hasRole(ROLES.ADMIN) && !!evento.value)
const opcionesPlantillasMasiva = computed(() =>
  plantillasMasiva.value.map(p => ({ value: p.idConfiguracion, label: p.nombrePlantilla }))
)
/** Solo el rol Invitado puede inscribirse como asistente. Los visitantes sin sesión ven el botón (redirige a login). */
const puedeRegistrarse = computed(() => !auth.isAuthenticated || auth.rol === ROLES.INVITADO)

const errorBanner = ref('')
const exitoBanner = ref('')

const isFinalizadoOManualmenteVencido = computed(() => {
  if (!evento.value) return false
  const estado = evento.value.estadoEvento?.toUpperCase() ?? ''
  if (estado === 'FINALIZADO') return true
  const fechaTermino = evento.value.fechaFinEvento || evento.value.fechaInicioEvento
  return fechaTermino && new Date(fechaTermino) < new Date()
})

const canInhabilitar = computed(() => {
  if (!auth.hasRole(ROLES.ADMIN, ROLES.OPERADOR)) return false
  if (!evento.value) return false
  const estado = evento.value.estadoEvento?.toUpperCase() ?? ''
  return estado !== 'CANCELADO' && estado !== 'FINALIZADO'
})

async function inhabilitar() {
  errorBanner.value = ''
  exitoBanner.value = ''
  const ok = await confirm({
    title: 'Inhabilitar evento',
    message: `¿Estás seguro de inhabilitar / cancelar el evento "${evento.value.nombreEvento}"? Los participantes inscritos serán notificados.`,
    confirmText: 'Inhabilitar',
    danger: true,
  })
  if (!ok) return
  try {
    await eventoAPI.cancel(evento.value.idEventos)
    evento.value.estadoEvento = 'CANCELADO'
    exitoBanner.value = `El evento "${evento.value.nombreEvento}" ha sido inhabilitado correctamente.`
  } catch (e) {
    errorBanner.value = e.response?.data?.error || 'Error al inhabilitar el evento. Inténtalo de nuevo.'
  }
}

const canEdit = computed(() => {
  if (!auth.hasRole(ROLES.ADMIN, ROLES.OPERADOR)) return false
  if (!evento.value) return false
  const estado = evento.value.estadoEvento?.toUpperCase() ?? ''
  if (estado === 'FINALIZADO' || estado === 'CANCELADO') return false
  if (isFinalizadoOManualmenteVencido.value) return false
  return true
})
const eventoLleno = computed(() =>
  cupos.value && !cupos.value.sinLimite && cupos.value.disponibles === 0
)
const eventoRegistrable = computed(() => {
  if (isFinalizadoOManualmenteVencido.value) return false
  const estado = evento.value?.estadoEvento?.toUpperCase() ?? ''
  return !['FINALIZADO', 'CANCELADO', 'BORRADOR'].includes(estado)
})
const pctAforo = computed(() => {
  if (!cupos.value || cupos.value.sinLimite || cupos.value.aforoMaximo === 0) return 0
  return Math.round((cupos.value.registrados / cupos.value.aforoMaximo) * 100)
})

function formatearFecha(f) {
  if (!f) return '—'
  return new Date(f).toLocaleDateString('es-ES', {
    weekday: 'long', day: 'numeric', month: 'long', year: 'numeric',
  })
}

async function registrarse() {
  if (!auth.isAuthenticated) {
    router.push({ name: 'Login', query: { redirect: route.fullPath } })
    return
  }

  registrando.value = true
  registroResultado.value = null

  try {
    // Obtener (o crear) el participante del usuario actual
    const { data: participante } = await asistenciaAPI.miParticipante()

    const { data } = await asistenciaAPI.registrar({
      idEventos: evento.value.idEventos,
      idParticipantes: participante.idParticipantes,
    })

    registroResultado.value = {
      ok: true,
      mensaje: 'Tu registro fue exitoso. Guarda tu código QR.',
      codigoQr: data.codigoQrInscripcion,
    }

    // Refrescar cupos
    const { data: cuposData } = await asistenciaAPI.cupos(evento.value.idEventos)
    cupos.value = cuposData

  } catch (e) {
    registroResultado.value = {
      ok: false,
      mensaje: e.response?.data?.error || 'Error al procesar el registro.',
    }
  } finally {
    registrando.value = false
  }
}

async function regenerarMasiva() {
  if (!evento.value) return
  errorBanner.value = ''
  exitoBanner.value = ''
  const ok = await confirm({
    title: 'Regenerar certificados',
    message: `¿Regenerar todos los certificados de "${evento.value.nombreEvento}" con la plantilla seleccionada? El código de verificación de cada uno se mantiene.`,
    confirmText: 'Regenerar',
    danger: false,
  })
  if (!ok) return
  regenerandoMasiva.value = true
  try {
    const { data } = await certificadoAPI.regenerarPorEvento(
      evento.value.idEventos,
      plantillaRegenMasiva.value || null
    )
    exitoBanner.value = `Regenerados ${data.regenerados} de ${data.total} certificado(s) del evento.` +
      (data.omitidos ? ` Se omitieron ${data.omitidos}.` : '')
  } catch (e) {
    errorBanner.value = e.response?.data?.error || 'Error al regenerar los certificados del evento.'
  } finally {
    regenerandoMasiva.value = false
  }
}

onMounted(async () => {
  try {
    const [eventoResp, cuposResp] = await Promise.all([
      eventoAPI.getById(route.params.id),
      asistenciaAPI.cupos(route.params.id),
    ])
    evento.value = eventoResp.data
    cupos.value  = cuposResp.data
  } catch {
    evento.value = null
  } finally {
    cargando.value = false
  }
  if (auth.hasRole(ROLES.ADMIN)) {
    try {
      plantillasMasiva.value = (await plantillaCertificadoAPI.listarTodas()).data || []
    } catch {
      plantillasMasiva.value = []
    }
  }
})
</script>

<style scoped>
.fade-enter-active, .fade-leave-active { transition: opacity 0.3s, transform 0.3s; }
.fade-enter-from, .fade-leave-to { opacity: 0; transform: translateY(-4px); }
</style>
