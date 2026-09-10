<template>
  <div class="p-6 md:p-8 space-y-6 animate-fade-in">
    <div>
      <h1 class="text-headline-md text-on-surface font-semibold">Importar Participantes</h1>
      <p class="text-body-md text-on-surface-variant mt-1">
        Sube un archivo CSV o Excel con los datos de los participantes para registrarlos masivamente.
      </p>
    </div>

    <!-- Step 1: Selección de archivo -->
    <AppCard title="Paso 1 — Seleccionar archivo y evento">
      <div class="space-y-4">
        <AppSelect
          v-model="idEvento"
          label="Evento destino"
          prefix-icon="event"
          :options="opcionesEventos"
          required
          :error="errEvento"
          @update:model-value="onEventoChange"
        />

        <!-- Banner de error global (evento finalizado, etc.) -->
        <div
          v-if="errGlobal"
          class="flex items-start gap-3 p-4 rounded-xl bg-error-container border border-error/20"
        >
          <span class="material-symbols-outlined text-error" style="font-variation-settings:'FILL' 1">error</span>
          <div>
            <p class="text-title-sm font-semibold text-error">No se pudo procesar la importación</p>
            <p class="text-body-sm text-on-error-container mt-0.5">{{ errGlobal }}</p>
          </div>
        </div>

        <!-- Bloqueo visible si el evento está finalizado o cancelado -->
        <div
          v-if="eventoFinalizado"
          class="flex items-start gap-3 p-4 rounded-xl bg-error-container text-on-error-container"
        >
          <span class="material-symbols-outlined" style="font-variation-settings:'FILL' 1">block</span>
          <p class="text-body-sm">
            El evento <strong>{{ eventoSeleccionado?.nombreEvento }}</strong> está
            {{ String(eventoSeleccionado?.estadoEvento ?? '').toUpperCase() }}:
            no se pueden agregar participantes mediante importación masiva.
          </p>
        </div>

        <div>
          <label class="text-label-lg text-on-surface font-medium block mb-2">
            Archivo <span class="text-error">*</span>
            <span class="text-label-sm text-on-surface-variant font-normal ml-1">(CSV, Excel .xlsx o .xls)</span>
          </label>
          <div
            class="border-2 border-dashed rounded-xl p-8 text-center cursor-pointer transition-all"
            :class="[
              archivo ? 'border-success bg-success-container/30' : 'border-outline-variant hover:border-primary hover:bg-primary/5',
              eventoFinalizado ? 'opacity-60 pointer-events-none' : ''
            ]"
            @click="$refs.fileInput.click()"
            @dragover.prevent
            @drop.prevent="onDrop"
          >
            <input ref="fileInput" type="file" accept=".csv,.xlsx,.xls" class="hidden" @change="onFileSelect" />
            <span class="material-symbols-outlined text-[48px] mb-2 block" :class="archivo ? 'text-success' : 'text-outline'">
              {{ archivo ? 'task_alt' : 'upload_file' }}
            </span>
            <p class="text-body-md text-on-surface font-medium">
              {{ archivo ? archivo.name : 'Arrastra o haz clic para seleccionar' }}
            </p>
            <p class="text-body-sm text-on-surface-variant mt-1">CSV · Excel (.xlsx, .xls) · máx. 10 MB</p>
          </div>
        </div>

        <!-- Formato esperado -->
        <div class="p-4 bg-surface-container rounded-xl">
          <p class="text-label-md text-on-surface-variant font-semibold mb-2">Formato esperado de columnas:</p>
          <code class="text-body-sm text-primary bg-primary-container px-2 py-1 rounded block overflow-x-auto">
            primer_nombre, segundo_nombre, primer_apellido, segundo_apellido, email, tipo_documento, numero_documento, telefono
          </code>
          <div class="flex gap-2 mt-3">
            <a
              href="/api/importacion/plantilla"
              download
              class="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg border border-outline-variant text-label-sm text-on-surface-variant hover:bg-surface-container-high transition-colors"
            >
              <span class="material-symbols-outlined text-[16px]">download</span>
              Descargar plantilla CSV
            </a>
            <a
              href="/api/importacion/plantilla-excel"
              download
              class="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg border border-outline-variant text-label-sm text-on-surface-variant hover:bg-surface-container-high transition-colors"
            >
              <span class="material-symbols-outlined text-[16px]">table_view</span>
              Descargar plantilla Excel
            </a>
          </div>
        </div>

        <div class="flex gap-3">
          <AppButton
            :loading="loadingPreview"
            :disabled="!archivo || !idEvento || eventoFinalizado"
            icon="preview"
            variant="outlined"
            @click="previsualizarCsv"
          >
            Previsualizar
          </AppButton>
          <AppButton
            v-if="preview"
            :loading="loadingConfirmar"
            :disabled="!preview?.filasExitosas || eventoFinalizado"
            icon="upload"
            @click="confirmarImportacion"
          >
            Confirmar importación ({{ preview?.filasExitosas ?? 0 }} registros)
          </AppButton>
        </div>
      </div>
    </AppCard>

    <!-- Step 2: Preview -->
    <template v-if="preview">
      <!-- Stats -->
      <div class="grid grid-cols-3 gap-4">
        <div class="bg-white rounded-xl p-4 border border-outline-variant/20 text-center">
          <p class="text-headline-sm font-bold text-on-surface">{{ preview.totalFilas }}</p>
          <p class="text-body-sm text-on-surface-variant">Total filas</p>
        </div>
        <div class="bg-success-container rounded-xl p-4 text-center">
          <p class="text-headline-sm font-bold text-success">{{ preview.filasExitosas }}</p>
          <p class="text-body-sm text-success">Filas válidas</p>
        </div>
        <div class="bg-error-container rounded-xl p-4 text-center">
          <p class="text-headline-sm font-bold text-error">{{ preview.filasError }}</p>
          <p class="text-body-sm text-error">Filas con error</p>
        </div>
      </div>

      <!-- Filas válidas -->
      <AppTable
        title="Filas válidas"
        :columns="columnasCsv"
        :rows="preview.preview ?? []"
        row-key="numeroFila"
        default-sort-key="numeroFila"
        :searchable="false"
        empty-text="No hay filas válidas"
        empty-icon="check_circle"
      />

      <!-- Errores -->
      <AppTable
        v-if="preview.errores?.length"
        title="Filas con error"
        subtitle="Corrígelas en el CSV y vuelve a subir"
        :columns="columnasError"
        :rows="preview.errores"
        row-key="numeroFila"
        default-sort-key="numeroFila"
        :searchable="false"
      >
        <template #cell-motivoError="{ value }">
          <span class="text-error text-body-sm">{{ value }}</span>
        </template>
      </AppTable>
    </template>

    <!-- Resultado final -->
    <div
      v-if="resultado" class="p-5 rounded-xl border flex items-start gap-4"
      :class="resultado.estadoImportacion?.includes('ERROR') ? 'bg-warning-container border-warning/20' : 'bg-success-container border-success/20'"
    >
      <span
        class="material-symbols-outlined text-[36px] mt-0.5" style="font-variation-settings:'FILL' 1"
        :class="resultado.estadoImportacion?.includes('ERROR') ? 'text-warning' : 'text-success'"
      >
        {{ resultado.estadoImportacion?.includes('ERROR') ? 'warning' : 'check_circle' }}
      </span>
      <div>
        <p class="text-title-sm font-semibold text-on-surface">{{ resultado.estadoImportacion }}</p>
        <p class="text-body-sm text-on-surface-variant mt-1">
          {{ resultado.filasExitosas }} registros importados · {{ resultado.filasError }} errores
        </p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import importacionAPI from '../../services/importacionAPI.js'
