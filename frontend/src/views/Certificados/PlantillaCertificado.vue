<template>
  <div class="p-6 md:p-8 space-y-6 animate-fade-in">
    <!-- Header -->
    <header class="flex flex-col md:flex-row md:items-center justify-between gap-4">
      <div>
        <h1 class="text-headline-md text-on-surface font-semibold">Diseño y Plantillas de Certificados</h1>
        <p class="text-body-md text-on-surface-variant mt-1">
          Crea, administra y selecciona qué plantilla se utilizará para expedir los certificados del sistema.
        </p>
      </div>

      <div class="flex items-center gap-3">
        <AppButton variant="outlined" icon="picture_as_pdf" :loading="descargandoPdf" @click="descargarPdfMuestra">
          Previsualizar PDF
        </AppButton>
        <AppButton icon="save" :loading="guardando" @click="guardarConfiguracion">
          Guardar Plantilla
        </AppButton>
      </div>
    </header>

    <!-- Selector de Plantilla Guardada -->
    <div class="bg-white rounded-2xl p-5 border border-outline-variant/30 shadow-elevation-1 flex flex-col md:flex-row items-start md:items-center justify-between gap-4">
      <div class="flex items-center gap-3 w-full md:w-auto">
        <span class="material-symbols-outlined text-primary text-[28px]">palette</span>
        <div class="flex-1 md:w-72">
          <label class="text-label-sm text-on-surface-variant font-semibold block mb-1">Plantilla Seleccionada</label>
          <select
            v-model="selectedId"
            class="w-full h-10 px-3 rounded-xl border border-outline-variant bg-surface-container-lowest text-body-md font-medium text-on-surface focus:outline-none focus:border-primary"
            @change="cargarPlantillaSeleccionada"
          >
            <option v-for="p in plantillas" :key="p.idConfiguracion" :value="p.idConfiguracion">
              {{ p.nombrePlantilla }} {{ p.esPredeterminada ? '★ (Predeterminada)' : '' }}
            </option>
          </select>
        </div>
      </div>

      <div class="flex items-center gap-2 w-full md:w-auto justify-end">
        <AppButton variant="outlined" size="sm" icon="add" @click="nuevaPlantilla">
          Nueva Plantilla
        </AppButton>
        <AppButton
          v-if="form.idConfiguracion && !form.esPredeterminada"
          variant="outlined"
          size="sm"
          icon="star"
          class="text-amber-700 border-amber-300 hover:bg-amber-50"
          @click="marcarComoPredeterminada"
        >
          Marcar Predeterminada
        </AppButton>
        <AppButton
          v-if="form.idConfiguracion && plantillas.length > 1"
          variant="danger"
          size="sm"
          icon="delete"
          @click="solicitarEliminar"
        >
          Eliminar
        </AppButton>
      </div>
    </div>

    <!-- Banners de notificación -->
    <AppBanner v-model="mensajeExito" type="success" />
    <AppBanner v-model="mensajeError" type="error" />

    <div v-if="cargando" class="flex flex-col items-center justify-center py-20 text-on-surface-variant">
      <span class="material-symbols-outlined text-[48px] text-primary animate-spin mb-2">progress_activity</span>
      <p class="text-body-md">Cargando plantillas de certificado...</p>
    </div>

    <div v-else class="grid grid-cols-1 lg:grid-cols-12 gap-8">
      <!-- Panel Izquierdo: Formularios de Edición (6 cols) -->
      <div class="lg:col-span-6 space-y-6">
        <!-- Nombre de Plantilla e Identidad -->
        <AppCard title="Identidad y Títulos">
          <div class="space-y-4">
            <AppInput
              v-model="form.nombrePlantilla"
              label="Nombre de la Plantilla"
              placeholder="Ej. Plantilla Institucional"
              required
            />
            <AppInput
              v-model="form.nombreOrganizacion"
              label="Nombre de la Organización / Institución"
              placeholder="Ej. UNIVERSIDAD EAN"
            />
            <AppInput
              v-model="form.tituloCertificado"
              label="Título del Certificado"
              placeholder="Ej. CERTIFICADO DE PARTICIPACIÓN"
            />

            <!-- Upload Logo -->
            <div>
              <label class="text-label-lg text-on-surface font-medium block mb-1">Logo de la Institución</label>
              <div class="flex items-center gap-4">
                <div class="w-20 h-20 rounded-xl border border-outline-variant/40 bg-surface-container-lowest flex items-center justify-center overflow-hidden p-2">
                  <img v-if="form.logoBase64" :src="form.logoBase64" alt="Logo" class="max-h-full max-w-full object-contain" />
                  <span v-else class="material-symbols-outlined text-on-surface-variant text-[32px]">image</span>
                </div>
                <div class="space-y-2">
                  <input ref="logoInput" type="file" accept="image/*" class="hidden" @change="onLogoSelect" />
                  <AppButton size="sm" variant="outlined" icon="upload" @click="$refs.logoInput.click()">
                    {{ form.logoBase64 ? 'Cambiar Logo' : 'Subir Logo' }}
                  </AppButton>
                  <AppButton v-if="form.logoBase64" size="sm" variant="text" class="text-error" @click="form.logoBase64 = ''">
                    Quitar
                  </AppButton>
                </div>
              </div>
            </div>
          </div>
        </AppCard>

        <!-- Colores y Paleta Estética -->
        <AppCard title="Paleta de Colores">
          <div class="grid grid-cols-2 gap-4">
            <div>
              <label class="text-label-sm text-on-surface-variant font-semibold block mb-1.5">Color de Borde Principal</label>
              <div class="flex items-center gap-3">
                <input v-model="form.colorBorde" type="color" class="w-10 h-10 rounded-lg cursor-pointer border border-outline-variant/40" />
                <span class="font-mono text-body-sm text-on-surface uppercase">{{ form.colorBorde }}</span>
              </div>
            </div>

            <div>
              <label class="text-label-sm text-on-surface-variant font-semibold block mb-1.5">Color Acento (Dorados/Esquinas)</label>
              <div class="flex items-center gap-3">
                <input v-model="form.colorAcento" type="color" class="w-10 h-10 rounded-lg cursor-pointer border border-outline-variant/40" />
                <span class="font-mono text-body-sm text-on-surface uppercase">{{ form.colorAcento }}</span>
              </div>
            </div>
          </div>
        </AppCard>

        <!-- Firmas electrónicas autorizadas -->
        <AppCard title="Firmas Autorizadas">
          <p class="text-body-sm text-on-surface-variant mb-4">
            Puedes agregar hasta 3 firmas escaneadas con su respectivo nombre y cargo.
          </p>

          <div class="space-y-6">
            <!-- Firma 1 -->
            <div class="p-4 rounded-xl border border-outline-variant/30 bg-surface-container-lowest space-y-3">
              <div class="flex items-center justify-between">
                <h3 class="text-title-sm font-semibold text-primary">Firma 1 (Principal)</h3>
              </div>
              <div class="grid grid-cols-2 gap-3">
                <AppInput v-model="form.firma1Nombre" label="Nombre del Firmante" placeholder="Ej. Ernesto" />
                <AppInput v-model="form.firma1Cargo" label="Cargo" placeholder="Ej. RECTOR" />
              </div>
              <div class="flex items-center gap-3 pt-1">
                <div class="w-28 h-12 rounded-lg border border-dashed border-outline-variant bg-white flex items-center justify-center p-1">
                  <img v-if="form.firma1ImagenBase64" :src="form.firma1ImagenBase64" class="max-h-full max-w-full object-contain" />
                  <span v-else class="text-label-sm text-outline">Sin Firma</span>
                </div>
                <input ref="firma1Input" type="file" accept="image/*" class="hidden" @change="e => onFirmaSelect(e, 1)" />
                <AppButton size="sm" variant="outlined" icon="draw" @click="$refs.firma1Input.click()">
                  {{ form.firma1ImagenBase64 ? 'Cambiar Imagen' : 'Subir Imagen Firma' }}
                </AppButton>
                <button v-if="form.firma1ImagenBase64" class="text-error text-label-sm hover:underline" @click="form.firma1ImagenBase64 = ''">Limpiar</button>
              </div>
            </div>

            <!-- Firma 2 -->
            <div class="p-4 rounded-xl border border-outline-variant/30 bg-surface-container-lowest space-y-3">
              <div class="flex items-center justify-between">
                <h3 class="text-title-sm font-semibold text-primary">Firma 2 (Opcional)</h3>
              </div>
              <div class="grid grid-cols-2 gap-3">
                <AppInput v-model="form.firma2Nombre" label="Nombre del Firmante" placeholder="Ej. Dra. Lucía" />
                <AppInput v-model="form.firma2Cargo" label="Cargo" placeholder="Ej. COORDINADORA ACADÉMICA" />
              </div>
              <div class="flex items-center gap-3 pt-1">
                <div class="w-28 h-12 rounded-lg border border-dashed border-outline-variant bg-white flex items-center justify-center p-1">
                  <img v-if="form.firma2ImagenBase64" :src="form.firma2ImagenBase64" class="max-h-full max-w-full object-contain" />
                  <span v-else class="text-label-sm text-outline">Sin Firma</span>
                </div>
                <input ref="firma2Input" type="file" accept="image/*" class="hidden" @change="e => onFirmaSelect(e, 2)" />
                <AppButton size="sm" variant="outlined" icon="draw" @click="$refs.firma2Input.click()">
                  {{ form.firma2ImagenBase64 ? 'Cambiar Imagen' : 'Subir Imagen Firma' }}
                </AppButton>
                <button v-if="form.firma2ImagenBase64" class="text-error text-label-sm hover:underline" @click="form.firma2ImagenBase64 = ''">Limpiar</button>
              </div>
            </div>

            <!-- Firma 3 -->
            <div class="p-4 rounded-xl border border-outline-variant/30 bg-surface-container-lowest space-y-3">
              <div class="flex items-center justify-between">
                <h3 class="text-title-sm font-semibold text-primary">Firma 3 (Opcional)</h3>
              </div>
              <div class="grid grid-cols-2 gap-3">
                <AppInput v-model="form.firma3Nombre" label="Nombre del Firmante" placeholder="Ej. Roberto Silva" />
                <AppInput v-model="form.firma3Cargo" label="Cargo" placeholder="Ej. SECRETARÍA GENERAL" />
              </div>
              <div class="flex items-center gap-3 pt-1">
                <div class="w-28 h-12 rounded-lg border border-dashed border-outline-variant bg-white flex items-center justify-center p-1">
                  <img v-if="form.firma3ImagenBase64" :src="form.firma3ImagenBase64" class="max-h-full max-w-full object-contain" />
                  <span v-else class="text-label-sm text-outline">Sin Firma</span>
                </div>
                <input ref="firma3Input" type="file" accept="image/*" class="hidden" @change="e => onFirmaSelect(e, 3)" />
                <AppButton size="sm" variant="outlined" icon="draw" @click="$refs.firma3Input.click()">
                  {{ form.firma3ImagenBase64 ? 'Cambiar Imagen' : 'Subir Imagen Firma' }}
                </AppButton>
                <button v-if="form.firma3ImagenBase64" class="text-error text-label-sm hover:underline" @click="form.firma3ImagenBase64 = ''">Limpiar</button>
              </div>
            </div>
          </div>
        </AppCard>
      </div>

      <!-- Panel Derecho: Vista Previa Estandarizada (6 cols) -->
      <div class="lg:col-span-6 space-y-4">
        <div class="sticky top-6">
          <div class="flex items-center justify-between mb-2">
            <h2 class="text-title-md font-semibold text-on-surface flex items-center gap-2">
              <span class="material-symbols-outlined text-primary">visibility</span>
              Vista Previa en Tiempo Real
            </h2>
            <span class="text-label-sm bg-primary-container text-primary px-2.5 py-1 rounded-full font-semibold">
              Elegante A4
            </span>
          </div>

          <!-- Marco de Certificado Interactivo Estilizado -->
          <div
            class="w-full bg-[#fbf9f8] rounded-2xl p-6 relative transition-all overflow-hidden flex flex-col justify-between"
            style="min-height: 420px; box-shadow: 0 10px 30px rgba(0,0,0,0.12);"
          >
            <!-- Patrón Guilloche sutil -->
            <div class="absolute inset-0 opacity-[0.03] pointer-events-none bg-[radial-gradient(#0b1a39_1px,transparent_1px)] [background-size:16px_16px]" />

            <!-- Borde exterior e interior -->
            <div class="absolute inset-3 border-2 pointer-events-none" :style="{ borderColor: form.colorBorde || '#0b1a39' }" />
            <div class="absolute inset-4 border pointer-events-none" :style="{ borderColor: form.colorAcento || '#7b5800' }" />

            <!-- Esquinas L Decorativas -->
            <div class="absolute top-2 left-2 w-5 h-5 border-t-2 border-l-2 pointer-events-none" :style="{ borderColor: form.colorBorde || '#0b1a39' }">
              <div class="absolute top-1 left-1 w-3 h-3 border-t border-l" :style="{ borderColor: form.colorAcento || '#7b5800' }" />
            </div>
            <div class="absolute top-2 right-2 w-5 h-5 border-t-2 border-r-2 pointer-events-none" :style="{ borderColor: form.colorBorde || '#0b1a39' }">
              <div class="absolute top-1 right-1 w-3 h-3 border-t border-r" :style="{ borderColor: form.colorAcento || '#7b5800' }" />
            </div>
            <div class="absolute bottom-2 left-2 w-5 h-5 border-b-2 border-l-2 pointer-events-none" :style="{ borderColor: form.colorBorde || '#0b1a39' }">
              <div class="absolute bottom-1 left-1 w-3 h-3 border-b border-l" :style="{ borderColor: form.colorAcento || '#7b5800' }" />
            </div>
            <div class="absolute bottom-2 right-2 w-5 h-5 border-b-2 border-r-2 pointer-events-none" :style="{ borderColor: form.colorBorde || '#0b1a39' }">
              <div class="absolute bottom-1 right-1 w-3 h-3 border-b border-r" :style="{ borderColor: form.colorAcento || '#7b5800' }" />
            </div>

            <!-- Contenido simulado -->
            <div class="text-center space-y-3 z-10 my-auto px-4">
              <!-- Logo -->
              <div v-if="form.logoBase64" class="flex justify-center mb-1">
                <img :src="form.logoBase64" class="h-10 object-contain" alt="Logo preview" />
              </div>

              <!-- Organización & Título -->
              <div>
                <p class="text-[11px] font-bold tracking-[0.2em] uppercase" :style="{ color: form.colorAcento || '#7b5800' }">
                  {{ form.nombreOrganizacion || 'UNIVERSIDAD EAN' }}
                </p>
                <h3 class="text-headline-sm font-serif font-bold tracking-wider uppercase mt-1" :style="{ color: form.colorBorde || '#0b1a39' }">
                  {{ form.tituloCertificado || 'CERTIFICADO DE PARTICIPACIÓN' }}
                </h3>
              </div>

              <!-- Destinatario con separador de diamante -->
              <div class="space-y-1">
                <p class="text-label-sm italic text-on-surface-variant">Se otorga con distinción el presente reconocimiento a:</p>
                <p class="text-title-lg font-serif font-bold tracking-wide" :style="{ color: form.colorBorde || '#0b1a39' }">
                  JUAN VILLAMIL
                </p>
                <div class="w-3/4 max-w-[280px] mx-auto h-[1px] bg-outline-variant relative my-2">
                  <div class="absolute left-1/2 top-1/2 -translate-x-1/2 -translate-y-1/2 w-2 h-2 rotate-45" :style="{ backgroundColor: form.colorAcento || '#7b5800' }" />
                </div>
                <p class="text-label-sm text-on-surface-variant">Documento de Identidad: 1000521258</p>
              </div>

              <!-- Texto cuerpo -->
              <p class="text-body-xs text-on-surface max-w-md mx-auto leading-relaxed">
                Por su destacada participación en <strong :style="{ color: form.colorBorde }">Evento recreo deportivo</strong>, con una duración de <strong :style="{ color: form.colorBorde }">6 horas</strong>, demostrando compromiso y excelencia académica.
              </p>
              <p class="text-[10px] text-on-surface-variant font-medium">Emitido el 13 de agosto de 2026</p>
            </div>

            <!-- Footer Firmas y QR -->
            <div class="grid grid-cols-12 gap-2 items-end pt-3 border-t border-outline-variant/30 z-10 px-2">
              <div class="col-span-3 text-left">
                <span class="text-[8px] font-bold block uppercase" :style="{ color: form.colorAcento || '#7b5800' }">Código de Verificación</span>
                <span class="font-mono text-[9px] font-bold bg-surface-container px-2 py-0.5 rounded border border-outline-variant block mt-0.5" :style="{ color: form.colorBorde || '#0b1a39' }">
                  CERT-4E566F1B
                </span>
              </div>

              <div class="col-span-7 flex justify-center items-end gap-3">
                <!-- Firma 1 -->
                <div class="text-center min-w-[70px]">
                  <img v-if="form.firma1ImagenBase64" :src="form.firma1ImagenBase64" class="h-6 object-contain mx-auto mb-0.5" />
                  <div class="h-0.5 w-full my-0.5" :style="{ backgroundColor: form.colorBorde || '#0b1a39' }" />
                  <p class="text-[9px] font-bold text-on-surface leading-none">{{ form.firma1Nombre || '' }}</p>
                  <p class="text-[8px] text-on-surface-variant uppercase leading-none mt-0.5">{{ form.firma1Cargo || 'RECTOR' }}</p>
                </div>

                <!-- Firma 2 -->
                <div v-if="form.firma2Cargo || form.firma2ImagenBase64" class="text-center min-w-[70px]">
                  <img v-if="form.firma2ImagenBase64" :src="form.firma2ImagenBase64" class="h-6 object-contain mx-auto mb-0.5" />
                  <div class="h-0.5 w-full my-0.5" :style="{ backgroundColor: form.colorBorde || '#0b1a39' }" />
                  <p class="text-[9px] font-bold text-on-surface leading-none">{{ form.firma2Nombre || '' }}</p>
                  <p class="text-[8px] text-on-surface-variant uppercase leading-none mt-0.5">{{ form.firma2Cargo }}</p>
                </div>

                <!-- Firma 3 -->
                <div v-if="form.firma3Cargo || form.firma3ImagenBase64" class="text-center min-w-[70px]">
                  <img v-if="form.firma3ImagenBase64" :src="form.firma3ImagenBase64" class="h-6 object-contain mx-auto mb-0.5" />
                  <div class="h-0.5 w-full my-0.5" :style="{ backgroundColor: form.colorBorde || '#0b1a39' }" />
                  <p class="text-[9px] font-bold text-on-surface leading-none">{{ form.firma3Nombre || '' }}</p>
                  <p class="text-[8px] text-on-surface-variant uppercase leading-none mt-0.5">{{ form.firma3Cargo }}</p>
                </div>
              </div>

              <div class="col-span-2 text-right">
                <div class="p-0.5 border border-secondary bg-white shadow-sm inline-block rounded">
                  <span class="material-symbols-outlined text-[20px] text-on-surface-variant block">qr_code_2</span>
                </div>
                <p class="text-[7px] font-semibold text-on-surface-variant mt-0.5" :style="{ color: form.colorAcento }">Escanear para validar</p>
              </div>
            </div>
          </div>

          <div class="flex justify-end mt-4">
            <AppButton variant="outlined" size="sm" icon="download" :loading="descargandoPdf" @click="descargarPdfMuestra">
              Descargar PDF de Ejemplo
            </AppButton>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import plantillaCertificadoAPI from '../../services/plantillaCertificadoAPI.js'
