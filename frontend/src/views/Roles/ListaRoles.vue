<template>
  <div class="p-6 md:p-8 space-y-6 animate-fade-in">
    <div class="flex items-center justify-between">
      <div>
        <h1 class="text-headline-md text-on-surface font-semibold">Roles</h1>
        <p class="text-body-md text-on-surface-variant mt-1">Configura los roles y permisos del sistema.</p>
      </div>
      <AppButton icon="add" @click="abrirModal(null)">Nuevo Rol</AppButton>
    </div>

    <AppTable
      title="Roles del sistema"
      :columns="columnas"
      :rows="roles"
      :loading="loading"
      :error="error"
      row-key="idRol"
      default-sort-key="idRol"
      :searchable="false"
      @retry="cargar"
    >
      <template #cell-nombreRol="{ value }">
        <AppBadge :variant="rolVariant(value)">{{ value }}</AppBadge>
      </template>
      <template #actions="{ row }">
        <ButtonIcon icon="edit" title="Editar" @click="abrirModal(row)" />
        <ButtonIcon icon="delete" title="Eliminar" variant="danger" @click="eliminar(row)" />
      </template>
    </AppTable>

    <AppBanner v-model="errorBanner" type="error" />
    <AppBanner v-model="exito" type="success" />

    <AppModal v-model="modal" :title="editando ? 'Editar Rol' : 'Nuevo Rol'">
      <form class="space-y-4" @submit.prevent="guardar">
        <AppInput v-model="form.nombreRol" label="Nombre del rol" required placeholder="Ej: Coordinador" />
        <AppInput v-model="form.descripcion" label="Descripción" placeholder="Descripción del rol" type="textarea" :rows="3" />
      </form>
      <template #footer>
        <AppButton variant="outlined" @click="modal = false">Cancelar</AppButton>
        <AppButton :loading="guardando" @click="guardar">{{ editando ? 'Actualizar' : 'Crear' }}</AppButton>
      </template>
    </AppModal>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import rolAPI     from '../../services/rolAPI.js'
import AppTable   from '../../components/ui/AppTable.vue'
import AppButton  from '../../components/ui/AppButton.vue'
import AppModal   from '../../components/ui/AppModal.vue'
import AppInput   from '../../components/ui/AppInput.vue'
import AppBadge   from '../../components/ui/AppBadge.vue'
import AppBanner  from '../../components/ui/AppBanner.vue'
import ButtonIcon from '../../components/ui/ButtonIcon.vue'
import { useConfirm } from '../../composables/useConfirm.js'

const { confirm } = useConfirm()

const roles    = ref([])
const loading  = ref(false)
const error    = ref(null)
const modal    = ref(false)
const editando = ref(false)
const guardando= ref(false)
const errorBanner = ref('')
const exito    = ref('')
let   editId   = null

const columnas = [
  { key: 'idRol',      label: '#' },
  { key: 'nombreRol',  label: 'Nombre' },
  { key: 'descripcion',label: 'Descripción' },
]
const form = reactive({ nombreRol: '', descripcion: '' })

function rolVariant(n) {
  return { 'Super Administrador':'error', Administrador:'warning', Operador:'info', Monitor:'neutral', Invitado:'neutral' }[n] ?? 'neutral'
}

async function cargar() {
  loading.value = true; error.value = null
  try { roles.value = (await rolAPI.getAll()).data }
  catch (e) { error.value = e.response?.data?.error || 'Error al cargar roles' }
  finally { loading.value = false }
}

function abrirModal(r) {
  editando.value = !!r; editId = r?.idRol ?? null
  Object.assign(form, { nombreRol: r?.nombreRol ?? '', descripcion: r?.descripcion ?? '' })
  modal.value = true
}

async function guardar() {
  guardando.value = true
  try {
    if (editando.value) await rolAPI.update(editId, form)
    else await rolAPI.create(form)
    modal.value = false
    exito.value = editando.value ? 'Rol actualizado correctamente.' : 'Rol creado correctamente.'
    errorBanner.value = ''
    await cargar()
  } catch (e) {
    errorBanner.value = e.response?.data?.error || 'Error al guardar el rol'
  }
  finally { guardando.value = false }
}

async function eliminar(r) {
  const ok = await confirm({
    title: 'Eliminar rol',
    message: `¿Eliminar el rol "${r.nombreRol}"? Los usuarios con este rol perderán sus permisos.`,
    confirmText: 'Sí, eliminar',
    danger: true,
  })
  if (!ok) return
  try {
    await rolAPI.remove(r.idRol)
    exito.value = `El rol "${r.nombreRol}" ha sido eliminado.`
    errorBanner.value = ''
    await cargar()
  } catch (e) {
    errorBanner.value = e.response?.data?.error || 'No se puede eliminar: el rol está en uso'
  }
}

onMounted(cargar)
</script>
