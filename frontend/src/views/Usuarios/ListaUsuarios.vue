<template>
  <div class="p-6 md:p-8 space-y-6 animate-fade-in">
    <div class="flex items-center justify-between">
      <div>
        <h1 class="text-headline-md text-on-surface font-semibold">Usuarios</h1>
        <p class="text-body-md text-on-surface-variant mt-1">Administra los usuarios del sistema y sus roles.</p>
      </div>
      <AppButton icon="person_add" @click="abrirFormulario(null)">Nuevo Usuario</AppButton>
    </div>

    <!-- Banners de Notificación Superior -->
    <AppBanner v-model="errorBanner" type="error" />
    <AppBanner v-model="exitoBanner" type="success" />

    <AppTable
      title="Lista de Usuarios"
      :columns="columnas"
      :rows="usuariosFiltrados"
      :loading="loading"
      :error="error"
      row-key="idUsuario"
      default-sort-key="idUsuario"
      search-placeholder="Buscar por ID, nombre, email, documento o rol..."
      :search-fields="['idUsuario','nombreCompleto','primerNombreUsuario','segundoNombreUsuario','primerApellidoUsuario','segundoApellidoUsuario','emailUsuario','tipoDocumentoUsuario','numeroDocumentoUsuario','numeroTelefonoUsuario','rolNombre','nombresRoles']"
      @retry="cargar"
    >
      <template #filters>
        <AppSelect v-model="filtroRol" :options="opcionesRolFiltro" placeholder="Rol" class="w-36" />
        <AppSelect v-model="filtroEstado" :options="opcionesEstado" placeholder="Estado" class="w-32" />
      </template>

      <template #cell-nombre="{ row }">
        <div class="flex items-center gap-2">
          <div class="w-8 h-8 rounded-full bg-primary-container flex items-center justify-center text-on-primary-container text-label-sm font-bold shrink-0">
            {{ (row.primerNombreUsuario?.[0] ?? '') + (row.primerApellidoUsuario?.[0] ?? '') }}
          </div>
          <div>
            <p class="text-label-lg text-on-surface">{{ row.nombreCompleto || `${row.primerNombreUsuario} ${row.primerApellidoUsuario}` }}</p>
            <p class="text-body-sm text-on-surface-variant">{{ row.emailUsuario }}</p>
          </div>
        </div>
      </template>

      <template #cell-rol="{ row }">
        <div class="flex flex-wrap gap-1">
          <AppBadge
            v-for="rol in (row.nombresRoles?.length ? row.nombresRoles : [row.rolNombre ?? rolLabel(row.idRol)])"
            :key="rol"
            :variant="rolVariant(rol)"
          >{{ rol }}</AppBadge>
        </div>
      </template>

      <template #cell-estado="{ row }">
        <AppBadge :variant="row.estadoUsuario ? 'success' : 'error'" :dot="true">
          {{ row.estadoUsuario ? 'Activo' : 'Inactivo' }}
        </AppBadge>
      </template>

      <template #actions="{ row }">
        <ButtonIcon icon="edit" title="Editar" @click="abrirFormulario(row)" />
        <ButtonIcon
          :icon="row.estadoUsuario ? 'block' : 'check_circle'"
          :title="row.estadoUsuario ? 'Desactivar' : 'Activar'"
          variant="danger"
          @click="solicitarToggleEstado(row)"
        />
        <ButtonIcon icon="delete" title="Eliminar" variant="danger" @click="solicitarEliminar(row)" />
      </template>
    </AppTable>

    <!-- Modal usuario (Crear / Editar) -->
    <AppModal v-model="modal" :title="editando ? 'Editar Usuario' : 'Nuevo Usuario'" size="md">
      <form class="space-y-4" @submit.prevent="solicitarGuardar">
        <!-- Banner Visual de Advertencia de Privilegios al cambiar rol de ADMIN -->
        <Transition name="fade">
          <div v-if="editando && originalRoles.includes(ADMIN_ID) && !form.idsRoles.includes(ADMIN_ID)" class="p-4 bg-amber-50 border-l-4 border-amber-500 rounded-xl flex items-start gap-3 shadow-sm">
            <span class="material-symbols-outlined text-amber-600 shrink-0 text-[24px]">warning</span>
            <div>
              <p class="text-label-lg text-amber-900 font-semibold">⚠️ Advertencia: Reducción de permisos</p>
              <p class="text-body-sm text-amber-800 mt-0.5 leading-relaxed">
                {{ esEdicionPropia
                  ? 'Estás a punto de reducir tus propios privilegios quitándote el rol de Administrador. Al guardar, tu sesión actual se cerrará por seguridad.'
                  : 'Estás modificando el rol de este Administrador a un rol con menores privilegios del sistema.' }}
              </p>
            </div>
          </div>
        </Transition>

        <div class="grid grid-cols-2 gap-4">
          <AppInput v-model="form.primerNombreUsuario" label="Primer nombre" required />
          <AppInput v-model="form.segundoNombreUsuario" label="Segundo nombre" />
          <AppInput v-model="form.primerApellidoUsuario" label="Primer apellido" required />
          <AppInput v-model="form.segundoApellidoUsuario" label="Segundo apellido" />
        </div>
        <AppInput v-model="form.emailUsuario" label="Correo electrónico" type="email" prefix-icon="mail" required />
        <AppInput v-model="form.contrasenaUsuario" :label="editando ? 'Nueva contraseña (dejar vacío para no cambiar)' : 'Contraseña'" type="password" :required="!editando" />
        <div class="grid grid-cols-2 gap-4">
          <AppSelect v-model="form.tipoDocumentoUsuario" label="Tipo documento" :options="tiposDoc" required />
          <AppInput v-model="form.numeroDocumentoUsuario" label="Número documento" required />
        </div>
        <AppInput v-model="form.numeroTelefonoUsuario" label="Teléfono" prefix-icon="phone" />
        <div>
          <label class="block text-label-lg text-on-surface mb-2">Roles *</label>
          <div class="flex flex-wrap gap-2">
            <button
              v-for="opcion in opcionesRoles"
              :key="opcion.value"
              type="button"
              class="px-3 py-1.5 rounded-full border text-label-md font-medium transition flex items-center gap-1.5"
              :class="form.idsRoles.includes(Number(opcion.value))
                ? 'border-primary bg-primary text-on-primary'
                : 'border-outline-variant bg-white text-on-surface-variant hover:border-primary'"
              @click="toggleRol(Number(opcion.value))"
            >
              <span v-if="form.idsRoles.includes(Number(opcion.value))" class="material-symbols-outlined text-[16px]">check</span>
              {{ opcion.label }}
            </button>
          </div>
          <p class="text-body-sm text-on-surface-variant mt-1">
            Puedes asignar varios roles; el usuario podrá elegir con cuál iniciar sesión.
          </p>
        </div>

        <!-- Estado activo/inactivo — solo visible al editar -->
        <div v-if="editando" class="flex items-center justify-between p-4 rounded-xl border border-outline-variant bg-surface-container-low">
          <div>
            <p class="text-label-lg text-on-surface font-medium">Estado del usuario</p>
            <p class="text-body-sm text-on-surface-variant mt-0.5">
              {{ form.estadoUsuario ? 'El usuario puede iniciar sesión' : 'El usuario tiene el acceso bloqueado' }}
            </p>
          </div>
          <button
            type="button"
            class="relative inline-flex h-6 w-11 shrink-0 items-center rounded-full transition-colors duration-200 focus:outline-none"
            :class="form.estadoUsuario ? 'bg-primary' : 'bg-gray-300'"
            @click="form.estadoUsuario = !form.estadoUsuario"
          >
            <span
              class="inline-block h-4 w-4 transform rounded-full bg-white shadow transition-transform duration-200"
              :class="form.estadoUsuario ? 'translate-x-6' : 'translate-x-1'"
            />
          </button>
        </div>
      </form>

      <template #footer>
        <AppButton variant="outlined" @click="modal = false">Cancelar</AppButton>
        <AppButton :loading="guardando" @click="solicitarGuardar">{{ editando ? 'Actualizar' : 'Crear' }}</AppButton>
      </template>
    </AppModal>

    <!-- Modal de Confirmación Visual Personalizada -->
    <AppModal v-model="modalConfirmacion" :title="confirmConfig.titulo" size="sm">
      <div class="flex flex-col items-center text-center space-y-3 py-2">
        <div
          class="w-14 h-14 rounded-full flex items-center justify-center shrink-0 shadow-sm"
          :class="{
            'bg-amber-100 text-amber-600': confirmConfig.tipo === 'warning',
            'bg-error-container text-error': confirmConfig.tipo === 'danger',
            'bg-primary-container text-primary': confirmConfig.tipo === 'info'
          }"
        >
          <span class="material-symbols-outlined text-[32px]">{{ confirmConfig.icono }}</span>
        </div>

        <div class="space-y-1">
          <p class="text-body-md text-on-surface leading-relaxed">{{ confirmConfig.mensaje }}</p>
          <p v-if="confirmConfig.detalle" class="text-label-sm text-amber-800 bg-amber-50 p-3 rounded-xl border border-amber-200 text-left mt-2 font-medium">
            {{ confirmConfig.detalle }}
          </p>
        </div>
      </div>

      <template #footer>
        <AppButton variant="outlined" @click="modalConfirmacion = false">Cancelar</AppButton>
        <AppButton
          :variant="confirmConfig.tipo === 'danger' ? 'danger' : 'primary'"
          :loading="ejecutandoConfirmacion"
          @click="ejecutarAccionConfirmada"
        >
          {{ confirmConfig.textoBoton }}
        </AppButton>
      </template>
    </AppModal>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../../stores/auth.js'
