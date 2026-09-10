<template>
  <div class="p-6 md:p-8 animate-fade-in">
    <div class="flex flex-col sm:flex-row justify-between items-start sm:items-center mb-8 gap-4">
      <div>
        <h1 class="text-headline-md text-on-surface font-semibold">
          {{ esEdicion ? 'Editar Evento' : 'Crear Evento' }}
        </h1>
        <p class="text-body-md text-on-surface-variant mt-1">
          {{ esEdicion ? 'Actualiza los detalles del evento.' : 'Completa los detalles para publicar tu nuevo evento.' }}
        </p>
      </div>
      <div class="flex gap-3">
        <AppButton variant="outlined" icon="arrow_back" @click="router.back()">Volver</AppButton>
        <AppButton :loading="cargando" :disabled="esFinalizado" icon="save" @click="guardarEvento">
          {{ cargando ? 'Guardando...' : (esEdicion ? (esFinalizado ? 'Evento Finalizado' : 'Actualizar') : 'Crear Evento') }}
        </AppButton>
      </div>
    </div>

    <Transition name="fade">
      <div v-if="errorGlobal" class="mb-6 p-4 bg-error-container border-l-4 border-error rounded-xl flex items-start gap-3">
        <span class="material-symbols-outlined text-error shrink-0">error</span>
        <p class="text-body-sm text-error font-medium">{{ errorGlobal }}</p>
      </div>
    </Transition>

    <div class="grid grid-cols-1 lg:grid-cols-12 gap-6">
      <div class="lg:col-span-5 space-y-5">
        <div class="bg-white rounded-xl border border-outline-variant/20 shadow-elevation-1 p-5">
          <p class="text-label-lg text-on-surface font-semibold mb-3">Imagen de portada</p>

          <div
            v-if="imagenPreview"
            class="relative group h-52 rounded-lg overflow-hidden border border-outline-variant"
          >
            <img :src="imagenPreview" alt="Vista previa" class="w-full h-full object-cover" />
            <div v-if="!esFinalizado" class="absolute inset-0 bg-black/60 opacity-0 group-hover:opacity-100 flex items-center justify-center transition-opacity">
              <AppButton variant="danger" icon="delete" size="sm" @click="eliminarImagen">Eliminar</AppButton>
            </div>
          </div>

          <div
            v-else
            class="h-52 rounded-lg border-2 border-dashed border-outline-variant flex flex-col items-center justify-center transition-all"
            :class="esFinalizado ? 'opacity-60 cursor-not-allowed' : 'cursor-pointer hover:border-primary hover:bg-primary/5'"
            @click="!esFinalizado && $refs.fileInput.click()"
            @dragover.prevent
            @drop.prevent="handleDrop"
          >
            <input ref="fileInput" type="file" accept="image/*" class="hidden" :disabled="esFinalizado" @change="handleFileSelect" />
            <span class="material-symbols-outlined text-[40px] text-outline mb-2">add_photo_alternate</span>
            <p class="text-body-sm text-on-surface-variant font-medium">Arrastra o haz clic</p>
            <p class="text-label-sm text-outline mt-1">JPG, PNG · máx. 5 MB</p>
          </div>

          <p v-if="errorImagen" class="text-body-sm text-error mt-2 flex items-center gap-1">
            <span class="material-symbols-outlined text-[14px]">error</span>{{ errorImagen }}
          </p>

          <p class="text-body-sm text-on-surface-variant mt-2 flex items-center gap-1">
            <span class="material-symbols-outlined text-[16px] text-outline">info</span>
            Imagen: JPG/PNG máx. 5 MB. Arrastra o haz clic sobre la portada.
          </p>
        </div>

        <div class="bg-white rounded-xl border border-outline-variant/20 shadow-elevation-1 p-5 space-y-4">
          <div>
            <AppInput
              v-model="form.nombreEvento"
              label="Nombre del evento"
              placeholder="Cumbre de Innovación Tech 2025"
              required
              :disabled="esFinalizado"
              :error="errores.nombreEvento"
            />
            <p class="text-label-sm text-on-surface-variant mt-1">Título visible en el catálogo. Debe ser único por fecha.</p>
          </div>

          <div class="grid grid-cols-2 gap-4">
            <div>
              <AppInput v-model="form.fechaInicioEvento" label="Fecha inicio" type="datetime-local" required :disabled="esFinalizado" :min="fechaMinima" :error="errores.fechaInicioEvento" />
              <p class="text-label-sm text-on-surface-variant mt-1">Cuándo inicia el evento.</p>
            </div>
            <div>
              <AppInput v-model="form.fechaFinEvento" label="Fecha fin" type="datetime-local" required :disabled="esFinalizado" :min="form.fechaInicioEvento || fechaMinima" :error="errores.fechaFinEvento" />
              <p class="text-label-sm text-on-surface-variant mt-1">Debe ser posterior al inicio.</p>
            </div>
          </div>

          <div class="grid grid-cols-2 gap-4">
            <div>
              <AppSelect
                v-model="form.modalidadEvento"
                label="Modalidad"
                :options="['Presencial','Virtual','Híbrido']"
                required
                :disabled="esFinalizado"
                :error="errores.modalidadEvento"
              />
              <p class="text-label-sm text-on-surface-variant mt-1">Define si pide lugar, URL o ambos.</p>
            </div>
            <div>
              <AppInput
                v-model.number="form.aforoMaximoEvento"
                label="Aforo máximo"
                type="number"
                placeholder="Ej: 200"
                min="0"
                step="1"
                :disabled="esFinalizado"
                :error="errores.aforoMaximoEvento"
                @keydown="prevenirTeclasInvalidas"
              />
              <p class="text-label-sm text-on-surface-variant mt-1">Cupos disponibles. Vacío = sin límite.</p>
            </div>
          </div>

          <div class="grid grid-cols-2 gap-4">
            <div>
              <AppInput
                v-model.number="form.duracionEvento"
                label="Duración (horas)"
                type="number"
                placeholder="Ej: 8"
                min="0.5"
                step="0.5"
                required
                :disabled="esFinalizado"
                :error="errores.duracionEvento"
                @keydown="prevenirTeclasInvalidas"
              />
              <p class="text-label-sm text-on-surface-variant mt-1">Horas totales del evento.</p>
            </div>
            <div>
              <AppSelect
                v-model="form.estadoEvento"
                label="Estado"
                :options="['borrador','activo','finalizado','cancelado']"
                required
                :disabled="esFinalizado"
                :error="errores.estadoEvento"
              />
              <p class="text-label-sm text-on-surface-variant mt-1">Borrador: oculto. Activo: visible.</p>
            </div>
          </div>

          <div>
            <AppInput
              v-model.number="form.diasMinimosCertificacion"
              label="Días mínimos de asistencia para certificar"
              type="number"
              placeholder="Ej: 2"
              min="0"
              step="1"
              :disabled="esFinalizado"
              :error="errores.diasMinimosCertificacion"
              @keydown="prevenirTeclasInvalidas"
            />
            <p class="text-label-sm text-on-surface-variant mt-1">Obligatorio si dura &gt;1 día. Máx. días del evento.</p>
          </div>
        </div>
      </div>

      <div class="lg:col-span-7 space-y-5">
        <div class="bg-white rounded-xl border border-outline-variant/20 shadow-elevation-1 p-5 space-y-4">
          <div class="grid grid-cols-2 gap-4">
            <div>
              <AppSelect
                v-model="form.tipoEvento"
                label="Tipo de evento"
                :options="tiposEvento"
                required
                :disabled="esFinalizado"
                :error="errores.tipoEvento"
              />
              <p class="text-label-sm text-on-surface-variant mt-1">Categoría del evento.</p>
            </div>
            <!-- Presencial: solo Lugar | Virtual: solo URL | Híbrido: ambos -->
            <div v-if="esPresencial || esHibrido">
              <AppInput
                v-model="form.lugarEvento"
                label="Lugar"
                placeholder="Ej: Auditorio Principal, Cra 30 #45-03"
                :required="esPresencial || esHibrido"
                :disabled="esFinalizado"
                :error="errores.lugarEvento"
              />
              <p class="text-label-sm text-on-surface-variant mt-1">Dirección o sala presencial.</p>
            </div>
            <div v-if="esVirtual || esHibrido">
              <AppInput
                v-model="form.enlaceUrl"
                label="URL del evento"
                placeholder="Ej: https://meet.google.com/abc-defg-hij"
                type="url"
                :required="esVirtual || esHibrido"
                :disabled="esFinalizado"
                :error="errores.enlaceUrl"
              />
              <p class="text-label-sm text-on-surface-variant mt-1">Enlace con https:// para acceso virtual.</p>
            </div>
            <!-- Placeholder cuando aún no selecciona modalidad -->
            <div v-if="!form.modalidadEvento">
              <AppInput
                v-model="form.lugarEvento"
                label="Lugar / URL"
                placeholder="Selecciona primero la modalidad"
                disabled
                :error="errores.lugarEvento"
              />
              <p class="text-label-sm text-on-surface-variant mt-1">Elige modalidad para habilitar el campo.</p>
            </div>
          </div>
        </div>

        <div class="bg-white rounded-xl border border-outline-variant/20 shadow-elevation-1 p-5">
          <AppInput
            v-model="form.descripcionEvento"
            label="Descripción detallada"
            type="textarea"
            :rows="10"
            maxlength="2000"
            :disabled="esFinalizado"
            placeholder="Cuéntanos más sobre la agenda, los ponentes y qué esperar del evento..."
          />
          <p class="text-label-sm text-on-surface-variant mt-1">Agenda y ponentes. Se guarda en la BD.</p>
          <p class="text-right text-label-sm text-outline mt-2">
            {{ (form.descripcionEvento || '').length }} / 2000
          </p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useEventoStore } from '../../stores/evento.js'
