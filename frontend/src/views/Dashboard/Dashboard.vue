<template>
  <div class="p-6 md:p-8 space-y-8 animate-fade-in">
    <!-- Header -->
    <div>
      <h1 class="text-headline-md text-on-surface font-semibold">Panel de Control</h1>
      <p class="text-body-md text-on-surface-variant mt-1">Resumen general del sistema · {{ fechaHoy }}</p>
    </div>

    <!-- Stats cards -->
    <div v-if="dash.loading && !dash.resumen" class="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-6 gap-4">
      <div v-for="i in 6" :key="i" class="h-24 bg-surface-container rounded-xl animate-pulse" />
    </div>

    <div v-else class="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-6 gap-4">
      <div
        v-for="stat in stats"
        :key="stat.label"
        class="bg-white rounded-xl p-4 border border-outline-variant/20 shadow-elevation-1 flex flex-col gap-2"
      >
        <div class="flex items-center justify-between">
          <span
            class="material-symbols-outlined text-[22px]"
            :class="{
              'text-primary': stat.variant === 'primary' || !stat.variant,
              'text-success': stat.variant === 'success',
              'text-warning': stat.variant === 'warning',
              'text-error': stat.variant === 'error',
              'text-secondary': stat.variant === 'secondary'
            }"
          >
            {{ stat.icon }}
          </span>
        </div>
        <p class="text-headline-sm font-bold text-on-surface">{{ stat.value ?? '—' }}</p>
        <p class="text-body-sm text-on-surface-variant">{{ stat.label }}</p>
      </div>
    </div>

    <!-- Calendario -->
    <CalendarioEventos @abrir-evento="abrirEvento" />

    <!-- Gráficos -->
    <GraficosDashboard />

    <!-- Eventos con aforo -->
    <AppCard title="Eventos y Ocupación" subtitle="Porcentaje de aforo y asistencia por evento">
      <template #header-action>
        <div class="flex items-center gap-2">
          <AppSelect
            v-model="filtros.modalidad"
            :options="modalidades"
            placeholder="Modalidad"
            class="w-36"
          />
          <AppSelect
            v-model="filtros.estado"
            :options="estados"
            placeholder="Estado"
            class="w-36"
          />
          <AppButton variant="outlined" size="sm" icon="refresh" @click="cargarEventos" />
        </div>
      </template>

      <div v-if="dash.loading" class="flex justify-center py-12">
        <AppSpinner size="lg" class="text-primary" />
      </div>

      <div v-else-if="!eventosFiltrados.length" class="text-center py-12 text-on-surface-variant">
        <span class="material-symbols-outlined text-[48px] mb-2 block">event_busy</span>
        <p class="text-body-md">No hay eventos disponibles</p>
      </div>

      <AppTable
        v-else
        :columns="columnasOcupacion"
        :rows="eventosFiltrados"
        row-key="idEventos"
        default-sort-key="idEventos"
        :searchable="false"
      >
        <template #cell-nombreEvento="{ row }">
          <p class="text-label-lg text-on-surface font-medium">{{ row.nombreEvento }}</p>
          <p class="text-body-sm text-on-surface-variant">{{ formatFecha(row.fechaInicioEvento) }}</p>
        </template>

        <template #cell-tipoEvento="{ row }">
          <AppBadge variant="info">{{ row.tipoEvento || 'General' }}</AppBadge>
          <span class="ml-2 text-body-sm text-on-surface-variant">{{ row.modalidadEvento }}</span>
        </template>

        <template #cell-estadoEvento="{ value }">
          <AppBadge :variant="estadoVariant(value)" :dot="true">{{ value }}</AppBadge>
        </template>

        <template #cell-aforo="{ row }">
          <div v-if="row.aforoMaximo && row.aforoMaximo > 0" class="space-y-1 min-w-[150px]">
            <div class="flex justify-between text-label-sm">
              <span class="text-on-surface-variant font-medium">{{ row.registrados }} / {{ row.aforoMaximo }}</span>
              <span class="font-semibold text-primary">{{ Math.min(100, row.porcentajeAforo || 0) }}%</span>
            </div>
            <div class="h-2 rounded-full bg-surface-container-high overflow-hidden">
              <div
                class="h-full rounded-full bg-primary transition-all duration-500"
                :style="{ width: Math.min(100, row.porcentajeAforo || 0) + '%' }"
              />
            </div>
          </div>
          <div v-else class="text-body-sm text-on-surface-variant font-medium">
            {{ row.registrados }} {{ row.registrados === 1 ? 'inscrito' : 'inscritos' }}
            <span class="text-label-sm text-outline">(Sin límite)</span>
          </div>
        </template>

        <template #cell-asistencia="{ row }">
          <div v-if="row.registrados > 0" class="space-y-1 min-w-[150px]">
            <div class="flex justify-between text-label-sm">
              <span class="text-on-surface-variant font-medium">{{ row.asistieron }} / {{ row.registrados }}</span>
              <span class="font-semibold text-emerald-600">{{ Math.min(100, row.porcentajeAsistencia || 0) }}%</span>
            </div>
            <div class="h-2 rounded-full bg-surface-container-high overflow-hidden">
              <div
                class="h-full rounded-full bg-emerald-500 transition-all duration-500"
                :style="{ width: Math.min(100, row.porcentajeAsistencia || 0) + '%' }"
              />
            </div>
          </div>
          <div v-else class="text-body-sm text-on-surface-variant font-medium">0 asistencias</div>
        </template>
      </AppTable>
    </AppCard>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useDashboardStore } from '../../stores/dashboard.js'
