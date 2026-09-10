<template>
  <AppCard title="Calendario de eventos" subtitle="Navega mes a mes y haz clic en un evento para ver detalles">
    <!-- Navegación de mes -->
    <div class="mb-4 flex items-center justify-between">
      <ButtonIcon icon="chevron_left" title="Mes anterior" class="!rounded-full" @click="cambiarMes(-1)" />
      <p class="text-title-md font-semibold text-on-surface capitalize">{{ tituloMes }}</p>
      <ButtonIcon icon="chevron_right" title="Mes siguiente" class="!rounded-full" @click="cambiarMes(1)" />
    </div>

    <!-- Días de la semana -->
    <div class="mb-1 grid grid-cols-7 gap-1 text-center text-label-md font-semibold text-on-surface-variant">
      <div v-for="d in NOMBRES_DIA" :key="d">{{ d }}</div>
    </div>

    <!-- Calendario -->
    <div class="grid grid-cols-7 gap-1">
      <div
        v-for="(celda, i) in celdas"
        :key="i"
        class="min-h-[96px] rounded-lg border p-1.5"
        :class="celda ? 'border-outline-variant/20 bg-white' : 'border-transparent'"
      >
        <template v-if="celda">
          <p
            class="flex h-6 w-6 items-center justify-center rounded-full text-body-sm font-medium"
            :class="esHoy(celda) ? 'bg-primary text-white' : 'text-on-surface-variant'"
          >
            {{ celda.getDate() }}
          </p>
          <div class="mt-1 space-y-1">
            <template v-for="(ev, idx) in eventosDelDiaPaginados(celda)" :key="ev.idEventos">
              <button
                v-if="idx < eventosPorCelda"
                class="w-full truncate rounded-md px-1.5 py-0.5 text-left text-[11px] font-medium text-white transition hover:opacity-80"
                :class="colorEstado(ev.estadoEvento)"
                :title="`${ev.nombreEvento} (${ev.estadoEvento})`"
                @click="$emit('abrir-evento', ev.idEventos)"
              >
                {{ ev.nombreEvento }}
              </button>
            </template>
            
            <!-- Badge de más eventos -->
            <button
              v-if="tieneMasEventos(celda)"
              class="w-full rounded-md bg-surface-variant px-1.5 py-0.5 text-left text-[11px] font-medium text-on-surface-variant transition hover:bg-surface-variant/80"
              @click="verMasEventos(celda)"
            >
              +{{ eventosDelDia(celda).length - eventosPorCelda }} más
            </button>
          </div>
        </template>
      </div>
    </div>

    <!-- Modal para ver todos los eventos del día -->
    <div 
      v-if="diaSeleccionado"
      class="fixed inset-0 z-50 flex items-center justify-center bg-black/50"
      @click="diaSeleccionado = null"
    >
      <div class="max-h-[80vh] w-[90%] max-w-2xl overflow-auto rounded-2xl bg-white p-6 shadow-2xl" @click.stop>
        <div class="mb-4 flex items-center justify-between">
          <h3 class="text-title-lg font-semibold text-on-surface">
            Eventos del {{ formatearFecha(diaSeleccionado) }}
          </h3>
          <ButtonIcon icon="close" title="Cerrar" class="!rounded-full" @click="diaSeleccionado = null" />
        </div>
        <div class="space-y-2">
          <div
            v-for="ev in eventosDelDia(diaSeleccionado)"
            :key="ev.idEventos"
            class="flex items-center justify-between rounded-lg border border-outline-variant/20 p-3 hover:bg-surface-variant/30"
          >
            <div>
              <p class="font-medium text-on-surface">{{ ev.nombreEvento }}</p>
              <p class="text-body-sm text-on-surface-variant">{{ ev.estadoEvento }}</p>
            </div>
            <button
              class="rounded-lg bg-primary px-4 py-2 text-body-sm font-medium text-white transition hover:opacity-90"
              @click="$emit('abrir-evento', ev.idEventos); diaSeleccionado = null"
            >
              Ver detalles
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- Leyenda de colores -->
    <div class="mt-4 flex items-center justify-center gap-6 border-t border-outline-variant/20 pt-4">
      <div class="flex items-center gap-2">
        <span class="h-3 w-3 rounded bg-primary"></span>
        <span class="text-body-sm text-on-surface-variant">Activo</span>
      </div>
      <div class="flex items-center gap-2">
        <span class="h-3 w-3 rounded bg-outline"></span>
        <span class="text-body-sm text-on-surface-variant">Finalizado</span>
      </div>
      <div class="flex items-center gap-2">
        <span class="h-3 w-3 rounded bg-error"></span>
        <span class="text-body-sm text-on-surface-variant">Cancelado</span>
      </div>
    </div>
  </AppCard>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import eventoAPI from '../../services/eventoAPI.js'