import usuarioAPI   from '../../services/usuarioAPI.js'
import rolAPI       from '../../services/rolAPI.js'
import AppTable   from '../../components/ui/AppTable.vue'
import AppButton  from '../../components/ui/AppButton.vue'
import AppModal   from '../../components/ui/AppModal.vue'
import AppInput   from '../../components/ui/AppInput.vue'
import AppSelect  from '../../components/ui/AppSelect.vue'
import AppBadge   from '../../components/ui/AppBadge.vue'
import AppBanner  from '../../components/ui/AppBanner.vue'
import ButtonIcon from '../../components/ui/ButtonIcon.vue'

const auth     = useAuthStore()
const router   = useRouter()

const usuarios    = ref([])
const loading     = ref(false)
const error       = ref(null)
const errorBanner = ref('')
const exitoBanner = ref('')

const modal     = ref(false)
const editando  = ref(false)
const guardando = ref(false)

// Modal de confirmación visual
const modalConfirmacion      = ref(false)
const ejecutandoConfirmacion = ref(false)
const confirmConfig = reactive({
  titulo:     '',
  mensaje:    '',
  detalle:    '',
  icono:      'warning',
  tipo:       'warning',
  textoBoton: 'Confirmar',
  accion:     null,
})

const columnas = [
  { key: 'nombre', label: 'Usuario' },
  { key: 'numeroDocumentoUsuario', label: 'Documento' },
  { key: 'rol',    label: 'Rol' },
  { key: 'estado', label: 'Estado' },
]

