<template>
  <div class="p-6 md:p-8 space-y-6 animate-fade-in">
    <div>
      <h1 class="text-headline-md text-on-surface font-semibold">QR</h1>
      <p class="text-body-md text-on-surface-variant mt-1">Registra la asistencia de los participantes.</p>
    </div>

    <div class="grid grid-cols-1 lg:grid-cols-3 gap-6 items-start">
      <!-- Columna izquierda: QR y check-in masivo -->
      <div class="lg:col-span-1 space-y-6">
        <!-- QR Check-in con cámara -->
      <AppCard title="Check-in por QR" subtitle="Apunta la cámara al código QR del participante">
        <div class="space-y-3">
          <div v-if="!scannerActivo && !resultadoQr" class="flex flex-col items-center justify-center gap-3 py-6 rounded-xl border-2 border-dashed border-outline-variant bg-surface-container-low">
            <span class="material-symbols-outlined text-[48px] text-on-surface-variant" style="font-variation-settings:'FILL' 0">qr_code_scanner</span>
            <p class="text-body-md text-on-surface-variant">La cámara está apagada</p>
            <AppButton icon="videocam" :loading="iniciandoCamara" @click="iniciarScanner">
              Activar cámara
            </AppButton>
          </div>

          <div v-show="scannerActivo" class="space-y-3">
            <div id="qr-reader" class="rounded-xl overflow-hidden w-full" style="min-height:260px"></div>
            <AppButton variant="outlined" icon="videocam_off" class="w-full" @click="detenerScanner">
              Detener cámara
            </AppButton>
          </div>

          <div v-if="scannerActivo" class="flex items-center gap-2 mt-2">
            <div class="flex-1 h-px bg-outline-variant"></div>
            <span class="text-label-sm text-on-surface-variant px-2">o ingresa el código</span>
            <div class="flex-1 h-px bg-outline-variant"></div>
          </div>
          <form v-if="scannerActivo || codigoQr" class="flex gap-2" @submit.prevent="checkInQr">
            <AppInput
              v-model="codigoQr"
              placeholder="QR-ABCD1234"
              prefix-icon="qr_code"
              :error="qrError"
              class="flex-1"
            />
            <AppButton type="submit" :loading="loadingQr" icon="send">
              OK
            </AppButton>
          </form>
        </div>

        <Transition name="fade">
          <div
            v-if="resultadoQr" class="mt-5 p-4 rounded-xl border flex items-start gap-3"
            :class="resultadoQr.ok ? 'bg-success-container border-success/20' : 'bg-error-container border-error/20'"
          >
            <span class="material-symbols-outlined text-[32px] mt-0.5" :class="resultadoQr.ok ? 'text-success' : 'text-error'" style="font-variation-settings:'FILL' 1">
              {{ resultadoQr.ok ? 'check_circle' : 'cancel' }}
            </span>
            <div class="flex-1">
              <p class="text-label-lg font-semibold" :class="resultadoQr.ok ? 'text-success' : 'text-error'">
                {{ resultadoQr.ok ? 'Check-in exitoso' : 'Error en check-in' }}
              </p>
              <p class="text-body-sm text-on-surface-variant mt-0.5">{{ resultadoQr.mensaje }}</p>
              <AppButton variant="text" size="sm" icon="refresh" class="mt-2 -ml-2" @click="resetQr">
                Escanear otro
              </AppButton>
            </div>
          </div>
        </Transition>
      </AppCard>

