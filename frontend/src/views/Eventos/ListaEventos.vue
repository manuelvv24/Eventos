<template>
  <div class="p-6 md:p-8 space-y-6 animate-fade-in">
    <div class="flex items-center justify-between">
      <div>
        <h1 class="text-headline-md text-on-surface font-semibold">Eventos</h1>
        <p class="text-body-md text-on-surface-variant mt-1">Gestiona todos los eventos del sistema.</p>
      </div>
      <AppButton v-if="canEdit" icon="add" :to="'/app/eventos/nuevo'">Crear Evento</AppButton>
    </div>

    <!-- Banners de Notificación -->
    <AppBanner v-model="errorBanner" type="error" />
    <AppBanner v-model="exitoBanner" type="success" />

    <AppTable
      title="Lista de Eventos"
      :columns="columnas"
      :rows="filasFiltradas"
      :loading="store.loading"
      :error="store.error"
      row-key="idEventos"
      default-sort-key="idEventos"
      search-placeholder="Buscar evento..."
      :search-fields="['idEventos','nombreEvento','tipoEvento','modalidadEvento','descripcionEvento','lugarEvento','estadoEvento']"
      @retry="store.fetchAll()"
    >
      <template #filters>
        <AppSelect v-model="filtroEstado" :options="opcionesEstado" :placeholder="null" class="w-36" />
        <AppSelect v-model="filtroTipo" :options="opcionesTipo" placeholder="Tipo" class="w-36" />
        <AppSelect v-model="filtroModalidad" :options="opcionesModalidad" placeholder="Modalidad" class="w-36" />
      </template>

      <template #cell-nombreEvento="{ row }">
        <div class="flex items-center gap-2">
          <div class="w-8 h-8 rounded-lg bg-primary-container flex items-center justify-center shrink-0">
            <span class="material-symbols-outlined text-primary text-[18px]">event</span>
          </div>
          <RouterLink :to="`/app/eventos/${row.idEventos}`" class="text-label-lg text-on-surface hover:text-primary hover:underline font-medium">
            {{ row.nombreEvento }}
          </RouterLink>
        </div>
      </template>

      <template #cell-tipoEvento="{ value }">
        <AppBadge variant="info">{{ value || 'General' }}</AppBadge>
      </template>

      <template #cell-modalidadEvento="{ value }">
        <span class="text-body-sm text-on-surface-variant">{{ value }}</span>
      </template>

      <template #cell-estadoEvento="{ value }">
        <AppBadge :variant="estadoVariant(value)" :dot="true">{{ value }}</AppBadge>
      </template>

      <template #cell-fechaInicioEvento="{ value }">
        <span class="text-body-sm text-on-surface-variant whitespace-nowrap">{{ formatFecha(value) }}</span>
      </template>

      <template #actions="{ row }">
        <ButtonIcon icon="visibility" title="Ver detalles" :to="`/app/eventos/${row.idEventos}`" />
        <ButtonIcon
          v-if="canEditRow(row)"
          icon="edit"
          title="Editar"
          :to="`/app/eventos/${row.idEventos}/editar`"
        />
        <ButtonIcon
          v-if="canInhabilitarRow(row)"
          icon="block"
          title="Inhabilitar Evento"
          variant="danger"
          @click="inhabilitar(row)"
        />
        <ButtonIcon v-if="canDelete" icon="delete" title="Eliminar" variant="danger" @click="eliminar(row)" />
      </template>
    </AppTable>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useEventoStore } from '../../stores/evento.js'
import { useAuthStore, ROLES } from '../../stores/auth.js'
import AppTable  from '../../components/ui/AppTable.vue'
import AppButton from '../../components/ui/AppButton.vue'
import AppBadge  from '../../components/ui/AppBadge.vue'
import AppSelect from '../../components/ui/AppSelect.vue'
import AppBanner from '../../components/ui/AppBanner.vue'
import ButtonIcon from '../../components/ui/ButtonIcon.vue'
import { useConfirm } from '../../composables/useConfirm.js'

const store = useEventoStore()
const auth  = useAuthStore()
const { confirm } = useConfirm()

const errorBanner = ref('')
const exitoBanner = ref('')

function canEditRow(row) {
  if (!auth.hasRole(ROLES.ADMIN, ROLES.OPERADOR)) return false
  const estado = (row?.estadoEvento || '').toUpperCase()
  if (estado === 'FINALIZADO' || estado === 'CANCELADO') return false
  const fechaTermino = row?.fechaFinEvento || row?.fechaInicioEvento
  if (fechaTermino && new Date(fechaTermino) < new Date()) return false
  return true
}