const tiposDoc      = ['CC','TI','CE','PA','NIT']

const opcionesRoles = ref([])

const opcionesRolFiltro = computed(() => [
  { value: '', label: 'Todos' },
  ...opcionesRoles.value,
])

const filtroRol  = ref('')
const filtroEstado = ref('')
const opcionesEstado = [
  { value: '', label: 'Todos' },
  { value: 'Activo',   label: 'Activo' },
  { value: 'Inactivo', label: 'Inactivo' },
]

const usuariosFiltrados = computed(() => {
  let result = usuarios.value
  if (filtroRol.value) {
    const rol = Number(filtroRol.value)
    result = result.filter(u =>
      u.idsRoles?.map(Number).includes(rol) ||
      Number(u.idRol) === rol
    )
  }
  if (filtroEstado.value) {
    const esperado = filtroEstado.value === 'Activo'
    result = result.filter(u => Boolean(u.estadoUsuario) === esperado)
  }
  return result.map(u => ({
    ...u,
    nombreCompleto: [u.primerNombreUsuario, u.segundoNombreUsuario, u.primerApellidoUsuario, u.segundoApellidoUsuario].filter(Boolean).join(' ')
  }))
})

const form = reactive({
  primerNombreUsuario: '', segundoNombreUsuario: '',
  primerApellidoUsuario: '', segundoApellidoUsuario: '',
  emailUsuario: '', contrasenaUsuario: '',
  tipoDocumentoUsuario: '', numeroDocumentoUsuario: '',
  numeroTelefonoUsuario: '', idsRoles: [],
  estadoUsuario: true,
})

let editId        = null
let originalRoles = []
let originalEmail = null

const esEdicionPropia = computed(() => {
  if (!editando.value) return false
  return Number(editId) === Number(auth.idUsuario) ||
    originalEmail.toLowerCase() === (auth.user?.emailUsuario || '').toLowerCase()
})

const ADMIN_ID = computed(() => {
  const op = opcionesRoles.value.find(o => o.label.toLowerCase() === 'administrador')
  return op ? Number(op.value) : null
})

const INVITADO_ID = computed(() => {
  const op = opcionesRoles.value.find(o => o.label.toLowerCase() === 'invitado')
  return op ? Number(op.value) : null
})