import eventoAPI      from '../../services/eventoAPI.js'
import AppCard   from '../../components/ui/AppCard.vue'
import AppSelect from '../../components/ui/AppSelect.vue'
import AppButton from '../../components/ui/AppButton.vue'
import AppTable  from '../../components/ui/AppTable.vue'
import { useConfirm } from '../../composables/useConfirm.js'

const { confirm } = useConfirm()

const eventos          = ref([])
const archivo          = ref(null)
const idEvento         = ref('')
const errEvento        = ref('')
const errGlobal        = ref('')
const loadingPreview   = ref(false)
const loadingConfirmar = ref(false)
const preview          = ref(null)
const resultado        = ref(null)

const estadoNoImportable = (estado) => {
  const e = String(estado ?? '').toUpperCase()
  return e === 'FINALIZADO' || e === 'CANCELADO' ? e : ''
}

const opcionesEventos = computed(() =>
  eventos.value.map(e => {
    const bad = estadoNoImportable(e.estadoEvento)
    return {
      value: String(e.idEventos),
      label: bad ? `${e.nombreEvento} (${bad.charAt(0) + bad.slice(1).toLowerCase()})` : e.nombreEvento,
      disabled: Boolean(bad),
    }
  })
)

const eventoSeleccionado = computed(() =>
  eventos.value.find(e => String(e.idEventos) === String(idEvento.value))
)

