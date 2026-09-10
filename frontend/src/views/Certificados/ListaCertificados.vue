<template>
  <div class="p-6 md:p-8 space-y-6 animate-fade-in">
    <div>
      <h1 class="text-headline-md text-on-surface font-semibold">Certificados</h1>
      <p class="text-body-md text-on-surface-variant mt-1">Genera y descarga certificados de asistencia para los participantes.</p>
    </div>

    <AppCard title="Generar Certificado" subtitle="Ingresa el ID del registro de asistencia">
      <form class="flex flex-col sm:flex-row gap-3 items-end" @submit.prevent="generar">
        <AppInput
          v-model="idRegistro"
          label="ID de Registro de Asistencia"
          type="number"
          prefix-icon="how_to_reg"
          placeholder="Ej: 15"
          required
          class="flex-1"
        />
        <AppButton type="submit" :loading="loadingGen" icon="workspace_premium" size="md">Generar</AppButton>
      </form>

      <!-- Error de generación -->
      <Transition name="fade">
        <div v-if="errorGen" class="mt-4 p-4 rounded-xl bg-error-container border border-error/20 flex items-start gap-3">
          <span class="material-symbols-outlined text-error text-[24px] mt-0.5" style="font-variation-settings:'FILL' 1">error</span>
          <div>
            <p class="text-label-lg font-semibold text-error">No se pudo generar el certificado</p>
            <p class="text-body-sm text-on-surface-variant mt-0.5">{{ errorGen }}</p>
          </div>
        </div>
      </Transition>

      <Transition name="fade">
        <div v-if="generado" class="mt-5 p-4 rounded-xl bg-success-container border border-success/20 flex items-center justify-between gap-4">
          <div class="flex items-center gap-3">
            <span class="material-symbols-outlined text-success text-[32px]" style="font-variation-settings:'FILL' 1">workspace_premium</span>
            <div>
              <p class="text-label-lg font-semibold text-on-surface">Certificado generado</p>
              <p class="text-body-sm text-on-surface-variant">Código: {{ generado.codigoVerificacion }}</p>
            </div>
          </div>
          <AppButton variant="outlined" icon="download" size="sm" @click="descargar(generado.idRegistroAsistencia)">
            Descargar PDF
          </AppButton>
        </div>
      </Transition>
    </AppCard>

    <!-- Asistencias con opción de generar -->
    <AppBanner v-model="errorAccion" type="error" />

    <!-- Regeneración de certificados (solo admin/superadmin) -->
    <AppCard
      v-if="auth.hasRole(ROLES.ADMIN)"
      title="Regenerar certificados con otra plantilla"
      subtitle="Selecciona la plantilla y pulsa el botón de regenerar en la fila del participante. El código de verificación no cambia."
    >
      <div class="flex flex-col sm:flex-row gap-2 items-end">
        <AppSelect
          v-model="plantillaRegen"
          label="Plantilla"
          placeholder="Predeterminada del Sistema"
          prefix-icon="palette"
          :options="opcionesPlantillas"
          class="flex-1"
        />
      </div>
    </AppCard>
    <AppBanner v-if="auth.hasRole(ROLES.ADMIN)" v-model="exitoRegen" type="success" />

    <AppTable
      title="Registros de Asistencia"
      subtitle="Genera certificados para los participantes que asistieron"
      :columns="columnas"
      :rows="filasFiltradas"
      :loading="loadingLista"
      :error="errorLista"
      row-key="idRegistroAsistencia"
      default-sort-key="idRegistroAsistencia"
      search-placeholder="Buscar por ID registro, ID participante, QR..."
      :search-fields="['idRegistroAsistencia','idParticipantes','idEventos','codigoQrInscripcion','estadoAsistencia']"
      @retry="cargarAsistencias"
    >
      <template #filters>
        <AppSelect v-model="filtroEstado" :options="opcionesEstado" placeholder="Estado" class="w-32" />
      </template>
      <template #cell-idRegistroAsistencia="{ row }">
        <span class="font-mono text-label-md bg-primary-container/40 text-on-primary-container px-2 py-0.5 rounded font-bold">
          #{{ row.idRegistroAsistencia }}
        </span>
      </template>
      <template #cell-idParticipantes="{ row }">
        <span class="font-mono text-label-md bg-surface-container-high text-on-surface px-2 py-0.5 rounded font-medium">
          #{{ row.idParticipantes }}
        </span>
      </template>
      <template #cell-estadoAsistencia="{ row }">
        <AppBadge :variant="esAsistio(row) ? 'success' : 'neutral'" :dot="true">{{ row.estadoAsistencia }}</AppBadge>
      </template>
      <template #cell-dias="{ row }">
        <span v-if="row.diasRequeridos > 0" class="inline-flex items-center gap-1">
          <AppBadge :variant="cumple(row) ? 'success' : 'warning'">{{ row.diasAsistidas }}/{{ row.diasRequeridos }} días</AppBadge>
        </span>
        <span v-else class="text-on-surface-variant text-label-sm">—</span>
      </template>
      <template #cell-fechaAsistencia="{ value }">
        {{ formatFecha(value) }}
      </template>
      <template #actions="{ row }">
        <AppButton
          v-if="puedeGenerar(row)"
          size="sm"
          icon="workspace_premium"
          title="Generar certificado"
          @click="generarParaRegistro(row.idRegistroAsistencia)"
        >
          Certificado
        </AppButton>
        <ButtonIcon
          v-else-if="esAsistio(row)"
          icon="hourglass_empty"
          :title="`Requiere ${row.diasRequeridos} día(s) de check-in; solo tiene ${row.diasAsistidas}`"
          disabled
        />
        <ButtonIcon
          v-if="row.tieneCertificado"
          icon="download"
          title="Descargar PDF"
          @click="descargar(row.idRegistroAsistencia)"
        />
        <ButtonIcon
          v-if="row.tieneCertificado && auth.hasRole(ROLES.ADMIN)"
          :icon="regenerando === row.idRegistroAsistencia ? 'progress_activity' : 'refresh'"
          :class="regenerando === row.idRegistroAsistencia ? 'animate-spin' : ''"
          :disabled="regenerando === row.idRegistroAsistencia"
          title="Regenerar PDF con la plantilla seleccionada"
          @click="regenerarPDF(row.idRegistroAsistencia)"
        />
      </template>
    </AppTable>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useAuthStore } from '../../stores/auth.js'