<!-- Check-in Masivo -->
      <AppCard title="Check-in Masivo" subtitle="Marca asistencia a todos los inscritos pendientes de un evento">
        <div class="space-y-4">
          <AppSelect
            v-model="idEventoMasivo"
            label="Evento destino"
            prefix-icon="event"
            :options="eventosOpciones"
            :loading="cargandoEventos"
            placeholder="Seleccione un evento..."
            :error="masivoError"
          />

          <!-- Preview stats -->
          <Transition name="fade">
            <div v-if="previewMasivo" class="grid grid-cols-3 gap-2 p-3 rounded-lg bg-surface-container-low">
              <div class="text-center">
                <p class="text-title-lg font-bold text-on-surface">{{ previewMasivo.totalInscritos }}</p>
                <p class="text-label-sm text-on-surface-variant">Inscritos</p>
              </div>
              <div class="text-center">
                <p class="text-title-lg font-bold text-warning">{{ previewMasivo.pendientes }}</p>
                <p class="text-label-sm text-on-surface-variant">Pendientes</p>
              </div>
              <div class="text-center">
                <p class="text-title-lg font-bold text-success">{{ previewMasivo.yaAsistieron }}</p>
                <p class="text-label-sm text-on-surface-variant">Asistieron</p>
              </div>
            </div>
          </Transition>

          <!-- Botones -->
          <div class="flex gap-2">
            <AppButton
              variant="outlined"
              icon="visibility"
              :loading="loadingPreview"
              :disabled="!idEventoMasivo"
              class="flex-1"
              @click="previsualizarMasivo"
            >
              Previsualizar
            </AppButton>
            <AppButton
              icon="group_add"
              :loading="loadingMasivo"
              :disabled="!previewMasivo || previewMasivo.pendientes === 0"
              class="flex-1"
              @click="ejecutarMasivo"
            >
              Marcar todos
            </AppButton>
          </div>
        </div>

        <Transition name="fade">
          <div
            v-if="resultadoMasivo" class="mt-5 p-4 rounded-xl border flex items-center gap-3"
            :class="resultadoMasivo.ok ? 'bg-success-container border-success/20' : 'bg-error-container border-error/20'"
          >
            <span class="material-symbols-outlined text-[32px]" :class="resultadoMasivo.ok ? 'text-success' : 'text-error'" style="font-variation-settings:'FILL' 1">
              {{ resultadoMasivo.ok ? 'check_circle' : 'cancel' }}
            </span>
            <div>
              <p class="text-label-lg font-semibold" :class="resultadoMasivo.ok ? 'text-success' : 'text-error'">
                {{ resultadoMasivo.ok ? 'Check-in masivo completado' : 'Error' }}
              </p>
              <p class="text-body-sm text-on-surface-variant mt-0.5">{{ resultadoMasivo.mensaje }}</p>
            </div>
          </div>
        </Transition>
      </AppCard>
      </div>

      <!-- Columna derecha: check-in manual -->
      <div class="lg:col-span-2">
        <AppCard title="Check-in Manual" subtitle="Busca por identificación o ID de registro y elige el evento">
          <AppSelect
            :model-value="pestañaManual"
            label="Buscar por"
            :options="pestañaOpciones"
            class="mb-4"
            @update:model-value="cambiarPestaña"
          />

          <Transition name="panel-manual" mode="out-in">
            <!-- Pestaña: búsqueda por número de identificación -->
            <div v-if="pestañaManual === 'documento'" key="documento" class="space-y-4">
              <form class="space-y-3" @submit.prevent="buscarInscripciones">
                <div class="grid grid-cols-1 sm:grid-cols-[12rem_1fr] gap-3">
                  <AppSelect
                    v-model="tipoDocBusqueda"
                    label="Tipo doc."
                    :options="tiposDocOpciones"
                  />
                  <AppInput
                    v-model="docBusqueda"
                    label="Número de identificación"
                    prefix-icon="badge"
                    placeholder="Ej: 1023456789"
                    required
                    :error="docError"
                  />
                </div>
                <AppButton type="submit" icon="search" :loading="buscandoDoc" class="w-full">
                  Buscar inscripciones
                </AppButton>
              </form>

              <!-- Resultados -->
              <template v-if="inscripciones.length">
                <p class="text-label-md text-on-surface-variant">
                  {{ inscripciones.length }} {{ inscripciones.length === 1 ? 'inscripción' : 'inscripciones' }}
                  de <span class="font-semibold text-on-surface">{{ nombreParticipante || docBusqueda }}</span>
                </p>
                <AppSelect
                  v-model="metodo"
                  label="Método de verificación"
                  prefix-icon="how_to_reg"
                  :options="metodos"
                />
                <div class="grid gap-3 sm:grid-cols-2">
                  <div
                    v-for="insc in inscripciones"
                    :key="insc.idRegistroAsistencia"
                    class="flex flex-col justify-between gap-3 p-4 rounded-xl border border-outline-variant/20 bg-surface-container-low"
                  >
                    <div class="min-w-0">
                      <div class="flex items-start justify-between gap-2">
                        <p class="text-label-lg font-semibold text-on-surface">{{ insc.nombreEvento }}</p>
                        <AppBadge :variant="insc.yaAsistio ? 'success' : 'warning'">
                          {{ insc.yaAsistio ? 'Asistió' : 'Pendiente' }}
                        </AppBadge>
                      </div>
                      <p class="text-body-sm text-on-surface-variant mt-1 flex items-center gap-1">
                        <span class="material-symbols-outlined text-[15px]">calendar_today</span>
                        {{ formatFechaEvento(insc.fechaInicioEvento) }}
                      </p>
                    </div>
                    <div class="border-t border-outline-variant/10 pt-2 text-body-sm text-on-surface-variant space-y-0.5">
                      <p>{{ insc.nombreCompleto }}</p>
                      <p>{{ insc.tipoDocumento }} {{ insc.numeroDocumento }}</p>
                      <p class="text-label-sm">ID de registro: #{{ insc.idRegistroAsistencia }}</p>
                    </div>
                    <AppButton
                      size="sm"
                      icon="how_to_reg"
                      :disabled="insc.yaAsistio"
                      :loading="checkeandoId === insc.idRegistroAsistencia"
                      class="w-full"
                      @click="confirmarCheckIn(insc)"
                    >
                      {{ insc.yaAsistio ? 'Hecho' : 'Check-in' }}
                    </AppButton>
                  </div>
                </div>
              </template>
              <div
                v-else-if="buscadoDoc"
                class="p-4 rounded-xl bg-surface-container-low text-body-sm text-on-surface-variant flex items-center gap-2"
              >
                <span class="material-symbols-outlined text-[20px]">search_off</span>
                No se encontraron inscripciones activas para ese documento.
              </div>
            </div>

            <!-- Pestaña: búsqueda por ID de registro -->
            <form v-else key="id" class="space-y-4" @submit.prevent="checkInManual">
              <AppInput
                v-model="idRegistro"
                label="ID de Registro de Asistencia"
                prefix-icon="tag"
                type="number"
                placeholder="Ej: 42"
                required
                :error="manualError"
              />
              <AppSelect
                v-model="metodo"
                label="Método de verificación"
                prefix-icon="how_to_reg"
                :options="metodos"
              />
              <AppButton type="submit" :loading="loadingManual" icon="how_to_reg" class="w-full" size="lg">
                Marcar Asistencia
              </AppButton>
            </form>
          </Transition>

          <Transition name="fade">
            <div
              v-if="resultadoManual" class="mt-5 p-4 rounded-xl border flex items-center gap-3"
              :class="resultadoManual.ok ? 'bg-success-container border-success/20' : 'bg-error-container border-error/20'"
            >
              <span class="material-symbols-outlined text-[32px]" :class="resultadoManual.ok ? 'text-success' : 'text-error'" style="font-variation-settings:'FILL' 1">
                {{ resultadoManual.ok ? 'check_circle' : 'cancel' }}
              </span>
              <div>
                <p class="text-label-lg font-semibold" :class="resultadoManual.ok ? 'text-success' : 'text-error'">
                  {{ resultadoManual.ok ? 'Asistencia registrada' : 'Error' }}
                </p>
                <p class="text-body-sm text-on-surface-variant mt-0.5">{{ resultadoManual.mensaje }}</p>
              </div>
            </div>
          </Transition>
        </AppCard>
      </div>
    </div>

    <!-- Historial reciente -->
    <AppTable
      title="Check-ins Recientes"
      :columns="columnas"
      :rows="checkIns"
      :loading="loadingLista"
      row-key="idCheckIn"
      default-sort-key="idCheckIn"
      search-placeholder="Buscar por ID, ID registro, método o IP..."
      :search-fields="['idCheckIn','idRegistroAsistencia','metodoCheckIn','idUsuario','ipCheckIn','createdAt']"
    >
      <template #cell-metodoCheckIn="{ value }">
        <AppBadge :variant="value === 'QR' ? 'info' : value === 'MASIVO' ? 'warning' : 'neutral'">
          {{ value }}
        </AppBadge>
      </template>
      <template #cell-createdAt="{ value }">
        {{ formatFecha(value) }}
      </template>
    </AppTable>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { Html5Qrcode } from 'html5-qrcode'