import eventoAPI from '../../services/eventoAPI.js'
import AppInput  from '../../components/ui/AppInput.vue'
import AppSelect from '../../components/ui/AppSelect.vue'
import AppButton from '../../components/ui/AppButton.vue'

const router = useRouter()
const route  = useRoute()
const store  = useEventoStore()

const esEdicion  = computed(() => !!route.params.id)
const cargando   = ref(false)
const errorGlobal= ref('')
const errores    = reactive({})

const fileInput    = ref(null)
const imagenPreview= ref(null)
const imagenBase64 = ref(null)
const errorImagen  = ref('')

const esFinalizado = ref(false)

const tiposEvento = [
  'Conferencia', 'Seminario', 'Congreso', 'Taller', 'Capacitación',
  'Curso', 'Charla', 'Webinar', 'Torneo', 'Networking',
  'Feria', 'Cultural', 'Deportivo', 'Bienestar',
]

const modalidadNorm = computed(() => (form.modalidadEvento||'').toLowerCase().normalize('NFD').replace(/[\u0300-\u036f]/g,''))
const esPresencial = computed(() => modalidadNorm.value === 'presencial')
const esVirtual = computed(() => modalidadNorm.value === 'virtual')
const esHibrido = computed(() => modalidadNorm.value === 'hibrido')