import AppCard from '../../components/ui/AppCard.vue'
import ButtonIcon from '../../components/ui/ButtonIcon.vue'

defineEmits(['abrir-evento'])

const NOMBRES_DIA = ['Lun', 'Mar', 'Mié', 'Jue', 'Vie', 'Sáb', 'Dom']

const hoy = new Date()
const anio = ref(hoy.getFullYear())
const mes = ref(hoy.getMonth())
const eventos = ref([])
const eventosPorCelda = ref(3) // Máximo 3 eventos visibles por día
const diaSeleccionado = ref(null)

const tituloMes = computed(() =>
  new Date(anio.value, mes.value, 1).toLocaleDateString('es-ES', { month: 'long', year: 'numeric' })
)

const celdas = computed(() => {
  const primero = new Date(anio.value, mes.value, 1)
  const offset = (primero.getDay() + 6) % 7
  const totalDias = new Date(anio.value, mes.value + 1, 0).getDate()
  const arr = Array(offset).fill(null)
  for (let d = 1; d <= totalDias; d++) arr.push(new Date(anio.value, mes.value, d))
  return arr
})

// Total de páginas (fuera)
const eventosDelMes = computed(() => {
  const inicioMes = new Date(anio.value, mes.value, 1)
  const finMes = new Date(anio.value, mes.value + 1, 0, 23, 59, 59)
  
  return eventos.value.filter(e => {
    const ini = new Date(e.fechaInicioEvento)
    return ini >= inicioMes && ini <= finMes
  }).sort((a, b) => new Date(a.fechaInicioEvento) - new Date(b.fechaInicioEvento))
})

function trunc(f) { return new Date(f.getFullYear(), f.getMonth(), f.getDate()).getTime() }

function eventosDelDia(dia) {
  const t = trunc(dia)
  return eventosDelMes.value.filter(e => {
    const ini = trunc(new Date(e.fechaInicioEvento))
    const fin = e.fechaFinEvento ? trunc(new Date(e.fechaFinEvento)) : ini
    return t >= ini && t <= fin
  })
}

function eventosDelDiaPaginados(dia) {
  return eventosDelDia(dia).slice(0, eventosPorCelda.value)
}

function tieneMasEventos(dia) {
  return eventosDelDia(dia).length > eventosPorCelda.value
}

function verMasEventos(dia) {
  diaSeleccionado.value = dia
}

function formatearFecha(fecha) {
  return fecha.toLocaleDateString('es-ES', { 
    weekday: 'long', 
    year: 'numeric', 
    month: 'long', 
    day: 'numeric' 
  })
}

function esHoy(d) { return trunc(d) === trunc(new Date()) }

function cambiarMes(delta) {
  const m = mes.value + delta
  if (m < 0) { mes.value = 11; anio.value-- }
  else if (m > 11) { mes.value = 0; anio.value++ }
  else mes.value = m
}

function colorEstado(estado) {
  const colores = {
    'FINALIZADO': 'bg-outline',
    'CANCELADO': 'bg-error',
    'ACTIVO': 'bg-primary',
    'PUBLICADO': 'bg-primary',
    'BORRADOR': 'bg-outline/50'
  }
  return colores[estado?.toUpperCase()] || 'bg-primary'
}

onMounted(async () => {
  try {
    const { data } = await eventoAPI.getAll()
    eventos.value = data ?? []
  } catch { eventos.value = [] }
})
</script>