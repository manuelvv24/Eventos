<template>
  <div class="p-6 md:p-8 space-y-6 animate-fade-in">
    <div class="flex items-center justify-between">
      <div>
        <h1 class="text-headline-md text-on-surface font-semibold">Participantes</h1>
        <p class="text-body-md text-on-surface-variant mt-1">Gestiona los participantes registrados en el sistema.</p>
      </div>
      <AppButton v-if="canEdit" icon="person_add" @click="abrirModal()">Agregar</AppButton>
    </div>

    <AppBanner v-model="errorBanner" type="error" />
    <AppBanner v-model="exitoBanner" type="success" />

    <AppTable
      title="Lista de Participantes"
      :columns="columnas"
      :rows="filasFiltradas"
      :loading="store.loading"
      :error="store.error"
      row-key="idParticipantes"
      default-sort-key="idParticipantes"
      search-placeholder="Buscar por ID, nombre completo, email, documento o tipo doc..."
      :search-fields="['idParticipantes','idUsuarios','nombreCompleto','primerNombre','segundoNombre','primerApellido','segundoApellido','email','tipoDocumento','documento','telefono']"
      @retry="store.fetchAll()"
    >
      <template #filters>
        <AppSelect v-model="filtroTipoDoc" :options="opcionesTipoDoc" placeholder="Tipo Doc." class="w-36" />
      </template>
      <template #cell-idParticipantes="{ row }">
        <span class="font-mono text-label-md bg-primary-container/40 text-on-primary-container px-2 py-1 rounded-md font-bold">#{{ row.idParticipantes }}</span>
      </template>
      <template #cell-nombre="{ row }">
        <div class="flex items-center gap-2">
          <div class="w-8 h-8 rounded-full bg-primary-container flex items-center justify-center text-on-primary-container font-bold text-label-sm shrink-0">{{ iniciales(row) }}</div>
          <span class="text-label-lg text-on-surface">{{ nombreCompleto(row) }}</span>
        </div>
      </template>
      <template #actions="{ row }">
        <ButtonIcon icon="history" title="Ver historial" @click="verHistorial(row)" />
        <ButtonIcon v-if="canEdit" icon="delete" title="Eliminar" variant="danger" @click="eliminar(row)" />
      </template>
    </AppTable>

    <AppModal v-model="modalHistorial" title="Historial de Eventos" :subtitle="participanteSeleccionado?.email" size="lg">
      <div v-if="loadingHistorial" class="flex justify-center py-8"><AppSpinner size="lg" class="text-primary" /></div>
      <div v-else-if="!historial.length" class="text-center py-8 text-on-surface-variant">
        <span class="material-symbols-outlined text-[48px] block mb-2">event_busy</span>
        <p>Sin eventos registrados</p>
      </div>
      <div v-else class="space-y-3">
        <div v-for="r in historial" :key="r.idRegistro" class="p-4 rounded-xl border border-outline-variant/20 bg-surface-container-lowest">
          <div class="flex items-start justify-between">
            <div>
              <p class="text-label-lg text-on-surface font-semibold">{{ r.nombreEvento }}</p>
              <p class="text-body-sm text-on-surface-variant mt-0.5">{{ formatFecha(r.fechaInicio) }} · {{ r.tipoEvento }} · {{ r.modalidadEvento }}</p>
            </div>
            <AppBadge :variant="String(r.estadoAsistencia ?? '').toUpperCase() === 'ASISTIO' ? 'success' : 'neutral'" :dot="true">{{ r.estadoAsistencia }}</AppBadge>
          </div>
        </div>
      </div>
    </AppModal>

    <!-- Modal crear participante — misma estructura que Auth/Registro.vue -->
    <AppModal v-model="modalCrear" title="Nuevo Participante" subtitle="Registra un nuevo participante en el sistema" size="lg">
      <div v-if="errorBannerModal" class="mb-4 p-3 rounded-xl bg-error-container text-error text-body-sm flex items-center gap-2">
        <span class="material-symbols-outlined text-[20px]">error</span>{{ errorBannerModal }}
      </div>
      <form class="space-y-4" @submit.prevent="crearParticipante">
        <!-- Nombres -->
        <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div>
            <label class="block text-label-lg text-on-surface mb-1">Primer nombre *</label>
            <input v-model="formPart.primerNombreUsuario" type="text" required placeholder="Ej. Juan" maxlength="30" class="w-full px-4 py-3 rounded-lg border border-outline-variant focus:outline-none focus:border-primary focus:ring-2 focus:ring-primary/20 text-body-md" @input="formPart.primerNombreUsuario = limpiarNombreValue($event.target.value)" />
            <p v-if="errorNombreCorto(formPart.primerNombreUsuario)" class="mt-1 text-label-sm text-error">{{ errorNombreCorto(formPart.primerNombreUsuario) }}</p>
          </div>
          <div>
            <label class="block text-label-lg text-on-surface mb-1">Segundo nombre</label>
            <input v-model="formPart.segundoNombreUsuario" type="text" placeholder="(Opcional)" maxlength="30" class="w-full px-4 py-3 rounded-lg border border-outline-variant focus:outline-none focus:border-primary focus:ring-2 focus:ring-primary/20 text-body-md" @input="formPart.segundoNombreUsuario = limpiarNombreValue($event.target.value)" />
          </div>
        </div>
        <!-- Apellidos -->
        <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div>
            <label class="block text-label-lg text-on-surface mb-1">Primer apellido *</label>
            <input v-model="formPart.primerApellidoUsuario" type="text" required placeholder="Ej. Pérez" maxlength="30" class="w-full px-4 py-3 rounded-lg border border-outline-variant focus:outline-none focus:border-primary focus:ring-2 focus:ring-primary/20 text-body-md" @input="formPart.primerApellidoUsuario = limpiarNombreValue($event.target.value)" />
            <p v-if="errorNombreCorto(formPart.primerApellidoUsuario)" class="mt-1 text-label-sm text-error">{{ errorNombreCorto(formPart.primerApellidoUsuario) }}</p>
          </div>
          <div>
            <label class="block text-label-lg text-on-surface mb-1">Segundo apellido</label>
            <input v-model="formPart.segundoApellidoUsuario" type="text" placeholder="(Opcional)" maxlength="30" class="w-full px-4 py-3 rounded-lg border border-outline-variant focus:outline-none focus:border-primary focus:ring-2 focus:ring-primary/20 text-body-md" @input="formPart.segundoApellidoUsuario = limpiarNombreValue($event.target.value)" />
          </div>
        </div>
        <!-- Documento -->
        <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div>
            <label class="block text-label-lg text-on-surface mb-1">Tipo de documento *</label>
            <select v-model="formPart.tipoDocumentoUsuario" required class="w-full px-4 py-3 rounded-lg border border-outline-variant bg-white text-body-md">
              <option value="" disabled>Seleccione...</option>
              <option value="CC">Cédula de Ciudadanía</option>
              <option value="CE">Cédula de Extranjería</option>
              <option value="TI">Tarjeta de Identidad</option>
              <option value="PA">Pasaporte</option>
            </select>
          </div>
          <div>
            <label class="block text-label-lg text-on-surface mb-1">Número de documento * <span class="text-label-sm font-normal text-on-surface-variant">(máx. 18)</span></label>
            <input v-model="formPart.numeroDocumentoUsuario" type="text" required placeholder="Ej. 1020304050" maxlength="18" class="w-full px-4 py-3 rounded-lg border border-outline-variant focus:outline-none focus:border-primary text-body-md uppercase" @input="formPart.numeroDocumentoUsuario = $event.target.value.toUpperCase().replace(/[^A-Z0-9]/g,'').slice(0,18)" />
            <p v-if="errorDocumento" class="mt-1 text-label-sm text-error">{{ errorDocumento }}</p>
          </div>
        </div>
        <!-- Teléfono -->
        <div>
          <label class="block text-label-lg text-on-surface mb-1">Teléfono *</label>
          <input v-model="formPart.numeroTelefonoUsuario" type="tel" required placeholder="300 123 4567" maxlength="15" class="w-full px-4 py-3 rounded-lg border border-outline-variant focus:outline-none focus:border-primary text-body-md" @input="formPart.numeroTelefonoUsuario = $event.target.value.replace(/[^0-9]/g,'').slice(0,15)" />
        </div>
        <!-- Email -->
        <div>
          <label class="block text-label-lg text-on-surface mb-1">Correo electrónico *</label>
          <input v-model="formPart.emailUsuario" type="email" required placeholder="correo@ejemplo.com" maxlength="100" class="w-full px-4 py-3 rounded-lg border border-outline-variant focus:outline-none focus:border-primary text-body-md" />
        </div>
        <!-- Contraseña -->
        <div>
          <label class="block text-label-lg text-on-surface mb-1">Contraseña *</label>
          <div class="relative">
            <input v-model="formPart.contrasenaUsuario" :type="mostrarPassword ? 'text' : 'password'" required placeholder="••••••••" class="w-full px-4 py-3 pr-12 rounded-lg border border-outline-variant focus:outline-none focus:ring-2 focus:ring-primary/20 text-body-md" :class="formPart.contrasenaUsuario && !esValida ? 'border-error' : ''" />
            <button type="button" tabindex="-1" class="absolute right-3 top-1/2 -translate-y-1/2 text-on-surface-variant" @click="mostrarPassword=!mostrarPassword"><span class="material-symbols-outlined text-[22px]">{{ mostrarPassword?'visibility_off':'visibility' }}</span></button>
          </div>
          <template v-if="formPart.contrasenaUsuario">
            <div class="flex gap-1 mt-2"><div v-for="i in 5" :key="i" class="h-1.5 flex-1 rounded-full" :class="i <= fortaleza.nivel ? fortaleza.color : 'bg-surface-container-highest'"></div></div>
            <p class="text-label-sm mt-1 font-medium" :class="fortaleza.textoClase">Fortaleza: {{ fortaleza.texto }}</p>
            <ul class="mt-2 grid grid-cols-1 sm:grid-cols-2 gap-1">
              <li v-for="r in reglas" :key="r.key" class="flex items-center gap-1.5 text-label-sm" :class="r.cumple?'text-success':'text-on-surface-variant'"><span class="material-symbols-outlined text-[16px]">{{ r.cumple?'check_circle':'radio_button_unchecked' }}</span>{{ r.label }}</li>
            </ul>
          </template>
        </div>
        <!-- Confirmar -->
        <div>
          <label class="block text-label-lg text-on-surface mb-1">Confirmar contraseña *</label>
          <input v-model="confirmarPassword" :type="mostrarPassword ? 'text' : 'password'" required placeholder="••••••••" class="w-full px-4 py-3 rounded-lg border border-outline-variant text-body-md" :class="confirmarPassword && !coinciden ? 'border-error' : ''" />
          <p v-if="confirmarPassword && !coinciden" class="mt-1 text-label-sm text-error">Las contraseñas no coinciden</p>
        </div>
      </form>
      <template #footer>
        <AppButton variant="outlined" @click="modalCrear=false">Cancelar</AppButton>
        <AppButton :loading="guardandoPart" @click="crearParticipante">Crear participante</AppButton>
      </template>
    </AppModal>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, toRef } from 'vue'