function rolLabel(id) {
  if (id == null) return '—'
  const opcion = opcionesRoles.value.find(o => String(o.value) === String(id))
  return opcion?.label ?? '—'
}
function rolVariant(nombre) {
  return {
    'Super Administrador': 'error',
    Administrador: 'warning',
    Operador: 'info',
    Monitor: 'neutral',
    Invitado: 'neutral',
  }[nombre] ?? 'neutral'
}

function toggleRol(id) {
  if (form.idsRoles.length === 1 && form.idsRoles.includes(id)) {
    errorBanner.value = 'Debe asignar al menos un rol'
    return
  }
  const idx = form.idsRoles.indexOf(id)
  if (idx >= 0) form.idsRoles.splice(idx, 1)
  else form.idsRoles.push(id)
}

async function cargar() {
  loading.value = true; error.value = null
  try {
    const [usuariosResp, rolesResp] = await Promise.all([usuarioAPI.getAll(), rolAPI.getAll()])
    usuarios.value = usuariosResp.data
    opcionesRoles.value = (rolesResp.data || []).map(r => ({ value: String(r.idRol), label: r.nombreRol }))
  } catch (e) { error.value = e.response?.data?.error || 'Error al cargar usuarios' }
  finally { loading.value = false }
}

function abrirFormulario(u) {
  errorBanner.value = ''
  exitoBanner.value = ''
  editando.value    = !!u
  editId            = u?.idUsuario ?? null
  originalRoles     = (u?.idsRoles?.length
    ? u.idsRoles
    : (u?.idRol ? [u.idRol] : [])).map(Number)
  originalEmail     = u?.emailUsuario ?? ''

  Object.assign(form, {
    primerNombreUsuario: u?.primerNombreUsuario ?? '',
    segundoNombreUsuario: u?.segundoNombreUsuario ?? '',
    primerApellidoUsuario: u?.primerApellidoUsuario ?? '',
    segundoApellidoUsuario: u?.segundoApellidoUsuario ?? '',
    emailUsuario: u?.emailUsuario ?? '',
    contrasenaUsuario: '',
    tipoDocumentoUsuario: u?.tipoDocumentoUsuario ?? '',
    numeroDocumentoUsuario: u?.numeroDocumentoUsuario ?? '',
    numeroTelefonoUsuario: u?.numeroTelefonoUsuario ?? '',
    idsRoles: [...originalRoles],
    estadoUsuario: u?.estadoUsuario ?? true,
  })
  modal.value = true
}

function solicitarGuardar() {
  if (!form.idsRoles.length) {
    errorBanner.value = 'Debe asignar al menos un rol'
    return
  }
  const yaNoEsAdmin = editando.value && originalRoles.includes(ADMIN_ID.value) && !form.idsRoles.includes(ADMIN_ID.value)

  if (editando.value && yaNoEsAdmin) {
    confirmConfig.titulo     = esEdicionPropia.value ? '¿Reducir tus propios permisos?' : '¿Cambiar rol de Administrador?'
    confirmConfig.mensaje    = `Se quitará el rol de Administrador a ${form.emailUsuario}.`
    confirmConfig.detalle    = esEdicionPropia.value
      ? '⚠️ Perderás tus permisos de administración y tu sesión actual se cerrará inmediatamente al guardar.'
      : '⚠️ Estás reduciendo los privilegios de un Administrador en el sistema.'
    confirmConfig.icono      = 'warning'
    confirmConfig.tipo       = 'warning'
    confirmConfig.textoBoton = 'Sí, cambiar rol'
    confirmConfig.accion     = () => ejecutarGuardar()
    modalConfirmacion.value  = true
    return
  }

  ejecutarGuardar()
}

function rolesIguales(a, b) {
  const sorted = arr => [...arr].sort((x, y) => x - y)
  return JSON.stringify(sorted(a)) === JSON.stringify(sorted(b))
}