const form = reactive({
  nombreEvento:      '',
  descripcionEvento: '',
  tipoEvento:        'Conferencia',
  modalidadEvento:   '',
  fechaInicioEvento: '',
  fechaFinEvento:    '',
  lugarEvento:       '',
  aforoMaximoEvento: null,
  duracionEvento:    null,
  diasMinimosCertificacion: null,
  estadoEvento:      'activo',
  imagenUrl:         '',
  enlaceUrl:         '',
})

const fechaMinima = computed(() => {
  const d = new Date()
  return `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')}T${String(d.getHours()).padStart(2,'0')}:${String(d.getMinutes()).padStart(2,'0')}`
})

function handleFileSelect(e) { if (!esFinalizado.value) procesarImagen(e.target.files[0]) }
function handleDrop(e) { if (!esFinalizado.value) procesarImagen(e.dataTransfer.files[0]) }

function procesarImagen(file) {
  errorImagen.value = ''
  if (!file) return
  if (!file.type.startsWith('image/')) { errorImagen.value = 'Solo se permiten imágenes'; return }
  if (file.size > 5 * 1024 * 1024)    { errorImagen.value = 'La imagen no debe superar 5 MB'; return }

  const reader = new FileReader()
  reader.onload = e => {
    imagenPreview.value = e.target.result
    imagenBase64.value  = e.target.result
    form.imagenUrl      = e.target.result
  }
  reader.readAsDataURL(file)
}

function eliminarImagen() {
  if (esFinalizado.value) return
  imagenPreview.value = null
  imagenBase64.value  = null
  form.imagenUrl      = ''
  if (fileInput.value) fileInput.value.value = ''
}