import AppCard   from '../../components/ui/AppCard.vue'
import AppInput  from '../../components/ui/AppInput.vue'
import AppButton from '../../components/ui/AppButton.vue'
import AppBanner from '../../components/ui/AppBanner.vue'
import { useConfirm } from '../../composables/useConfirm.js'

const { confirm } = useConfirm()
const plantillas     = ref([])
const selectedId     = ref(null)
const cargando       = ref(true)
const guardando      = ref(false)
const descargandoPdf = ref(false)
const mensajeExito   = ref('')
const mensajeError   = ref('')

const defaultForm = () => ({
  idConfiguracion: null,
  nombrePlantilla: 'Nueva Plantilla',
  esPredeterminada: false,
  nombreOrganizacion: 'UNIVERSIDAD EAN',
  tituloCertificado: 'CERTIFICADO DE PARTICIPACIÓN',
  textoCuerpo: '',
  colorBorde: '#0b1a39',
  colorAcento: '#7b5800',
  logoBase64: '',
  firma1Nombre: 'Ernesto',
  firma1Cargo: 'RECTOR',
  firma1ImagenBase64: '',
  firma2Nombre: 'Dra. Lucía',
  firma2Cargo: 'COORDINADORA ACADÉMICA',
  firma2ImagenBase64: '',
  firma3Nombre: '',
  firma3Cargo: '',
  firma3ImagenBase64: '',
})

