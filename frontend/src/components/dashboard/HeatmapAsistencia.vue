<template>
  <AppCard title="Heatmap de asistencia" subtitle="Distribución por día de la semana y hora">
    <div class="overflow-x-auto pb-2">
      <div :style="{ display: 'grid', gridTemplateColumns: `56px repeat(${HORAS.length}, minmax(22px, 1fr))`, gap: '3px' }">
        <div />
        <div v-for="h in HORAS" :key="h" class="text-center text-[10px] text-on-surface-variant">
          {{ String(h).padStart(2, '0') }}
        </div>

        <template v-for="dia in DIAS" :key="dia.id">
          <div class="flex items-center text-body-sm text-on-surface-variant">{{ dia.nombre }}</div>
          <div
            v-for="h in HORAS"
            :key="`${dia.id}-${h}`"
            class="h-6 rounded-md border border-outline-variant/10 cursor-pointer transition-transform hover:scale-110"
            :style="{ backgroundColor: color(valor(dia.id, h)) }"
            :title="`${dia.nombre} · ${h}:00 — ${valor(dia.id, h)} asistencia(s)`"
          />
        </template>
      </div>

      <div class="mt-4 flex items-center gap-2 text-body-sm text-on-surface-variant">
        <span>Menos</span>
        <span v-for="n in 5" :key="n" class="h-4 w-4 rounded" :style="{ backgroundColor: colorEscala(n - 1) }" />
        <span>Más</span>
      </div>
    </div>
  </AppCard>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import dashboardAPI from '../../services/dashboardAPI.js'
import AppCard from '../../components/ui/AppCard.vue'

const DIAS = [
  { id: 1, nombre: 'Lun' }, { id: 2, nombre: 'Mar' }, { id: 3, nombre: 'Mié' },
  { id: 4, nombre: 'Jue' }, { id: 5, nombre: 'Vie' }, { id: 6, nombre: 'Sáb' }, { id: 7, nombre: 'Dom' },
]
const HORAS = Array.from({ length: 24 }, (_, i) => i)

const mapa = ref({})
const max = ref(1)

function valor(dia, hora) { return mapa.value[`${dia}-${hora}`] ?? 0 }

function color(v) {
  if (v === 0) return 'rgba(67, 56, 202, 0.04)'
  const alpha = 0.15 + 0.85 * (v / max.value)
  return `rgba(67, 56, 202, ${alpha.toFixed(2)})`
}

function colorEscala(n) { return `rgba(67, 56, 202, ${(0.08 + n * 0.22).toFixed(2)})` }

onMounted(async () => {
  try {
    const { data } = await dashboardAPI.heatmap()
    const m = {}
    data.forEach(c => { m[`${c.dia}-${c.hora}`] = c.total })
    mapa.value = m
    max.value = Math.max(1, ...data.map(c => c.total))
  } catch (e) { console.error(e) }
})
</script>