import { ROLES }        from '../../constants/index.js'
import certificadoAPI from '../../services/certificadoAPI.js'
import dashboardAPI   from '../../services/dashboardAPI.js'
import plantillaCertificadoAPI from '../../services/plantillaCertificadoAPI.js'
import AppCard    from '../../components/ui/AppCard.vue'
import AppInput   from '../../components/ui/AppInput.vue'
import AppButton  from '../../components/ui/AppButton.vue'
import AppSelect  from '../../components/ui/AppSelect.vue'
import AppTable   from '../../components/ui/AppTable.vue'
import AppBadge   from '../../components/ui/AppBadge.vue'
import ButtonIcon from '../../components/ui/ButtonIcon.vue'
import AppBanner  from '../../components/ui/AppBanner.vue'

const auth = useAuthStore()

const idRegistro  = ref('')
const loadingGen  = ref(false)
const generado    = ref(null)
const errorGen    = ref(null)
const asistencias = ref([])
const loadingLista= ref(false)
const errorAccion = ref('')
const errorLista  = ref(null)

const filtroEstado   = ref('')
const opcionesEstado = [
  { value: '', label: 'Todos' },
  { value: 'ASISTIO',   label: 'Asistió' },
  { value: 'REGISTRADO', label: 'Registrado' },
  { value: 'ELIMINADO', label: 'Eliminado' },
]

