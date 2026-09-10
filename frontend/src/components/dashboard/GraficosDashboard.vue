<template>
  <div class="grid grid-cols-1 gap-6 lg:grid-cols-2">
    <AppCard title="Asistencia por evento">
      <template #header-action>
        <div class="flex items-center gap-2">
          <AppSelect v-model="filtroAnio" :options="anios" class="w-28" />
          <AppSelect v-model="filtroMes" :options="meses" class="w-32" />
        </div>
      </template>
      <div class="h-[28rem]">
        <Bar v-if="barData && barData.labels.length" :data="barData" :options="barOptions" />
        <div
          v-else-if="datosBrutos"
          class="h-full flex flex-col items-center justify-center gap-2 text-on-surface-variant"
        >
          <span class="material-symbols-outlined text-[40px] opacity-60">bar_chart</span>
          <p class="text-body-md">No hay registros para estas fechas</p>
        </div>
      </div>
    </AppCard>
    <AppCard title="Registros por mes">
      <template #header-action>
        <div class="flex items-center gap-2">
          <AppSelect v-model="filtroAnio" :options="anios" class="w-28" />
          <AppSelect v-model="filtroMes" :options="meses" class="w-32" />
        </div>
      </template>
      <div class="h-[24rem]">
        <Line v-if="lineData && lineData.labels.length" :data="lineData" :options="sinLeyenda" />
        <div
          v-else-if="datosBrutos"
          class="h-full flex flex-col items-center justify-center gap-2 text-on-surface-variant"
        >
          <span class="material-symbols-outlined text-[40px] opacity-60">show_chart</span>
          <p class="text-body-md">No hay registros para estas fechas</p>
        </div>
      </div>
    </AppCard>
    <AppCard title="Eventos por modalidad">
      <div class="h-[24rem]"><Doughnut v-if="modalidadData" :data="modalidadData" :options="doughnutOptions" /></div>
    </AppCard>
    <AppCard title="Eventos por tipo">
      <div class="h-[24rem]"><Doughnut v-if="tipoData" :data="tipoData" :options="doughnutOptions" /></div>
    </AppCard>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import {
  Chart as ChartJS, Title, Tooltip, Legend, BarElement, CategoryScale,
  LinearScale, ArcElement, PointElement, LineElement, Filler,
} from 'chart.js'
import { Bar, Line, Doughnut } from 'vue-chartjs'
import dashboardAPI from '../../services/dashboardAPI.js'
import AppCard from '../../components/ui/AppCard.vue'
import AppSelect from '../../components/ui/AppSelect.vue'

ChartJS.register(Title, Tooltip, Legend, BarElement, CategoryScale, LinearScale, ArcElement, PointElement, LineElement, Filler)

// Plugin para mostrar el valor encima de cada barra de asistencia
const valorEnBarra = {
  id: 'valorEnBarra',
  afterDatasetsDraw(chart) {
    if (chart.config.type !== 'bar') return
    const { ctx } = chart
    const meta = chart.getDatasetMeta(0)
    if (!meta?.data || !meta.data.length) return
    ctx.save()
    ctx.font = '600 12px Inter, system-ui, sans-serif'
    ctx.textAlign = 'center'
    ctx.textBaseline = 'bottom'
    ctx.fillStyle = '#1f2937'
    meta.data.forEach((bar, i) => {
      const valor = chart.data.datasets[0]?.data?.[i]
      if (valor == null || Number(valor) <= 0) return
      ctx.fillText(new Intl.NumberFormat('es-CO').format(valor), bar.x, bar.y - 2)
    })
    ctx.restore()
  },
}
ChartJS.register(valorEnBarra)

const COLORES = ['#4338ca', '#7c3aed', '#0ea5e9', '#10b981', '#f59e0b', '#ef4444', '#ec4899']

const sinLeyenda = { responsive: true, maintainAspectRatio: false, plugins: { legend: { display: false } } }

// Opciones para gráficos Doughnut con porcentajes
const doughnutOptions = {
  responsive: true,
  maintainAspectRatio: false,
  cutout: '65%',
  plugins: {
    legend: { 
      position: 'bottom',
      labels: {
        padding: 15,
        usePointStyle: true,
        pointStyle: 'rectRounded'
      }
    },
    tooltip: {
      backgroundColor: 'rgba(0, 0, 0, 0.8)',
      titleFont: { size: 13, weight: 'bold' },
      bodyFont: { size: 12 },
      padding: 12,
      cornerRadius: 8,
      callbacks: {
        label: (context) => {
          const label = context.label || ''
          const value = context.parsed
          const total = context.dataset.data.reduce((acc, val) => acc + val, 0)
          const percentage = ((value / total) * 100).toFixed(1)
          return `${label}: ${value} eventos (${percentage}%)`
        }
      }
    }
  }
}