function canInhabilitarRow(row) {
  if (!auth.hasRole(ROLES.ADMIN, ROLES.OPERADOR)) return false
  const estado = (row?.estadoEvento || '').toUpperCase()
  return estado !== 'CANCELADO' && estado !== 'FINALIZADO'
}

async function inhabilitar(evento) {
  errorBanner.value = ''
  exitoBanner.value = ''
  const ok = await confirm({
    title: 'Inhabilitar evento',
    message: `¿Estás seguro de inhabilitar / cancelar el evento "${evento.nombreEvento}"? Los participantes inscritos serán notificados.`,
    confirmText: 'Inhabilitar',
    danger: true,
  })
  if (!ok) return
  try {
    await store.cancel(evento.idEventos)
    exitoBanner.value = `El evento "${evento.nombreEvento}" ha sido inhabilitado correctamente.`
  } catch (e) {
    errorBanner.value = e.response?.data?.error || 'Error al inhabilitar el evento. Inténtalo de nuevo.'
  }
}

const canEdit   = computed(() => auth.hasRole(ROLES.ADMIN, ROLES.OPERADOR))
const canDelete = computed(() => auth.hasRole(ROLES.ADMIN))

const filtroEstado  = ref('')
const opcionesEstado= [
  { value: '', label: 'Todos' },
  { value: 'Activo', label: 'Activo' },
  { value: 'Publicado', label: 'Publicado' },
  { value: 'Borrador', label: 'Borrador' },
  { value: 'Finalizado', label: 'Finalizado' },
  { value: 'Cancelado', label: 'Cancelado' },
]

const filtroTipo  = ref('')
const opcionesTipo = [
  { value: '', label: 'Todos' },
  ...['Conferencia', 'Seminario', 'Congreso', 'Taller', 'Capacitación',
     'Curso', 'Charla', 'Webinar', 'Torneo', 'Networking',
     'Feria', 'Cultural', 'Deportivo', 'Bienestar', 'General']
    .map(t => ({ value: t, label: t })),
]

const filtroModalidad  = ref('')
const opcionesModalidad = ['', 'Presencial', 'Virtual', 'Híbrido'].map(m => ({
  value: m,
  label: m || 'Todos',
}))

const norm = s => (s || '').toLowerCase().normalize('NFD').replace(/[\u0300-\u036f]/g, '')

const filasFiltradas = computed(() => {
  const estado     = norm(filtroEstado.value)
  const tipo       = norm(filtroTipo.value)
  const modalidad  = norm(filtroModalidad.value)
  return store.items.filter(e => {
    if (estado && norm(e.estadoEvento) !== estado) return false
    if (tipo) {
      const tipoEv = e.tipoEvento || 'General'
      if (norm(tipoEv) !== tipo) return false
    }
    if (modalidad && norm(e.modalidadEvento) !== modalidad) return false
    return true
  })
})

const columnas = [
  { key: 'idEventos',        label: '#',         cellClass: 'text-on-surface-variant' },
  { key: 'nombreEvento',     label: 'Evento' },
  { key: 'tipoEvento',       label: 'Tipo' },
  { key: 'modalidadEvento',  label: 'Modalidad' },
  { key: 'fechaInicioEvento',label: 'Fecha inicio' },
  { key: 'estadoEvento',     label: 'Estado' },
]

function estadoVariant(s) {
  return {
    ACTIVO:'success', PUBLICADO:'success', BORRADOR:'neutral',
    FINALIZADO:'info', CANCELADO:'error',
  }[s?.toUpperCase()] ?? 'neutral'
}

function formatFecha(f) {
  return f ? new Date(f).toLocaleDateString('es-ES', { day:'2-digit', month:'short', year:'numeric' }) : '—'
}

async function eliminar(evento) {
  errorBanner.value = ''
  exitoBanner.value = ''
  const ok = await confirm({
    title: 'Eliminar evento',
    message: `¿Está seguro de eliminar el evento "${evento.nombreEvento}"? Se moverá a la papelera y podrá restablecerse desde el panel del Super Administrador.`,
    confirmText: 'Sí, eliminar',
    danger: true,
  })
  if (!ok) return
  try {
    await store.remove(evento.idEventos)
    exitoBanner.value = `El evento "${evento.nombreEvento}" ha sido eliminado.`
  } catch (e) {
    errorBanner.value = e.response?.data?.error || 'Error al eliminar el evento'
  }
}

onMounted(() => store.fetchAll())
</script>