const filasFiltradas = computed(() => {
  const estado = filtroEstado.value.toUpperCase()
  if (!estado) return asistencias.value
  return asistencias.value.filter(a => String(a.estadoAsistencia ?? '').toUpperCase() === estado)
})

// Regeneración con plantilla (solo admin/superadmin)
const plantillas       = ref([])
const plantillaRegen   = ref('')
const regenerando      = ref(null)
const exitoRegen       = ref('')
const opcionesPlantillas = computed(() =>
  plantillas.value.map(p => ({ value: p.idConfiguracion, label: p.nombrePlantilla }))
)

const columnas = [
  { key: 'idRegistroAsistencia', label: 'ID Registro' },
  { key: 'idParticipantes',        label: 'ID Participante' },
  { key: 'estadoAsistencia',     label: 'Estado' },
  { key: 'dias',                 label: 'Días check-in' },
  { key: 'codigoQrInscripcion',  label: 'Código QR' },
  { key: 'fechaAsistencia',      label: 'Fecha' },
]

function esAsistio(row) {
  return String(row?.estadoAsistencia ?? '').trim().toUpperCase() === 'ASISTIO'
}

function cumple(row) {
  return !row.diasRequeridos || (row.diasAsistidas ?? 0) >= row.diasRequeridos
}

function puedeGenerar(row) {
  return esAsistio(row) && cumple(row)
}

async function generar() {
  loadingGen.value = true; generado.value = null; errorGen.value = null
  try { generado.value = (await certificadoAPI.generar(idRegistro.value)).data }
  catch (e) { errorGen.value = e.response?.data?.error || 'Error al generar el certificado' }
  finally { loadingGen.value = false }
}

async function generarParaRegistro(id) {
  errorGen.value = null; generado.value = null; errorAccion.value = ''
  try { generado.value = (await certificadoAPI.generar(id)).data }
  catch (e) {
    errorAccion.value = e.response?.data?.error || 'Error al generar el certificado'
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }
}

async function descargar(idReg) {
  errorAccion.value = ''
  try {
    const resp = await certificadoAPI.descargar(idReg)
    const url  = URL.createObjectURL(new Blob([resp.data], { type: 'application/pdf' }))
    const a    = document.createElement('a')
    a.href = url; a.download = `certificado-${idReg}.pdf`; a.click()
    URL.revokeObjectURL(url)
  } catch (e) {
    errorAccion.value = e.response?.data?.error || 'Error al descargar el certificado'
  }
}

async function regenerarPDF(idReg) {
  errorAccion.value = ''
  exitoRegen.value = ''
  regenerando.value = idReg
  try {
    const { data } = await certificadoAPI.regenerar(idReg, plantillaRegen.value || null)
    exitoRegen.value = `Certificado ${data.codigoVerificacion} regenerado con la plantilla seleccionada.`
    window.scrollTo({ top: 0, behavior: 'smooth' })
  } catch (e) {
    errorAccion.value = e.response?.data?.error || 'Error al regenerar el certificado'
  } finally {
    regenerando.value = null
  }
}

async function cargarAsistencias() {
  loadingLista.value = true; errorLista.value = null
  try { asistencias.value = (await dashboardAPI.asistencias()).data }
  catch (e) { errorLista.value = e.response?.data?.error || 'Error al cargar asistencias' }
  finally { loadingLista.value = false }
}

async function cargarPlantillas() {
  try {
    plantillas.value = (await plantillaCertificadoAPI.listarTodas()).data || []
  } catch {
    plantillas.value = []
  }
}

function formatFecha(f) {
  return f ? new Date(f).toLocaleDateString('es-ES', { day:'2-digit', month:'short', year:'numeric' }) : '—'
}

onMounted(() => {
  cargarAsistencias()
  if (auth.hasRole(ROLES.ADMIN)) cargarPlantillas()
})
</script>

<style scoped>
.fade-enter-active, .fade-leave-active { transition: opacity 0.3s; }
.fade-enter-from, .fade-leave-to { opacity: 0; }
</style>