import { useAuthStore, ROLES } from '../../stores/auth.js'
import { useParticipanteStore } from '../../stores/participante.js'
import participanteAPI from '../../services/participanteAPI.js'
import usuarioAPI from '../../services/usuarioAPI.js'
import api from '../../services/api.js'
import AppTable from '../../components/ui/AppTable.vue'
import AppSelect from '../../components/ui/AppSelect.vue'
import AppButton from '../../components/ui/AppButton.vue'
import AppModal from '../../components/ui/AppModal.vue'
import AppBadge from '../../components/ui/AppBadge.vue'
import AppSpinner from '../../components/ui/AppSpinner.vue'
import AppBanner from '../../components/ui/AppBanner.vue'
import ButtonIcon from '../../components/ui/ButtonIcon.vue'
import { useConfirm } from '../../composables/useConfirm.js'
import { usePasswordStrength } from '../../composables/usePasswordStrength.js'

const auth = useAuthStore()
const store = useParticipanteStore()
const { confirm } = useConfirm()
const canEdit = computed(() => auth.hasRole(ROLES.ADMIN, ROLES.OPERADOR))

const errorBanner = ref('')
const exitoBanner = ref('')
const errorBannerModal = ref('')

const columnas = [
  { key: 'idParticipantes', label: 'Nº Participante' },
  { key: 'nombre', label: 'Nombre' },
  { key: 'email', label: 'Email' },
  { key: 'tipoDocumento', label: 'Tipo Doc.' },
  { key: 'documento', label: 'Documento' },
]