const form = ref(defaultForm())

onMounted(async () => {
  await cargarPlantillas()
})

async function cargarPlantillas() {
  cargando.value = true
  try {
    const { data } = await plantillaCertificadoAPI.listarTodas()
    plantillas.value = data || []
    if (plantillas.value.length > 0) {
      const pred = plantillas.value.find(p => p.esPredeterminada) || plantillas.value[0]
      selectedId.value = pred.idConfiguracion
      form.value = { ...pred }
    }
  } catch {
    mensajeError.value = 'No se pudieron cargar las plantillas de certificado.'
  } finally {
    cargando.value = false
  }
}

function cargarPlantillaSeleccionada() {
  const p = plantillas.value.find(item => item.idConfiguracion === selectedId.value)
  if (p) {
    form.value = { ...p }
  }
}

function nuevaPlantilla() {
  form.value = { ...defaultForm(), nombrePlantilla: `Plantilla #${plantillas.value.length + 1}` }
  selectedId.value = null
  mensajeExito.value = 'Formulario listo para crear una nueva plantilla.'
}

async function marcarComoPredeterminada() {
  if (!form.value.idConfiguracion) return
  try {
    await plantillaCertificadoAPI.marcarPredeterminada(form.value.idConfiguracion)
    mensajeExito.value = `"${form.value.nombrePlantilla}" es ahora la plantilla predeterminada.`
    await cargarPlantillas()
  } catch {
    mensajeError.value = 'No se pudo marcar la plantilla como predeterminada.'
  }
}