// Opciones mejoradas para la gráfica de barras
const barOptions = {
  responsive: true,
  maintainAspectRatio: false,
  layout: {
    padding: { top: 24, right: 10, bottom: 32, left: 10 },
  },
  plugins: {
    legend: { display: false },
    tooltip: {
      enabled: true,
      backgroundColor: 'rgba(0, 0, 0, 0.8)',
      titleFont: { size: 13, weight: 'bold' },
      bodyFont: { size: 12 },
      padding: 12,
      cornerRadius: 8,
      callbacks: {
        title: (context) => {
          const index = context[0]?.dataIndex
          const eventName = barData.value?.eventNames?.[index] || ''
          return eventName
        },
        label: (context) => {
          return `Participantes: ${context.parsed.y}`
        }
      }
    }
  },
  scales: {
    x: {
      ticks: {
        autoSkip: false,
        rotation: 0,
        maxRotation: 0,
        minRotation: 0,
        font: {
          size: 11,
          weight: 'bold'
        },
        padding: 14,
      },
      grid: {
        display: false
      }
    },
    y: {
      beginAtZero: true,
      ticks: {
        stepSize: 20
      }
    }
  }
}

const datosBrutos = ref(null)
const modalidadData = ref(null)
const tipoData = ref(null)

const MESES = ['Enero', 'Febrero', 'Marzo', 'Abril', 'Mayo', 'Junio',
  'Julio', 'Agosto', 'Septiembre', 'Octubre', 'Noviembre', 'Diciembre']

const filtroAnio = ref(String(new Date().getFullYear()))
const filtroMes  = ref('Todos')
const meses = ['Todos', ...MESES]

const anios = computed(() => {
  const set = new Set()
  ;(datosBrutos.value?.fechasInicioEvento || []).forEach(f => { if (f) set.add(new Date(f).getFullYear()) })
  return ['Todos', ...[...set].sort((a, b) => b - a)]
})

const barData = computed(() => {
  const bruto = datosBrutos.value
  if (!bruto) return null
  const fechas = bruto.fechasInicioEvento || []
  const indices = []
  bruto.nombresEventos.forEach((_, i) => {
    const fecha = fechas[i]
    const d = fecha ? new Date(fecha) : null
    if (filtroAnio.value !== 'Todos' && (!d || d.getFullYear() !== Number(filtroAnio.value))) return
    if (filtroMes.value !== 'Todos' && (!d || d.getMonth() !== MESES.indexOf(filtroMes.value))) return
    indices.push(i)
  })
  return {
    labels: indices.map(i => {
      const id = bruto.eventIds?.[i] || bruto.idEventos?.[i] || i + 1
      return `#${id}`
    }),
    datasets: [{
      label: 'Asistentes',
      data: indices.map(i => bruto.asistenciaPorEvento[i]),
      backgroundColor: COLORES,
      borderRadius: 8,
      barThickness: 20,
    }],
    eventNames: indices.map(i => bruto.nombresEventos[i]),
  }
})

function mapDoughnut(obj) {
  return { labels: Object.keys(obj), datasets: [{ data: Object.values(obj), backgroundColor: COLORES }] }
}

const lineData = computed(() => {
  const meses = datosBrutos.value?.meses || []
  const registros = datosBrutos.value?.registrosPorMes || []
  const indices = []
  meses.forEach((m, i) => {
    const [anio, mes] = String(m).split('-')
    if (filtroAnio.value !== 'Todos' && anio !== String(filtroAnio.value)) return
    if (filtroMes.value !== 'Todos' && Number(mes) !== MESES.indexOf(filtroMes.value) + 1) return
    indices.push(i)
  })
  if (!indices.length) return { labels: [], datasets: [{ label: 'Registros', data: [] }] }
  return {
    labels: indices.map(i => meses[i]),
    datasets: [{
      label: 'Registros',
      data: indices.map(i => registros[i]),
      borderColor: '#4338ca',
      backgroundColor: 'rgba(67, 56, 202, 0.15)',
      fill: true, tension: 0.4, pointRadius: 4,
    }],
  }
})

onMounted(async () => {
  try {
    const { data } = await dashboardAPI.graficos()

    datosBrutos.value = {
      nombresEventos: data.nombresEventos || [],
      asistenciaPorEvento: data.asistenciaPorEvento || [],
      fechasInicioEvento: data.fechasInicioEvento || [],
      eventIds: data.eventIds,
      idEventos: data.idEventos,
      meses: data.meses || [],
      registrosPorMes: data.registrosPorMes || [],
    }
    modalidadData.value = mapDoughnut(data.eventosPorModalidad || {})
    tipoData.value      = mapDoughnut(data.eventosPorTipo || {})
  } catch (e) { console.error(e) }
})
</script>