const filtroTipoDoc = ref('')
const opcionesTipoDoc = [
  { value: '', label: 'Todos' },
  { value: 'CC', label: 'CC' },
  { value: 'CE', label: 'CE' },
  { value: 'TI', label: 'TI' },
  { value: 'PA', label: 'PA' },
]

const filasFiltradas = computed(() => {
  let rows = store.items
  if (filtroTipoDoc.value) {
    rows = rows.filter(p => String(p.tipoDocumento ?? '').toUpperCase() === filtroTipoDoc.value)
  }
  return rows.map(p => ({ ...p, nombreCompleto: nombreCompleto(p) }))
})

const modalHistorial = ref(false)
const participanteSeleccionado = ref(null)
const historial = ref([])
const loadingHistorial = ref(false)

function iniciales(row) { return (row.primerNombre?.[0] ?? '') + (row.primerApellido?.[0] ?? '') }
function nombreCompleto(row) {
  return [row.primerNombre, row.segundoNombre, row.primerApellido, row.segundoApellido].filter(Boolean).join(' ')
}
function formatFecha(f) { return f ? new Date(f).toLocaleDateString('es-ES', { day:'2-digit', month:'short', year:'numeric' }) : '—' }

async function verHistorial(row) {
  participanteSeleccionado.value = row
  modalHistorial.value = true
  loadingHistorial.value = true
  historial.value = []
  try { const { data } = await participanteAPI.historial(row.idParticipantes); historial.value = data } catch { historial.value = [] } finally { loadingHistorial.value = false }
}

