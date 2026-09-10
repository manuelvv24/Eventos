<template>
  <div class="p-6 md:p-8 space-y-6 animate-fade-in">
    <div class="flex items-center justify-between">
      <div>
        <h1 class="text-headline-md text-on-surface font-semibold">Papelera</h1>
        <p class="text-body-md text-on-surface-variant mt-1">
          Restablece registros eliminados (soft-delete). Acceso exclusivo del Super Administrador.
        </p>
      </div>
      <AppButton variant="outlined" icon="refresh" :loading="cargando" @click="cargar" />
    </div>

    <AppBanner v-model="errorBanner" type="error" />
    <AppBanner v-model="exitoBanner" type="success" />

    <!-- ── Usuarios eliminados ───────────────────────────────────────────── -->
    <AppCard title="Usuarios eliminados">
      <template #header-action>
        <AppBadge variant="error">{{ usuarios.length }}</AppBadge>
      </template>
      <div v-if="usuarios.length === 0" class="text-body-sm text-on-surface-variant py-4">Sin usuarios eliminados.</div>
      <ul v-else class="divide-y divide-outline-variant/30">
        <li v-for="u in usuarios" :key="u.idUsuario" class="flex items-center justify-between gap-3 py-3">
          <div class="min-w-0">
            <p class="text-label-lg text-on-surface truncate">{{ u.primerNombreUsuario }} {{ u.primerApellidoUsuario }}</p>
            <p class="text-body-sm text-on-surface-variant truncate">{{ u.emailUsuario }} · {{ u.numeroDocumentoUsuario }}</p>
          </div>
          <ButtonIcon icon="restore" title="Restablecer" @click="restaurar('usuario', u)" />
        </li>
      </ul>
    </AppCard>

    <!-- ── Registros de asistencia eliminados ────────────────────────────── -->
    <AppCard title="Inscripciones eliminadas">
      <template #header-action>
        <AppBadge variant="error">{{ asistencias.length }}</AppBadge>
      </template>
      <div v-if="asistencias.length === 0" class="text-body-sm text-on-surface-variant py-4">Sin inscripciones eliminadas.</div>
      <ul v-else class="divide-y divide-outline-variant/30">
        <li v-for="a in asistencias" :key="a.idRegistroAsistencia" class="flex items-center justify-between gap-3 py-3">
          <div class="min-w-0">
            <p class="text-label-lg text-on-surface truncate">Evento #{{ a.idEventos }} · Participante #{{ a.idParticipantes }}</p>
            <p class="text-body-sm text-on-surface-variant truncate">{{ a.codigoQrInscripcion }} · Registrado {{ formatoFecha(a.fechaAsistencia) }}</p>
          </div>
          <ButtonIcon icon="restore" title="Restablecer" @click="restaurar('asistencia', a)" />
        </li>
      </ul>
    </AppCard>

    <!-- ── Roles inactivados ─────────────────────────────────────────────── -->
    <AppCard title="Roles inactivados">
      <template #header-action>
        <AppBadge variant="error">{{ rolesInactivos.length }}</AppBadge>
      </template>
      <div v-if="rolesInactivos.length === 0" class="text-body-sm text-on-surface-variant py-4">Sin roles inactivados.</div>
      <ul v-else class="divide-y divide-outline-variant/30">
        <li v-for="r in rolesInactivos" :key="r.idRol" class="flex items-center justify-between gap-3 py-3">
          <div class="min-w-0">
            <p class="text-label-lg text-on-surface">{{ r.nombreRol }}</p>
            <p class="text-body-sm text-on-surface-variant truncate">{{ r.descripcion || '—' }}</p>
          </div>
          <ButtonIcon icon="restore" title="Restablecer" @click="restaurar('rol', r)" />
        </li>
      </ul>
    </AppCard>

    <!-- ── Plantillas de certificado inactivadas ─────────────────────────── -->
    <AppCard title="Plantillas de certificado inactivas">
      <template #header-action>
        <AppBadge variant="error">{{ configuraciones.length }}</AppBadge>
      </template>
      <div v-if="configuraciones.length === 0" class="text-body-sm text-on-surface-variant py-4">Sin plantillas inactivadas.</div>
      <ul v-else class="divide-y divide-outline-variant/30">
        <li v-for="c in configuraciones" :key="c.idConfiguracion" class="flex items-center justify-between gap-3 py-3">
          <div class="min-w-0">
            <p class="text-label-lg text-on-surface">{{ c.nombrePlantilla || 'Plantilla sin nombre' }}</p>
            <p class="text-body-sm text-on-surface-variant truncate">{{ c.nombreOrganizacion || '—' }} · {{ c.tituloCertificado || '—' }}</p>
          </div>
          <ButtonIcon icon="restore" title="Restablecer" @click="restaurar('configuracion', c)" />
        </li>
      </ul>
    </AppCard>

    <!-- ── Participantes eliminados ─────────────────────────────────────── -->
    <AppCard title="Participantes eliminados">
      <template #header-action>
        <AppBadge variant="error">{{ participantes.length }}</AppBadge>
      </template>
      <div v-if="participantes.length === 0" class="text-body-sm text-on-surface-variant py-4">Sin participantes eliminados.</div>
      <ul v-else class="divide-y divide-outline-variant/30">
        <li v-for="p in participantes" :key="p.idParticipantes" class="flex items-center justify-between gap-3 py-3">
          <div class="min-w-0">
            <p class="text-label-lg text-on-surface truncate">{{ p.primerNombre || 'Participante' }} {{ p.primerApellido || '' }}</p>
            <p class="text-body-sm text-on-surface-variant truncate">{{ p.email || '—' }} · {{ p.documento || '—' }}</p>
          </div>
          <ButtonIcon icon="restore" title="Restablecer" @click="restaurar('participante', p)" />
        </li>
      </ul>
    </AppCard>

    <!-- ── Eventos eliminados ────────────────────────────────────────────── -->
    <AppCard title="Eventos en papelera">
      <template #header-action>
        <AppBadge variant="error">{{ eventos.length }}</AppBadge>
      </template>
      <div v-if="eventos.length === 0" class="text-body-sm text-on-surface-variant py-4">Sin eventos en papelera.</div>
      <ul v-else class="divide-y divide-outline-variant/30">
        <li v-for="e in eventos" :key="e.idEventos" class="flex items-center justify-between gap-3 py-3">
          <div class="min-w-0">
            <p class="text-label-lg text-on-surface truncate">{{ e.nombreEvento }}</p>
            <p class="text-body-sm text-on-surface-variant truncate">{{ e.lugarEvento || '—' }} · {{ formatoFecha(e.fechaInicioEvento) }}</p>
          </div>
          <ButtonIcon icon="restore" title="Restablecer" @click="restaurar('evento', e)" />
        </li>
      </ul>
    </AppCard>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import restaurarAPI from '../../services/restaurarAPI.js'