const eventoFinalizado = computed(() => Boolean(estadoNoImportable(eventoSeleccionado.value?.estadoEvento)))

const columnasCsv = [
  { key: 'numeroFila',    label: '#' },
  { key: 'primerNombre',  label: 'Nombre' },
  { key: 'primerApellido',label: 'Apellido' },
  { key: 'email',         label: 'Email' },
  { key: 'tipoDocumento', label: 'Tipo Doc.' },
  { key: 'numeroDocumento',label: 'Documento' },
]
const columnasError = [
  { key: 'numeroFila',  label: 'Fila' },
  { key: 'contenidoFila',label: 'Contenido' },
  { key: 'motivoError', label: 'Error' },
]

function onFileSelect(e) {
  archivo.value = e.target.files[0] ?? null
  preview.value = null
  resultado.value = null
  errGlobal.value = ''
}

function onDrop(e) {
  archivo.value = e.dataTransfer.files[0] ?? null
  preview.value = null
  resultado.value = null
  errGlobal.value = ''
}

function onEventoChange() {
  preview.value = null
  resultado.value = null
  errEvento.value = ''
  errGlobal.value = ''
}

async function previsualizarCsv() {
  if (!idEvento.value)       { errEvento.value = 'Selecciona un evento'; return }
  if (eventoFinalizado.value){ errEvento.value = 'El evento está finalizado o cancelado: no admite importaciones'; return }
  errEvento.value = ''
  errGlobal.value = ''
  if (!archivo.value) return

  loadingPreview.value = true
  preview.value = null
  try {
    preview.value = (await importacionAPI.preview(archivo.value, idEvento.value)).data
  } catch (e) {
    errGlobal.value = e.response?.data?.error || 'Error al previsualizar'
  } finally {
    loadingPreview.value = false
  }
}

async function confirmarImportacion() {
  if (eventoFinalizado.value || !preview.value?.filasExitosas) return
  const ok = await confirm({
    title: 'Confirmar importación',
    message: `¿Importar ${preview.value.filasExitosas} participantes al evento seleccionado?`,
    confirmText: 'Importar',
  })
  if (!ok) return

  loadingConfirmar.value = true
  resultado.value = null
  errGlobal.value = ''
  try {
    resultado.value = (await importacionAPI.confirmar(archivo.value, idEvento.value)).data
    archivo.value = null
    preview.value = null
  } catch (e) {
    errGlobal.value = e.response?.data?.error || 'Error al importar'
  } finally {
    loadingConfirmar.value = false
  }
}

onMounted(async () => {
  try {
    const { data } = await eventoAPI.getAll()
    eventos.value = data ?? []
  } catch {
    eventos.value = []
  }
})
</script>