<template>
  <div class="p-6 md:p-8 space-y-6 animate-fade-in">
    <div>
      <h1 class="text-headline-md text-on-surface font-semibold">Auditoría</h1>
      <p class="text-body-md text-on-surface-variant mt-1">Registro completo de acciones realizadas en el sistema.</p>
    </div>

    <AppTable
      title="Log de Auditoría"
      :columns="columnas"
      :rows="logs"
      :loading="loading"
      :error="error"
      row-key="idLogAuditoria"
      default-sort-key="idLogAuditoria"
      search-placeholder="Buscar por ID, usuario, acción, entidad, IP o detalle..."
      :search-fields="['idLogAuditoria','idUsuario','accionAuditoria','entidadAuditada','idEntidadAuditada','ipOrigenAuditoria','detalleAuditoria','fechaAuditoria']"
      :page-size="20"
      @retry="cargar"
    >
      <template #filters>
        <AppSelect v-model="filtros.accion" :options="acciones" placeholder="Acción" class="w-32" />
        <AppSelect v-model="filtros.entidad" :options="entidades" placeholder="Entidad" class="w-36" />
        <AppButton variant="outlined" size="sm" icon="filter_list" @click="cargar">Filtrar</AppButton>
      </template>

      <template #cell-accionAuditoria="{ value }">
        <AppBadge :variant="accionVariant(value)">{{ value }}</AppBadge>
      </template>

      <template #cell-detalleAuditoria="{ value }">
        <span class="text-body-sm text-on-surface-variant line-clamp-1 max-w-[200px]" :title="value">{{ value }}</span>
      </template>

      <template #cell-fechaAuditoria="{ value }">
        <span class="text-body-sm text-on-surface-variant whitespace-nowrap">{{ formatFecha(value) }}</span>
      </template>
    </AppTable>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import dashboardAPI from '../../services/dashboardAPI.js'
import AppTable  from '../../components/ui/AppTable.vue'
import AppSelect from '../../components/ui/AppSelect.vue'
import AppButton from '../../components/ui/AppButton.vue'
import AppBadge  from '../../components/ui/AppBadge.vue'

const logs    = ref([])
const loading = ref(false)
const error   = ref(null)
const filtros = reactive({ accion: '', entidad: '' })

const columnas = [
  { key: 'idLogAuditoria',   label: '#' },
  { key: 'idUsuario',        label: 'Usuario' },
  { key: 'accionAuditoria',  label: 'Acción' },
  { key: 'entidadAuditada',  label: 'Entidad' },
  { key: 'idEntidadAuditada',label: 'ID Entidad' },
  { key: 'ipOrigenAuditoria',label: 'IP' },
  { key: 'detalleAuditoria', label: 'Detalle' },
  { key: 'fechaAuditoria',   label: 'Fecha' },
]

const acciones  = ['CREATE','UPDATE','DELETE','LOGIN','LOGOUT']
const entidades = ['Evento','Participante','Usuario','Certificado','RegistroAsistencia']

function accionVariant(a) {
  return { CREATE:'success', UPDATE:'warning', DELETE:'error', LOGIN:'info', LOGOUT:'neutral' }[a] ?? 'neutral'
}
function formatFecha(f) {
  return f ? new Date(f).toLocaleString('es-ES', { day:'2-digit', month:'short', year:'numeric', hour:'2-digit', minute:'2-digit' }) : '—'
}

async function cargar() {
  loading.value = true; error.value = null
  const params = {}
  if (filtros.accion)  params.accion  = filtros.accion
  if (filtros.entidad) params.entidad = filtros.entidad
  try { logs.value = (await dashboardAPI.auditoria(params)).data }
  catch (e) { error.value = e.response?.data?.error || 'Error al cargar logs' }
  finally { loading.value = false }
}

onMounted(cargar)
</script>