import AppCard    from '../../components/ui/AppCard.vue'
import AppButton  from '../../components/ui/AppButton.vue'
import AppBadge   from '../../components/ui/AppBadge.vue'
import AppBanner  from '../../components/ui/AppBanner.vue'
import ButtonIcon from '../../components/ui/ButtonIcon.vue'
import { useConfirm } from '../../composables/useConfirm.js'

const { confirm } = useConfirm()

const usuarios      = ref([])
const asistencias   = ref([])
const rolesInactivos = ref([])
const configuraciones = ref([])
const eventos       = ref([])
const participantes = ref([])
const cargando      = ref(false)
const errorBanner   = ref('')
const exitoBanner   = ref('')

function formatoFecha(v) {
  if (!v) return '—'
  const d = new Date(v)
  return isNaN(d) ? '—' : d.toLocaleDateString('es-CO')
}

async function cargar() {
  cargando.value = true
  errorBanner.value = ''
  try {
    const [u, a, r, c, e, p] = await Promise.all([
      restaurarAPI.usuarios(),
      restaurarAPI.asistencias(),
      restaurarAPI.roles(),
      restaurarAPI.configuraciones(),
      restaurarAPI.eventos(),
      restaurarAPI.participantes(),
    ])
    usuarios.value       = u.data ?? []
    asistencias.value    = a.data ?? []
    rolesInactivos.value = r.data ?? []
    configuraciones.value = c.data ?? []
    eventos.value        = e.data ?? []
    participantes.value  = p.data ?? []
  } catch (err) {
    errorBanner.value = err.response?.data?.error || 'Error al cargar la papelera'
  } finally {
    cargando.value = false
  }
}

async function restaurar(tipo, registro) {
  const nombre = {
    usuario:      `${registro.primerNombreUsuario} ${registro.primerApellidoUsuario}`,
    asistencia:   `inscripción #${registro.idRegistroAsistencia}`,
    rol:          registro.nombreRol,
    configuracion: registro.nombrePlantilla || 'plantilla',
    evento:       registro.nombreEvento,
    participante: `${registro.primerNombre || 'Participante'} ${registro.primerApellido || ''}`.trim(),
  }[tipo]

  const ok = await confirm({
    title: '¿Restablecer registro?',
    message: `Se restaurará ${nombre} y volverá a los listados activos.`,
    confirmText: 'Sí, restablecer',
  })
  if (!ok) return

  errorBanner.value = ''
  try {
    if (tipo === 'usuario')      await restaurarAPI.restaurarUsuario(registro.idUsuario)
    if (tipo === 'asistencia')   await restaurarAPI.restaurarAsistencia(registro.idRegistroAsistencia)
    if (tipo === 'rol')          await restaurarAPI.restaurarRol(registro.idRol)
    if (tipo === 'configuracion') await restaurarAPI.restaurarConfiguracion(registro.idConfiguracion)
    if (tipo === 'evento')       await restaurarAPI.restaurarEvento(registro.idEventos)
    if (tipo === 'participante') await restaurarAPI.restaurarParticipante(registro.idParticipantes)

    exitoBanner.value = `${nombre} restablecido correctamente.`
    await cargar()
  } catch (err) {
    errorBanner.value = err.response?.data?.error || 'No se pudo restablecer el registro'
  }
}

onMounted(cargar)
</script>