import checkInAPI from '../../services/checkInAPI.js'
import eventosAPI from '../../services/eventoAPI.js'
import { useAuthStore } from '../../stores/auth.js'
import AppCard    from '../../components/ui/AppCard.vue'
import AppInput   from '../../components/ui/AppInput.vue'
import AppSelect  from '../../components/ui/AppSelect.vue'
import AppButton  from '../../components/ui/AppButton.vue'
import AppTable   from '../../components/ui/AppTable.vue'
import AppBadge   from '../../components/ui/AppBadge.vue'

const auth = useAuthStore()

// ── QR Scanner ───────────────────────────────────────────────────────────────
const codigoQr       = ref('')
const qrError        = ref('')
const loadingQr      = ref(false)
const resultadoQr    = ref(null)
const scannerActivo  = ref(false)
const iniciandoCamara= ref(false)

let html5QrCode = null

async function iniciarScanner() {
  iniciandoCamara.value = true
  try {
    html5QrCode = new Html5Qrcode('qr-reader')
    await html5QrCode.start(
      { facingMode: 'environment' },
      { fps: 10, qrbox: { width: 250, height: 250 } },
      (decodedText) => {
        codigoQr.value = decodedText
        detenerScanner()
        checkInQr()
      },
      () => { /* scan en progreso, ignorar errores de frame */ }
    )
    scannerActivo.value = true
  } catch (err) {
    console.error('[QR] Error al iniciar cámara:', err)
    qrError.value = 'No se pudo acceder a la cámara. Verifica los permisos del navegador.'
  } finally {
    iniciandoCamara.value = false
  }
}

