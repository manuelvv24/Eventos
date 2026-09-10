<template>
  <div class="relative">
    <button
      class="relative rounded-full p-2 transition hover:bg-surface-container"
      aria-label="Notificaciones"
      @click="toggle"
    >
      <span class="material-symbols-outlined text-on-surface-variant">notifications</span>
      <span
        v-if="noLeidas > 0"
        class="absolute -right-0.5 -top-0.5 flex h-[18px] min-w-[18px] items-center justify-center rounded-full bg-error px-1 text-[10px] font-bold text-white"
      >{{ noLeidas > 9 ? '9+' : noLeidas }}</span>
    </button>

    <div
      v-if="abierto"
      class="absolute right-0 z-50 mt-2 max-h-[480px] w-96 overflow-y-auto rounded-xl border border-outline-variant/20 bg-white shadow-lg"
    >
      <div class="flex items-center justify-between border-b border-outline-variant/10 p-3">
        <p class="text-label-lg font-semibold text-on-surface">Notificaciones</p>
        <button class="text-label-sm text-primary hover:underline" @click="marcarTodas">
          Marcar todas leídas
        </button>
      </div>

      <div v-if="!notificaciones.length" class="p-8 text-center text-on-surface-variant">
        <span class="material-symbols-outlined mb-1 block text-[36px]">notifications_off</span>
        <p class="text-body-sm">Sin notificaciones</p>
      </div>

      <button
        v-for="n in notificaciones"
        :key="n.idNotificaciones"
        class="w-full border-b border-outline-variant/5 px-4 py-3 text-left transition hover:bg-surface-container-low"
        :class="n.estado === 'ENVIADA' ? 'bg-primary/5' : ''"
        @click="marcarLeida(n)"
      >
        <div class="flex items-start gap-3">
          <span class="material-symbols-outlined mt-0.5 text-[20px]" :class="icono(n.tipo).clase">
            {{ icono(n.tipo).icon }}
          </span>
          <div class="flex-1 min-w-0">
            <p class="text-label-md font-semibold text-on-surface truncate">{{ n.asunto }}</p>

            <template v-if="n.tipo === 'ACTUALIZACION' && cambiosDe(n).length">
              <p class="text-body-sm text-on-surface-variant mt-0.5">{{ introCambios(n) }}</p>
              <ul class="mt-1.5 space-y-1.5">
                <li v-for="(c, i) in cambiosDe(n)" :key="i" class="flex items-center gap-1.5 text-body-sm">
                  <span
                    class="material-symbols-outlined text-[15px] shrink-0"
                    :class="c.campo === 'hora' ? 'text-warning' : 'text-primary'"
                  >{{ c.campo === 'hora' ? 'schedule' : 'place' }}</span>
                  <span class="text-on-surface-variant/80 line-through">{{ c.anterior }}</span>
                  <span class="material-symbols-outlined text-[13px] text-on-surface-variant/60">arrow_forward</span>
                  <span class="font-semibold text-on-surface">{{ c.nuevo }}</span>
                </li>
              </ul>
            </template>

            <p v-else class="text-body-sm text-on-surface-variant mt-0.5 line-clamp-2">{{ n.descripcion }}</p>

            <p class="text-label-sm text-on-surface-variant/70 mt-1">
              {{ formatoFecha(n.fecha) }}
              <span v-if="n.evento"> · {{ n.evento }}</span>
            </p>
          </div>
          <span v-if="n.estado === 'ENVIADA'" class="mt-1.5 h-2 w-2 rounded-full bg-primary shrink-0" />
        </div>
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import notificacionAPI from '../services/notificacionAPI.js'

const abierto = ref(false)
const notificaciones = ref([])
const noLeidas = ref(0)
let timer = null

function icono(tipo) {
  return {
    RECORDATORIO:  { icon: 'alarm',             clase: 'text-warning' },
    CANCELACION:   { icon: 'block',             clase: 'text-error' },
    INSCRIPCION:   { icon: 'how_to_reg',        clase: 'text-success' },
    CERTIFICADO:   { icon: 'workspace_premium', clase: 'text-primary' },
    ACTUALIZACION: { icon: 'update',            clase: 'text-primary' },
  }[tipo] ?? { icon: 'notifications', clase: 'text-primary' }
}

function cambiosDe(n) {
  if (!n.descripcion || !n.descripcion.includes('tuvo cambios. ')) return []
  const texto = n.descripcion.split('tuvo cambios. ')[1].replace(/\.\s*$/, '')
  return texto.split(' · ').map(linea => {
    const partes = linea.split(/\s*(?:->|→)\s*/)
    return {
      campo: linea.trim().startsWith('Hora') ? 'hora' : 'lugar',
      anterior: (partes[0] || '').replace(/^(Hora|Lugar):\s*/, ''),
      nuevo: partes[1] || ''
    }
  })
}

function introCambios(n) {
  const idx = n.descripcion.indexOf('tuvo cambios.')
  return idx >= 0 ? n.descripcion.slice(0, idx + 'tuvo cambios'.length) + ':' : n.descripcion
}

function formatoFecha(f) {
  if (!f) return ''
  return new Date(f).toLocaleString('es-ES', {
    day: '2-digit',
    month: 'short',
    hour: '2-digit',
    minute: '2-digit'
  })
}

async function cargarNoLeidas() {
  try {
    const res = await notificacionAPI.noLeidas()
    noLeidas.value = res.data.noLeidas
  } catch (e) {
    console.error('Error cargando no leídas:', e)
  }
}

async function cargarLista() {
  try {
    const res = await notificacionAPI.listar()
    notificaciones.value = res.data
  } catch (e) {
    console.error('Error cargando lista:', e)
  }
}

async function toggle() {
  abierto.value = !abierto.value
  if (abierto.value) {
    await cargarLista()
  }
}

async function marcarLeida(n) {
  if (n.estado !== 'LEIDA') {
    try {
      await notificacionAPI.marcarLeida(n.idNotificaciones)
      n.estado = 'LEIDA'
      await cargarNoLeidas()
    } catch (e) {
      console.error('Error marcando leída:', e)
    }
  }
}

async function marcarTodas() {
  try {
    await notificacionAPI.marcarTodas()
    notificaciones.value.forEach(n => n.estado = 'LEIDA')
    noLeidas.value = 0
  } catch (e) {
    console.error('Error marcando todas:', e)
  }
}

function cerrarSiClickFuera(e) {
  const campana = e.target.closest('.relative')
  if (abierto.value && !campana) {
    abierto.value = false
  }
}

onMounted(() => {
  cargarNoLeidas()
  timer = setInterval(cargarNoLeidas, 30000)
  document.addEventListener('click', cerrarSiClickFuera)
})

onUnmounted(() => {
  clearInterval(timer)
  document.removeEventListener('click', cerrarSiClickFuera)
})
</script>

<style scoped>
.line-clamp-2 {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
</style>