async function solicitarEliminar() {
  if (!form.value.idConfiguracion) return
  const ok = await confirm({
    title: 'Eliminar plantilla',
    message: `¿Eliminar la plantilla "${form.value.nombrePlantilla}"? Esta acción no se puede deshacer.`,
    confirmText: 'Sí, eliminar',
    danger: true,
  })
  if (!ok) return
  try {
    await plantillaCertificadoAPI.eliminar(form.value.idConfiguracion)
    mensajeExito.value = 'Plantilla eliminada correctamente.'
    await cargarPlantillas()
  } catch {
    mensajeError.value = 'No se pudo eliminar la plantilla.'
  }
}

function fileToPngBase64(file) {
  return new Promise((resolve, reject) => {
    const url = URL.createObjectURL(file)
    const img = new Image()
    img.onload = () => {
      try {
        const canvas = document.createElement('canvas')
        canvas.width  = img.naturalWidth
        canvas.height = img.naturalHeight
        const ctx = canvas.getContext('2d')
        ctx.fillStyle = '#ffffff'
        ctx.fillRect(0, 0, canvas.width, canvas.height)
        ctx.drawImage(img, 0, 0)
        URL.revokeObjectURL(url)
        resolve(canvas.toDataURL('image/png'))
      } catch (e) {
        URL.revokeObjectURL(url)
        reject(e)
      }
    }
    img.onerror = err => { URL.revokeObjectURL(url); reject(err) }
    img.src = url
  })
}