async function detenerScanner() {
  if (html5QrCode) {
    try { await html5QrCode.stop() } catch { /* ignorar */ }
    html5QrCode = null
  }
  scannerActivo.value = false
}

function resetQr() {
  resultadoQr.value = null
  codigoQr.value = ''
  qrError.value = ''
}

async function checkInQr() {
  if (!codigoQr.value.trim()) return
  qrError.value = ''; resultadoQr.value = null; loadingQr.value = true
  try {
    await checkInAPI.checkInQr(codigoQr.value.trim())
    resultadoQr.value = { ok: true, mensaje: `Check-in registrado para QR: ${codigoQr.value}` }
    codigoQr.value = ''
    cargarLista()
  } catch (e) {
    resultadoQr.value = { ok: false, mensaje: e.response?.data?.error || 'Código QR inválido o ya registrado' }
  } finally { loadingQr.value = false }
}

// ── Check-in Manual ──────────────────────────────────────────────────────────
const pestañaManual = ref('documento')
const pestañaOpciones = [
  { value: 'documento', label: 'Por identificación' },
  { value: 'id',       label: 'Por ID de registro' },
]
const tiposDoc = ['CC', 'CE', 'TI', 'PA', 'NIT']
const tiposDocOpciones = [
  { value: '', label: 'Cualquier tipo' },
  ...tiposDoc.map(t => ({ value: t, label: t })),
]
const tipoDocBusqueda = ref('')
const docBusqueda     = ref('')
const docError        = ref('')
const buscandoDoc     = ref(false)
const buscadoDoc      = ref(false)
const inscripciones   = ref([])
const nombreParticipante = ref('')
const checkeandoId    = ref(null)

const idRegistro      = ref('')
const metodo          = ref('MANUAL')
const manualError     = ref('')
const loadingManual   = ref(false)
const resultadoManual = ref(null)

const metodos = [
  { value: 'MANUAL', label: 'Manual' },
  { value: 'PRESENCIAL', label: 'Presencial' },
]

function cambiarPestaña(p) {
  pestañaManual.value = p
  resultadoManual.value = null
}

async function buscarInscripciones() {
  docError.value = ''
  if (!docBusqueda.value.trim()) {
    docError.value = 'Ingresa el número de identificación'
    return
  }
  buscandoDoc.value = true
  buscadoDoc.value = false
  try {
    const res = await checkInAPI.buscarPorDocumento(docBusqueda.value.trim(), tipoDocBusqueda.value)
    inscripciones.value = res.data || []
    nombreParticipante.value = inscripciones.value[0]?.nombreCompleto || ''
    buscadoDoc.value = true
  } catch (e) {
    docError.value = e.response?.data?.error || 'Error al buscar'
  } finally {
    buscandoDoc.value = false
  }
}

async function confirmarCheckIn(insc) {
  resultadoManual.value = null
  checkeandoId.value = insc.idRegistroAsistencia
  try {
    await checkInAPI.checkInManual({
      idRegistroAsistencia: insc.idRegistroAsistencia,
      metodoCheckIn: metodo.value,
      idUsuario: auth.idUsuario,
    })
    insc.yaAsistio = true
    insc.estadoRegistro = 'ASISTIO'
    resultadoManual.value = {
      ok: true,
      mensaje: `Asistencia marcada para ${insc.nombreCompleto || 'el participante'} en «${insc.nombreEvento}»`,
    }
    cargarLista()
  } catch (e) {
    resultadoManual.value = { ok: false, mensaje: e.response?.data?.error || 'Error al registrar' }
  } finally {
    checkeandoId.value = null
  }
}