function prevenirTeclasInvalidas(e) {
  if (['-', '+', 'e', 'E'].includes(e.key)) e.preventDefault()
}

function calcularDiasEvento() {
  if (!form.fechaInicioEvento || !form.fechaFinEvento) return 1
  const inicio = new Date(form.fechaInicioEvento); inicio.setHours(0,0,0,0)
  const fin = new Date(form.fechaFinEvento); fin.setHours(0,0,0,0)
  const diff = fin - inicio
  if (isNaN(diff) || diff < 0) return 1
  return Math.floor(diff / 86400000) + 1
}
// Letras (con tildes/ñ), números, puntuación y símbolos: se permiten todos
// excepto "< >" y secuencias de HTML/scripts para evitar inserciones maliciosas.
const SECUENCIAS_INSEGURAS = /<\s*\/?\s*[a-zA-Z]|<\s*script|javascript\s*:|on[a-z]+\s*=|&\s*\w+\s*;|<\?|-->\s*\/\s*/i

function sanitizarTexto(valor = '') {
  if (!valor) return ''
  return String(valor)
    .replace(/&lt;/g, '<').replace(/&gt;/g, '>')
    .replace(/[\u0000-\u0008\u000B\u000C\u000E-\u001F\u007F]/g, '')
    .replace(/<\s*script\b[\s\S]*?<\/\s*script\s*>/gi, '')
    .replace(/javascript\s*:/gi, '')
    .replace(/\bon\w+\s*=\s*(['"]?)[^'"]*\1/gi, '')
    .trim()
}

function camposConContenidoInseguro() {
  const inseguros = {}
  ;['nombreEvento','descripcionEvento','lugarEvento'].forEach(campo => {
    const valor = form[campo]
    if (valor && (SECUENCIAS_INSEGURAS.test(valor) || /[<>]/u.test(valor))) {
      inseguros[campo] = true
    }
  })
  return inseguros
}
function validar() {
  Object.keys(errores).forEach(k => delete errores[k])
  if (!form.nombreEvento?.trim())    errores.nombreEvento      = 'El nombre es obligatorio'
  const inseguros = camposConContenidoInseguro()
  if (inseguros.nombreEvento)       errores.nombreEvento      = 'El nombre contiene código o etiquetas no permitidas'
  if (inseguros.descripcionEvento)  errores.descripcionEvento = 'La descripción contiene código o etiquetas no permitidas'
  if (inseguros.lugarEvento)        errores.lugarEvento       = 'El lugar contiene código o etiquetas no permitidas'
  if ((form.descripcionEvento || '').length > 2000) errores.descripcionEvento = 'La descripción no puede superar 2000 caracteres'
  // Validación Lugar/URL según modalidad - no mezclados
  if (esPresencial.value) {
    if (!form.lugarEvento?.trim()) errores.lugarEvento = 'El lugar es obligatorio para modalidad presencial'
  } else if (esVirtual.value) {
    if (!form.enlaceUrl?.trim()) errores.enlaceUrl = 'La URL es obligatoria para modalidad virtual'
    else if (!/^https?:\/\/.+/.test(form.enlaceUrl.trim())) errores.enlaceUrl = 'Debe ser una URL válida (https://...)'
  } else if (esHibrido.value) {
    if (!form.lugarEvento?.trim()) errores.lugarEvento = 'El lugar es obligatorio para modalidad híbrida'
    if (!form.enlaceUrl?.trim()) errores.enlaceUrl = 'La URL es obligatoria para modalidad híbrida'
    else if (!/^https?:\/\/.+/.test(form.enlaceUrl.trim())) errores.enlaceUrl = 'Debe ser una URL válida (https://...)'
  } else if (!form.lugarEvento?.trim() && !form.enlaceUrl?.trim()) {
    // sin modalidad seleccionada aún: exigir al menos lugar por compatibilidad
    errores.lugarEvento = 'Selecciona modalidad y completa el campo correspondiente'
  }
  if (!form.fechaInicioEvento)       errores.fechaInicioEvento = 'La fecha de inicio es obligatoria'
  if (!form.fechaFinEvento)          errores.fechaFinEvento    = 'La fecha de fin es obligatoria'
  if (!form.modalidadEvento)         errores.modalidadEvento   = 'Selecciona una modalidad'
  if (!form.duracionEvento)          errores.duracionEvento    = 'Ingresa la duración'
  else if (form.duracionEvento <= 0) errores.duracionEvento    = 'La duración debe ser mayor a 0'
  if (form.aforoMaximoEvento !== null && form.aforoMaximoEvento !== '') {
    if (form.aforoMaximoEvento < 0) errores.aforoMaximoEvento = 'El aforo no puede ser negativo'
    else if (!Number.isInteger(form.aforoMaximoEvento)) errores.aforoMaximoEvento = 'El aforo debe ser un número entero'
  }
  const dias = calcularDiasEvento()
  const esMultiDia = dias > 1
  const dm = form.diasMinimosCertificacion
  const dmVacio = dm === null || dm === ''
  if (esMultiDia) {
    if (dmVacio) errores.diasMinimosCertificacion = 'Obligatorio para eventos de más de 1 día'
    else if (dm < 1) errores.diasMinimosCertificacion = 'Debe ser al menos 1'
    else if (!Number.isInteger(dm)) errores.diasMinimosCertificacion = 'Debe ser un número entero'
    else if (dm > dias) errores.diasMinimosCertificacion = `No puede superar los ${dias} día(s) que dura el evento`
  } else if (!dmVacio) {
    if (dm < 1) errores.diasMinimosCertificacion = 'Debe ser al menos 1'
    else if (!Number.isInteger(dm)) errores.diasMinimosCertificacion = 'Debe ser un número entero'
    else if (dm > 1) errores.diasMinimosCertificacion = `Para eventos de 1 día no puede superar 1`
  }
  if (form.fechaFinEvento && form.fechaInicioEvento && form.fechaFinEvento <= form.fechaInicioEvento)
    errores.fechaFinEvento = 'La fecha de fin debe ser posterior al inicio'
  return Object.keys(errores).length === 0
}

async function guardarEvento() {
  errorGlobal.value = ''
  if (esFinalizado.value) {
    errorGlobal.value = 'Este evento ya ha finalizado y no se puede editar.'
    return
  }
  if (!validar()) return

  // Sanitizar contra inserciones maliciosas antes de enviar al backend
  form.nombreEvento      = sanitizarTexto(form.nombreEvento).replace(/[<>]/g, '')
  form.descripcionEvento = sanitizarTexto(form.descripcionEvento).replace(/[<>]/g, '')
  form.lugarEvento       = sanitizarTexto(form.lugarEvento).replace(/[<>]/g, '')

  // No mezclar: Presencial solo lugar, Virtual solo URL, Híbrido ambos
  if (esPresencial.value) {
    form.enlaceUrl = ''
  } else if (esVirtual.value) {
    form.lugarEvento = ''
  }

  cargando.value = true
  try {
    if (esEdicion.value) {
      await store.update(Number(route.params.id), form)
    } else {
      await store.create(form)
    }
    router.push('/app/eventos')
  } catch (e) {
    errorGlobal.value = e.response?.data?.error || 'Error al guardar el evento. Inténtalo de nuevo.'
  } finally {
    cargando.value = false
  }
}

onMounted(async () => {
  if (esEdicion.value) {
    try {
      const { data } = await eventoAPI.getById(Number(route.params.id))
      Object.assign(form, data)
      if (data.estadoEvento) form.estadoEvento = data.estadoEvento.toLowerCase()
      // Mantener lugar y enlace separados (compatibilidad: dato viejo con URL en lugarEvento)
      if (data.enlaceUrl && !data.lugarEvento && data.modalidadEvento?.toLowerCase().includes('virtual')) {
        // no hacer nada, ya está en enlaceUrl
      }
      if (data.imagenUrl) imagenPreview.value = data.imagenUrl

      const fechaTermino = data.fechaFinEvento || data.fechaInicioEvento
      if ((data.estadoEvento && data.estadoEvento.toLowerCase() === 'finalizado') || (fechaTermino && new Date(fechaTermino) < new Date())) {
        esFinalizado.value = true
        errorGlobal.value = 'Este evento ya ha finalizado y no se puede editar.'
      }
    } catch {
      errorGlobal.value = 'No se pudo cargar el evento para editar.'
    }
  }
})
</script>

<style scoped>
.fade-enter-active, .fade-leave-active { transition: opacity 0.2s; }
.fade-enter-from, .fade-leave-to { opacity: 0; }
</style>