async function onLogoSelect(e) {
  const file = e.target.files[0]
  if (!file) return
  try {
    form.value.logoBase64 = await fileToPngBase64(file)
  } catch {
    mensajeError.value = 'Error al procesar la imagen del logo.'
  }
}

async function onFirmaSelect(e, num) {
  const file = e.target.files[0]
  if (!file) return
  try {
    const b64 = await fileToPngBase64(file)
    if (num === 1) form.value.firma1ImagenBase64 = b64
    if (num === 2) form.value.firma2ImagenBase64 = b64
    if (num === 3) form.value.firma3ImagenBase64 = b64
  } catch {
    mensajeError.value = 'Error al procesar la imagen de la firma.'
  }
}

async function guardarConfiguracion() {
  guardando.value = true
  mensajeExito.value = ''
  mensajeError.value = ''
  try {
    let res
    if (form.value.idConfiguracion) {
      res = await plantillaCertificadoAPI.actualizar(form.value.idConfiguracion, form.value)
    } else {
      res = await plantillaCertificadoAPI.crear(form.value)
    }
    mensajeExito.value = '¡Plantilla guardada exitosamente!'
    await cargarPlantillas()
    if (res.data) {
      selectedId.value = res.data.idConfiguracion
      form.value = { ...res.data }
    }
  } catch (e) {
    mensajeError.value = e.response?.data?.error || 'Error al guardar la plantilla.'
  } finally {
    guardando.value = false
  }
}

async function descargarPdfMuestra() {
  descargandoPdf.value = true
  mensajeError.value = ''
  try {
    const response = await plantillaCertificadoAPI.previewPdf(form.value.idConfiguracion)
    const blob = new Blob([response.data], { type: 'application/pdf' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `preview-${form.value.nombrePlantilla || 'certificado'}.pdf`
    a.click()
    URL.revokeObjectURL(url)
  } catch {
    mensajeError.value = 'No se pudo generar el PDF de prueba.'
  } finally {
    descargandoPdf.value = false
  }
}
</script>