async function ejecutarGuardar() {
  guardando.value = true
  errorBanner.value = ''
  exitoBanner.value = ''

  // Mapeo y purificación DTO para Spring Boot
  const payload = {
    primerNombreUsuario: form.primerNombreUsuario?.trim() || '',
    segundoNombreUsuario: form.segundoNombreUsuario?.trim() || null,
    primerApellidoUsuario: form.primerApellidoUsuario?.trim() || '',
    segundoApellidoUsuario: form.segundoApellidoUsuario?.trim() || null,
    emailUsuario: form.emailUsuario?.trim() || '',
    tipoDocumentoUsuario: form.tipoDocumentoUsuario,
    numeroDocumentoUsuario: form.numeroDocumentoUsuario?.trim() || '',
    numeroTelefonoUsuario: form.numeroTelefonoUsuario?.trim() || null,
    idsRoles: form.idsRoles.map(Number),
    estadoUsuario: Boolean(form.estadoUsuario)
  }

  // Manejo de la contraseña según si es edición o creación
  if (form.contrasenaUsuario && form.contrasenaUsuario.trim() !== '') {
    payload.contrasenaUsuario = form.contrasenaUsuario
  } else if (!editando.value) {
    payload.contrasenaUsuario = form.contrasenaUsuario
  }

  try {
    if (editando.value) {
      await usuarioAPI.update(editId, payload)
    } else {
      await usuarioAPI.create(payload)
    }

    modal.value = false
    modalConfirmacion.value = false

    const cambioPropioRol   = editando.value && esEdicionPropia.value && !rolesIguales(originalRoles, form.idsRoles.map(Number))
    const seDesactivo       = !form.estadoUsuario

    if (editando.value && esEdicionPropia.value && (cambioPropioRol || seDesactivo)) {
      auth.logout()
      router.push('/login')
      return
    }

    exitoBanner.value = editando.value ? 'Usuario actualizado correctamente.' : 'Usuario creado correctamente.'
    await cargar()
  } catch (e) {
    errorBanner.value = e.response?.data?.message || e.response?.data?.error || 'Error 400: Revisa los campos obligatorios'
  } finally {
    guardando.value = false
  }
}

function solicitarToggleEstado(u) {
  const esUsuarioLogueado = Number(u.idUsuario) === Number(auth.idUsuario) ||
    u.emailUsuario.toLowerCase() === (auth.user?.emailUsuario || '').toLowerCase()

  confirmConfig.titulo     = u.estadoUsuario ? '¿Desactivar usuario?' : '¿Activar usuario?'
  confirmConfig.mensaje    = `¿Deseas ${u.estadoUsuario ? 'desactivar' : 'activar'} la cuenta del usuario ${u.emailUsuario}?`
  confirmConfig.detalle    = esUsuarioLogueado && u.estadoUsuario
    ? '⚠️ Estás a punto de desactivar tu propia cuenta. Tu sesión se cerrará de inmediato.'
    : ''
  confirmConfig.icono      = u.estadoUsuario ? 'block' : 'check_circle'
  confirmConfig.tipo       = u.estadoUsuario ? 'warning' : 'info'
  confirmConfig.textoBoton = u.estadoUsuario ? 'Sí, desactivar' : 'Sí, activar'
  confirmConfig.accion     = async () => {
    await usuarioAPI.cambiarEstado(u.idUsuario, !u.estadoUsuario)
    if (esUsuarioLogueado && u.estadoUsuario) {
      auth.logout()
      router.push('/login')
      return
    }
    exitoBanner.value = `Estado de ${u.emailUsuario} actualizado.`
    await cargar()
  }
  modalConfirmacion.value = true
}

function solicitarEliminar(u) {
  const esUsuarioLogueado = Number(u.idUsuario) === Number(auth.idUsuario) ||
    u.emailUsuario.toLowerCase() === (auth.user?.emailUsuario || '').toLowerCase()

  confirmConfig.titulo     = '¿Eliminar usuario?'
  confirmConfig.mensaje    = `¿Estás seguro de que deseas eliminar a ${u.emailUsuario}?`
  confirmConfig.detalle    = esUsuarioLogueado
    ? '⚠️ ADVERTENCIA: Estás a punto de eliminar TU PROPIA CUENTA. Tu sesión finalizará.'
    : 'Se aplicará un soft-delete: el usuario quedará oculto del listado y sin acceso, pero se conserva en la BD para poder restaurarlo.'
  confirmConfig.icono      = 'delete'
  confirmConfig.tipo       = 'danger'
  confirmConfig.textoBoton = 'Sí, eliminar'
  confirmConfig.accion     = async () => {
    await usuarioAPI.remove(u.idUsuario)
    if (esUsuarioLogueado) {
      auth.logout()
      router.push('/login')
      return
    }
    exitoBanner.value = `Usuario ${u.emailUsuario} eliminado.`
    await cargar()
  }
  modalConfirmacion.value = true
}

async function ejecutarAccionConfirmada() {
  if (!confirmConfig.accion) return
  ejecutandoConfirmacion.value = true
  try {
    await confirmConfig.accion()
    modalConfirmacion.value = false
  } catch (e) {
    errorBanner.value = e.response?.data?.error || 'Ocurrió un error al procesar la acción'
  } finally {
    ejecutandoConfirmacion.value = false
  }
}

onMounted(cargar)
</script>

<style scoped>
.fade-enter-active, .fade-leave-active { transition: opacity 0.25s; }
.fade-enter-from, .fade-leave-to { opacity: 0; }
</style>