const modalCrear = ref(false)
const guardandoPart = ref(false)
const mostrarPassword = ref(false)
const confirmarPassword = ref('')
const formPart = reactive({ primerNombreUsuario:'', segundoNombreUsuario:'', primerApellidoUsuario:'', segundoApellidoUsuario:'', emailUsuario:'', tipoDocumentoUsuario:'', numeroDocumentoUsuario:'', numeroTelefonoUsuario:'', contrasenaUsuario:'' })
const { reglas, fortaleza, esValida } = usePasswordStrength(toRef(formPart,'contrasenaUsuario'))
const coinciden = computed(() => formPart.contrasenaUsuario.length > 0 && formPart.contrasenaUsuario === confirmarPassword.value)

function limpiarNombreValue(v){ return (v||'').replace(/[^A-Za-zÁÉÍÓÚÜÑáéíóúüñ\s]/g,'').replace(/\s+/g,' ').slice(0,30) }
function errorNombreCorto(v){ const t=(v||'').trim(); if(!t) return ''; if(t.length<2) return 'Debe tener al menos 2 caracteres'; return '' }
const errorDocumento = computed(()=>{ const d=(formPart.numeroDocumentoUsuario||'').trim(); if(!d) return ''; if(d.length<5) return 'Debe tener al menos 5 caracteres'; return '' })

function abrirModal() {
  Object.assign(formPart,{ primerNombreUsuario:'', segundoNombreUsuario:'', primerApellidoUsuario:'', segundoApellidoUsuario:'', emailUsuario:'', tipoDocumentoUsuario:'', numeroDocumentoUsuario:'', numeroTelefonoUsuario:'', contrasenaUsuario:'' })
  confirmarPassword.value=''; errorBannerModal.value=''
  modalCrear.value = true
}
async function crearParticipante() {
  if(errorDocumento.value){ errorBannerModal.value=errorDocumento.value; return }
  if(errorNombreCorto(formPart.primerNombreUsuario) || errorNombreCorto(formPart.primerApellidoUsuario)){ errorBannerModal.value='Nombre y apellido deben tener al menos 2 caracteres'; return }
  if(!esValida.value){ errorBannerModal.value='La contraseña no cumple los requisitos'; return }
  if(!coinciden.value){ errorBannerModal.value='Las contraseñas no coinciden'; return }
  guardandoPart.value = true; errorBannerModal.value=''
  try {
    const { data: usuario } = await usuarioAPI.create({
      primerNombreUsuario: formPart.primerNombreUsuario.trim(),
      segundoNombreUsuario: formPart.segundoNombreUsuario.trim()||null,
      primerApellidoUsuario: formPart.primerApellidoUsuario.trim(),
      segundoApellidoUsuario: formPart.segundoApellidoUsuario.trim()||null,
      emailUsuario: formPart.emailUsuario.trim(),
      contrasenaUsuario: formPart.contrasenaUsuario,
      tipoDocumentoUsuario: formPart.tipoDocumentoUsuario,
      numeroDocumentoUsuario: formPart.numeroDocumentoUsuario.trim(),
      numeroTelefonoUsuario: formPart.numeroTelefonoUsuario.trim()||null,
      idRol: 4, estadoUsuario: true
    })
    await participanteAPI.create({ idUsuarios: usuario.idUsuario, usuario:{ idUsuario: usuario.idUsuario } }).catch(()=> api.post('/participantes',{ usuario:{ idUsuario: usuario.idUsuario }}))
    modalCrear.value=false; exitoBanner.value='Participante agregado correctamente.'; await store.fetchAll()
  } catch(e){ errorBannerModal.value=e.response?.data?.error||e.response?.data?.message||'Error al crear participante' } finally { guardandoPart.value=false }
}

async function eliminar(row) {
  const ok = await confirm({ title:'Eliminar participante', message:`¿Eliminar participante ${nombreCompleto(row)}? Se eliminarán también sus inscripciones.`, confirmText:'Sí, eliminar', danger:true })
  if(!ok) return
  try { await store.remove(row.idParticipantes); exitoBanner.value=`${nombreCompleto(row)} ha sido eliminado.`; errorBanner.value='' } catch(e){ errorBanner.value=e.response?.data?.error||'Error al eliminar el participante' }
}
onMounted(()=> store.fetchAll())
</script>