function formatFechaEvento(f) {
  if (!f) return 'Fecha por confirmar'
  return new Date(f).toLocaleDateString('es-ES', { day: '2-digit', month: 'short', year: 'numeric' })
}

async function checkInManual() {
  manualError.value = ''; resultadoManual.value = null; loadingManual.value = true
  try {
    await checkInAPI.checkInManual({
      idRegistroAsistencia: Number(idRegistro.value),
      metodoCheckIn: metodo.value,
      idUsuario: auth.idUsuario,
    })
    resultadoManual.value = { ok: true, mensaje: `Asistencia marcada para registro #${idRegistro.value}` }
    idRegistro.value = ''
    cargarLista()
  } catch (e) {
    resultadoManual.value = { ok: false, mensaje: e.response?.data?.error || 'Error al registrar' }
  } finally { loadingManual.value = false }
}

// ── Check-in Masivo ──────────────────────────────────────────────────────────
const idEventoMasivo  = ref('')
const eventosOpciones = ref([])
const cargandoEventos = ref(false)
const previewMasivo   = ref(null)
const loadingPreview  = ref(false)
const loadingMasivo   = ref(false)
const resultadoMasivo = ref(null)
const masivoError     = ref('')

async function cargarEventos() {
  cargandoEventos.value = true
  try {
    const res = await eventosAPI.getAll()
    eventosOpciones.value = (res.data || []).map(e => ({
      value: e.idEventos,
      label: e.nombreEvento
    }))
  } catch (err) {
    console.error('Error cargando eventos:', err)
  } finally {
    cargandoEventos.value = false
  }
}

async function previsualizarMasivo() {
  if (!idEventoMasivo.value) return
  masivoError.value = ''
  previewMasivo.value = null
  resultadoMasivo.value = null
  loadingPreview.value = true
  try {
    const res = await checkInAPI.previewMasivo(idEventoMasivo.value)
    previewMasivo.value = res.data
  } catch (e) {
    masivoError.value = e.response?.data?.error || 'Error al previsualizar'
  } finally {
    loadingPreview.value = false
  }
}

async function ejecutarMasivo() {
  if (!idEventoMasivo.value || !previewMasivo.value) return
  resultadoMasivo.value = null
  loadingMasivo.value = true
  try {
    const res = await checkInAPI.ejecutarMasivo({ idEvento: idEventoMasivo.value })
    resultadoMasivo.value = { ok: true, mensaje: res.data.mensaje || `${res.data.marcados} participantes marcados` }
    previewMasivo.value = null
    idEventoMasivo.value = ''
    cargarLista()
  } catch (e) {
    resultadoMasivo.value = { ok: false, mensaje: e.response?.data?.error || 'Error al ejecutar check-in masivo' }
  } finally {
    loadingMasivo.value = false
  }
}

// ── Lista reciente ───────────────────────────────────────────────────────────
const checkIns    = ref([])
const loadingLista= ref(false)

const columnas = [
  { key: 'idCheckIn',           label: '#' },
  { key: 'idRegistroAsistencia', label: 'ID Registro' },
  { key: 'metodoCheckIn',       label: 'Método' },
  { key: 'ipCheckIn',           label: 'IP' },
  { key: 'createdAt',           label: 'Fecha' },
]

async function cargarLista() {
  loadingLista.value = true
  try { checkIns.value = (await checkInAPI.getAll()).data }
  catch { checkIns.value = [] }
  finally { loadingLista.value = false }
}

function formatFecha(f) {
  return f ? new Date(f).toLocaleString('es-ES', { day:'2-digit', month:'short', hour:'2-digit', minute:'2-digit' }) : '—'
}

onMounted(() => {
  cargarLista()
  cargarEventos()
})
onUnmounted(detenerScanner)
</script>

<style scoped>
.fade-enter-active, .fade-leave-active { transition: opacity 0.3s, transform 0.3s; }
.fade-enter-from, .fade-leave-to { opacity: 0; transform: translateY(-4px); }
.panel-manual-enter-active, .panel-manual-leave-active { transition: opacity 0.2s, transform 0.2s; }
.panel-manual-enter-from { opacity: 0; transform: translateY(4px); }
.panel-manual-leave-to { opacity: 0; transform: translateY(-4px); }
</style>

<style>
#qr-reader {
  border: none !important;
}
#qr-reader video {
  border-radius: 0.75rem;
  width: 100% !important;
}
#qr-reader__scan_region {
  background: transparent !important;
}
#qr-reader__dashboard {
  display: none !important;
}
</style>