import AppCard    from '../../components/ui/AppCard.vue'
import AppTable   from '../../components/ui/AppTable.vue'
import AppBadge   from '../../components/ui/AppBadge.vue'
import AppButton  from '../../components/ui/AppButton.vue'
import AppSelect  from '../../components/ui/AppSelect.vue'
import AppSpinner from '../../components/ui/AppSpinner.vue'
import CalendarioEventos from '../../components/dashboard/CalendarioEventos.vue'
import GraficosDashboard from '../../components/dashboard/GraficosDashboard.vue'

const router = useRouter()
const dash   = useDashboardStore()

// Columnas de la tabla de ocupación (celdas personalizadas vía slots)
const columnasOcupacion = [
  { key: 'nombreEvento', label: 'Evento' },
  { key: 'tipoEvento', label: 'Tipo / Modalidad' },
  { key: 'estadoEvento', label: 'Estado' },
  { key: 'aforo', label: 'Aforo', sortable: false },
  { key: 'asistencia', label: 'Asistencia', sortable: false },
]

// Filtros inicializados con 'Todas' y 'Todos'
const filtros = ref({ modalidad: 'Todas', estado: 'Todos' })

// Opciones con 'Todas'/'Todos' como primera opción y estados en formato capitalizado
const modalidades = ['Todas', 'Presencial', 'Virtual', 'Híbrido']
const estados     = ['Todos', 'Activo', 'Finalizado', 'Cancelado']

const fechaHoy = new Date().toLocaleDateString('es-ES', { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' })

const stats = computed(() => {
  const r = dash.resumen || {}
  return [
    { icon: 'event',             label: 'Eventos',       value: r.totalEventos,      variant: 'primary'   },
    { icon: 'event_available',   label: 'Activos',       value: r.eventosActivos,    variant: 'success'   },
    { icon: 'group',             label: 'Participantes', value: r.totalParticipantes, variant: 'secondary' },
    { icon: 'manage_accounts',   label: 'Usuarios',      value: r.totalUsuarios,      variant: 'warning'   },
    { icon: 'workspace_premium', label: 'Certificados',  value: r.totalCertificados,  variant: 'primary'   },
    { icon: 'how_to_reg',        label: 'Registros',     value: r.totalRegistros,     variant: 'success'   },
  ]
})

function estadoVariant(estado) {
  const estadoUpper = estado?.toUpperCase()
  return { ACTIVO: 'success', FINALIZADO: 'neutral', CANCELADO: 'error' }[estadoUpper] ?? 'neutral'
}

function formatFecha(f) {
  return f ? new Date(f).toLocaleDateString('es-ES', { day: '2-digit', month: 'short', year: 'numeric' }) : '—'
}

function abrirEvento(id) {
  router.push(`/app/eventos/${id}`)
}

// Cargar TODOS los eventos sin filtros al backend
async function cargarEventos() {
  await dash.fetchEventos({})
}

// Filtrado LOCAL con comparación case-insensitive
const normalizar = (valor) => String(valor ?? '')
  .trim()
  .toLowerCase()
  .normalize('NFD')
  .replace(/[\u0300-\u036f]/g, '')

const eventosFiltrados = computed(() => {
  const modalidad = normalizar(filtros.value.modalidad)
  const estado    = normalizar(filtros.value.estado)
  return dash.eventos.filter(e => {
    // Filtrar por modalidad (ignorando mayúsculas/minúsculas y acentos)
    if (filtros.value.modalidad && filtros.value.modalidad !== 'Todas') {
      if (normalizar(e.modalidadEvento) !== modalidad) {
        return false
      }
    }
    // Filtrar por estado (ignorando mayúsculas/minúsculas y acentos)
    if (filtros.value.estado && filtros.value.estado !== 'Todos') {
      if (normalizar(e.estadoEvento) !== estado) {
        return false
      }
    }
    return true
  })
})

onMounted(async () => {
  await Promise.all([dash.fetchResumen(), cargarEventos()])
})
</script>