<template>
  <div class="bg-background text-on-surface min-h-screen font-sans flex flex-col">
    <!-- ═══════════════ TopNavBar ═══════════════ -->
    <header
      class="sticky top-0 z-50 bg-surface/80 backdrop-blur-xl border-b border-outline-variant/40 transition-shadow duration-300"
      :class="scrolled ? 'shadow-md' : ''"
    >
      <nav class="flex justify-between items-center h-16 px-6 md:px-8 max-w-[1440px] mx-auto w-full gap-4">
        <!-- Marca -->
        <RouterLink
          to="/"
          class="flex items-center gap-2 shrink-0 text-headline-md font-bold text-primary transition-colors duration-200 hover:text-secondary"
          aria-label="Kinetic Pulse — Ir al inicio"
          @click="irInicio"
        >
          <span class="material-symbols-outlined text-[28px]" :style="{ fontVariationSettings: '\'FILL\' 1' }">graphic_eq</span>
          Kinetic Pulse
        </RouterLink>

        <!-- Links centrados -->
        <nav class="hidden md:flex items-center gap-8 flex-1 justify-center text-label-md">
          <a
            href="#eventos"
            :class="linkClase(seccionActiva === 'eventos')"
            @click.prevent="irSeccion('#eventos')"
          >Eventos</a>
          <a
            href="#nosotros"
            :class="linkClase(seccionActiva === 'nosotros')"
            @click.prevent="irSeccion('#nosotros')"
          >¿Quiénes somos?</a>
          <RouterLink
            to="/verificar/codigo"
            class="pb-1 border-b-2 border-transparent text-on-surface-variant hover:text-primary transition-colors duration-200"
          >
            Validar certificado
          </RouterLink>
        </nav>

        <!-- Acciones -->
        <div class="flex items-center gap-2 shrink-0">
          <template v-if="auth.isAuthenticated">
            <AppButton variant="tonal" icon="space_dashboard" :to="auth.homeRoute">Mi panel</AppButton>
          </template>
          <template v-else>
            <AppButton variant="outlined" to="/registro" class="hidden md:inline-flex">Crear cuenta</AppButton>
            <AppButton to="/login">Iniciar sesión</AppButton>
          </template>

          <ButtonIcon
            :icon="menuAbierto ? 'close' : 'menu'"
            title="Menú"
            class="md:hidden !rounded-full"
            @click="menuAbierto = !menuAbierto"
          />
        </div>
      </nav>

      <!-- Menú móvil -->
      <Transition name="desplegar">
        <div
          v-if="menuAbierto"
          class="md:hidden px-4 pb-4 pt-2 border-t border-outline-variant/30 space-y-1 text-label-lg"
          @click="menuAbierto = false"
        >
          <a
            href="#eventos"
            class="flex items-center gap-3 px-3 py-2.5 rounded-lg text-on-surface hover:bg-surface-container transition-base"
            @click.prevent="irSeccion('#eventos')"
          >
            <span class="material-symbols-outlined text-primary text-[20px]">event</span> Eventos
          </a>
          <a
            href="#nosotros"
            class="flex items-center gap-3 px-3 py-2.5 rounded-lg text-on-surface hover:bg-surface-container transition-base"
            @click.prevent="irSeccion('#nosotros')"
          >
            <span class="material-symbols-outlined text-primary text-[20px]">info</span> ¿Quiénes somos?
          </a>
          <RouterLink
            to="/verificar/codigo"
            class="flex items-center gap-3 px-3 py-2.5 rounded-lg text-on-surface hover:bg-surface-container transition-base"
          >
            <span class="material-symbols-outlined text-primary text-[20px]">verified</span> Validar certificado
          </RouterLink>
          <RouterLink
            v-if="auth.isAuthenticated"
            :to="auth.homeRoute"
            class="flex items-center gap-3 px-3 py-2.5 rounded-lg text-on-surface hover:bg-surface-container transition-base"
          >
            <span class="material-symbols-outlined text-primary text-[20px]">space_dashboard</span> Mi panel
          </RouterLink>
          <template v-else>
            <RouterLink
              to="/registro"
              class="flex items-center gap-3 px-3 py-2.5 rounded-lg text-on-surface hover:bg-surface-container transition-base"
            >
              <span class="material-symbols-outlined text-primary text-[20px]">person_add</span> Crear cuenta
            </RouterLink>
            <RouterLink
              to="/login"
              class="flex items-center gap-3 px-3 py-2.5 rounded-lg text-on-surface hover:bg-surface-container transition-base"
            >
              <span class="material-symbols-outlined text-primary text-[20px]">login</span> Iniciar sesión
            </RouterLink>
          </template>
        </div>
      </Transition>
    </header>

    <!-- ═══════════════ Layout principal ═══════════════ -->
    <main class="flex-grow w-full max-w-[1440px] mx-auto px-6 md:px-8 pt-10 pb-16 flex flex-col md:flex-row gap-8">
      <!-- ── Sidebar de filtros ── -->
      <aside class="w-full md:w-72 shrink-0 flex flex-col gap-6">
        <div class="bg-surface rounded-xl p-6 shadow-sm border border-outline-variant/20 flex flex-col gap-6 md:sticky md:top-24 md:self-start">
          <!-- Estado -->
          <div class="flex flex-col gap-3">
            <h3 class="text-label-md font-semibold transition-colors duration-200 flex items-center gap-1.5" :class="filtroEstado ? 'text-primary' : 'text-on-surface'">
              Estado
              <span v-if="filtroEstado" class="text-label-sm bg-primary/10 text-primary px-1.5 py-0.5 rounded-full leading-none">1</span>
            </h3>
            <div class="flex flex-wrap gap-2">
              <button
                v-for="op in OPCIONES_ESTADO"
                :key="op.value || 'todos'"
                type="button"
                class="px-3 py-1.5 rounded-full text-label-sm border transition-all active:scale-95"
                :class="filtroEstado === op.value
                  ? 'bg-primary/10 text-primary border-transparent font-semibold'
                  : 'bg-surface-container-lowest text-on-surface-variant border-outline-variant/60 hover:border-primary hover:text-primary'"
                @click="filtroEstado = op.value"
              >
                {{ op.label }}
                <span
                  class="ml-0.5 font-semibold"
                  :class="filtroEstado === op.value ? 'opacity-70' : 'text-outline'"
                >{{ conteoEstados[op.value] }}</span>
              </button>
            </div>
          </div>

          <hr class="border-outline-variant/30">

          <!-- Rango de fechas -->
          <div class="flex flex-col gap-3">
            <h3 class="text-label-md font-semibold transition-colors duration-200 flex items-center gap-1.5" :class="(filtroDesde || filtroHasta) ? 'text-primary' : 'text-on-surface'">
              Rango de fechas
              <span v-if="filtroDesde || filtroHasta" class="text-label-sm bg-primary/10 text-primary px-1.5 py-0.5 rounded-full leading-none">1</span>
            </h3>
            <div class="flex flex-col gap-2">
              <div class="relative">
                <span class="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-on-surface-variant text-[18px] pointer-events-none">calendar_month</span>
                <input
                  v-model="filtroDesde"
                  type="date"
                  aria-label="Fecha desde"
                  class="w-full pl-10 pr-3 py-2 bg-surface-container-lowest border border-outline-variant/60 rounded-full text-body-md text-on-surface outline-none focus:border-primary focus:ring-1 focus:ring-primary transition-all"
                />
              </div>
              <div class="relative">
                <span class="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-on-surface-variant text-[18px] pointer-events-none">calendar_month</span>
                <input
                  v-model="filtroHasta"
                  type="date"
                  aria-label="Fecha hasta"
                  class="w-full pl-10 pr-3 py-2 bg-surface-container-lowest border border-outline-variant/60 rounded-full text-body-md text-on-surface outline-none focus:border-primary focus:ring-1 focus:ring-primary transition-all"
                />
              </div>
            </div>
          </div>

          <hr class="border-outline-variant/30">

          <!-- Tipo de evento -->
          <div class="flex flex-col gap-3">
            <h3 class="text-label-md font-semibold transition-colors duration-200 flex items-center gap-1.5" :class="tiposActivas.length ? 'text-primary' : 'text-on-surface'">
              Tipo de evento
              <span v-if="tiposActivas.length" class="text-label-sm bg-primary/10 text-primary px-1.5 py-0.5 rounded-full leading-none">{{ tiposActivas.length }}</span>
            </h3>
            <div class="flex flex-wrap gap-2">
              <button
                type="button"
                class="px-3 py-1.5 rounded-full text-label-sm border transition-all active:scale-95"
                :class="!tiposActivas.length
                  ? 'bg-primary/10 text-primary border-transparent font-semibold'
                  : 'bg-surface-container-lowest text-on-surface-variant border-outline-variant/60 hover:border-primary hover:text-primary'"
                @click="tiposActivas = []"
              >
                Todos
                <span
                  class="ml-0.5 font-semibold"
                  :class="!tiposActivas.length ? 'opacity-70' : 'text-outline'"
                >{{ conteoTipos[''] ?? 0 }}</span>
              </button>
              <button
                v-for="t in tiposDisponibles"
                :key="t"
                type="button"
                class="px-3 py-1.5 rounded-full text-label-sm border transition-all active:scale-95"
                :class="listaIncluye(tiposActivas, t)
                  ? 'bg-primary/10 text-primary border-transparent font-semibold'
                  : 'bg-surface-container-lowest text-on-surface-variant border-outline-variant/60 hover:border-primary hover:text-primary'"
                @click="toggleFiltroLista(tiposActivas, t)"
              >
                {{ t }}
                <span
                  class="ml-0.5 font-semibold"
                  :class="listaIncluye(tiposActivas, t) ? 'opacity-70' : 'text-outline'"
                >{{ conteoTipos[normalizarTexto(t)] ?? 0 }}</span>
              </button>
            </div>
          </div>

          <hr class="border-outline-variant/30">

          <!-- Modalidad -->
          <div class="flex flex-col gap-3">
            <h3 class="text-label-md font-semibold transition-colors duration-200 flex items-center gap-1.5" :class="modalidadesActivas.length ? 'text-primary' : 'text-on-surface'">
              Modalidad
              <span v-if="modalidadesActivas.length" class="text-label-sm bg-primary/10 text-primary px-1.5 py-0.5 rounded-full leading-none">{{ modalidadesActivas.length }}</span>
            </h3>
            <div class="flex flex-wrap gap-2">
              <button
                type="button"
                class="px-3 py-1.5 rounded-full text-label-sm border transition-all active:scale-95"
                :class="!modalidadesActivas.length
                  ? 'bg-primary/10 text-primary border-transparent font-semibold'
                  : 'bg-surface-container-lowest text-on-surface-variant border-outline-variant/60 hover:border-primary hover:text-primary'"
                @click="modalidadesActivas = []"
              >
                Todas
                <span
                  class="ml-0.5 font-semibold"
                  :class="!modalidadesActivas.length ? 'opacity-70' : 'text-outline'"
                >{{ conteoModalidades[''] ?? 0 }}</span>
              </button>
              <button
                v-for="m in modalidadesDisponibles"
                :key="m"
                type="button"
                class="px-3 py-1.5 rounded-full text-label-sm border transition-all active:scale-95"
                :class="listaIncluye(modalidadesActivas, m)
                  ? 'bg-primary/10 text-primary border-transparent font-semibold'
                  : 'bg-surface-container-lowest text-on-surface-variant border-outline-variant/60 hover:border-primary hover:text-primary'"
                @click="toggleFiltroLista(modalidadesActivas, m)"
              >
                {{ m }}
                <span
                  class="ml-0.5 font-semibold"
                  :class="listaIncluye(modalidadesActivas, m) ? 'opacity-70' : 'text-outline'"
                >{{ conteoModalidades[normalizarTexto(m)] ?? 0 }}</span>
              </button>
            </div>
          </div>
        </div>
      </aside>

      <!-- ── Área de resultados ── -->
      <section id="eventos" class="flex-grow flex flex-col gap-6 w-full min-w-0 scroll-mt-24">
        <!-- Header -->
        <div class="flex flex-col gap-4">
          <div class="flex justify-between items-end gap-4">
            <div>
              <h1 class="text-display-sm md:text-display-md font-bold text-on-surface">Explorar Eventos</h1>
              <p class="text-body-lg text-on-surface-variant mt-2">Encuentra tu próxima gran experiencia.</p>
            </div>
            <div class="hidden md:flex items-center gap-2 shrink-0">
              <span class="text-label-sm text-on-surface-variant">Ordenar por:</span>
              <select
                v-model="ordenActual"
                aria-label="Ordenar eventos"
                class="bg-transparent border-none font-semibold text-label-md text-primary cursor-pointer outline-none pr-4"
              >
                <option value="relevancia">Más relevantes</option>
                <option value="fecha">Fecha (próximos)</option>
                <option value="populares">Populares</option>
              </select>
            </div>
          </div>

          <!-- Buscador -->
          <div class="relative flex items-center max-w-md">
            <span class="material-symbols-outlined absolute left-3.5 text-on-surface-variant text-[20px] pointer-events-none">search</span>
            <input
              v-model="busqueda"
              type="text"
              placeholder="Buscar eventos..."
              class="w-full pl-11 pr-11 py-2.5 bg-surface-container-low border border-outline-variant/30 rounded-xl text-body-md text-on-surface placeholder:text-outline outline-none focus:border-primary focus:ring-1 focus:ring-primary transition-all"
            />
            <ButtonIcon
              v-if="busqueda"
              icon="close"
              title="Limpiar búsqueda"
              class="!absolute right-2 !rounded-full"
              @click="busqueda = ''"
            />
          </div>

          <!-- Filtros activos -->
          <div v-if="filtrosActivos.length" class="flex flex-wrap items-center gap-2">
            <span class="text-label-sm text-on-surface-variant mr-1">Filtros activos:</span>
            <button
              v-for="(f, i) in filtrosActivos"
              :key="`${f.etiqueta}-${i}`"
              type="button"
              class="group flex items-center gap-1 bg-surface border border-outline-variant/40 px-3 py-1 rounded-full text-label-sm text-on-surface hover:bg-surface-container transition-colors shadow-sm cursor-pointer"
              :title="`Quitar filtro: ${f.etiqueta}`"
              @click="f.limpiar()"
            >
              {{ f.etiqueta }}
              <span class="material-symbols-outlined text-[14px] text-on-surface-variant group-hover:text-error transition-colors">close</span>
            </button>
            <button type="button" class="text-primary text-label-sm font-semibold hover:underline ml-2" @click="limpiarFiltros">
              Limpiar todo
            </button>
          </div>

          <!-- Contador de resultados -->
          <p v-if="!loading && !error" class="text-label-lg text-on-surface-variant">
            <span class="font-bold text-on-surface">{{ eventosFiltrados.length }}</span>
            {{ eventosFiltrados.length === 1 ? 'evento encontrado' : 'eventos encontrados' }}
          </p>
        </div>

        <!-- Loading -->
        <div v-if="loading" class="flex flex-col items-center justify-center py-24 gap-4 text-on-surface-variant">
          <span class="material-symbols-outlined text-[48px] text-primary animate-spin">progress_activity</span>
          <p class="text-body-md">Cargando eventos...</p>
        </div>

        <!-- Error -->
        <div v-else-if="error" class="flex flex-col items-center justify-center py-24 gap-4 text-error">
          <span class="material-symbols-outlined text-[48px]">error_outline</span>
          <p class="text-body-md font-medium">{{ error }}</p>
          <AppButton variant="outlined" icon="refresh" @click="cargarEventos">Reintentar</AppButton>
        </div>

        <!-- Vacío -->
        <div v-else-if="eventosFiltrados.length === 0" class="flex flex-col items-center justify-center py-24 gap-4 text-on-surface-variant">
          <span class="material-symbols-outlined text-[48px]">search_off</span>
          <p class="text-body-md font-medium">No hay eventos que coincidan con los filtros.</p>
          <button v-if="filtrosActivos.length" type="button" class="text-primary text-label-md font-semibold hover:underline" @click="limpiarFiltros">
            Limpiar todo
          </button>
        </div>

        <!-- Grid -->
        <TransitionGroup v-else tag="div" name="card" appear class="relative grid grid-cols-1 lg:grid-cols-2 xl:grid-cols-3 gap-6">
          <article
            v-for="evento in eventosPaginados"
            :key="evento.idEventos"
            class="group bg-surface-container-lowest rounded-xl shadow-sm hover:shadow-xl hover:-translate-y-1 transition-all duration-300 flex flex-col h-full overflow-hidden border border-outline-variant/20 relative"
          >
            <!-- Imagen -->
            <div class="relative h-48 w-full bg-surface-container-high overflow-hidden shrink-0">
              <img
                v-if="!imgsFallidas.has(evento.idEventos)"
                :src="imagenDe(evento)"
                :alt="evento.nombreEvento"
                loading="lazy"
                class="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500"
                @error="marcarImgFallida(evento.idEventos)"
              />
              <div v-else class="absolute inset-0 bg-gradient-to-br from-primary/10 to-secondary/15">
                <div
                  class="absolute inset-0 opacity-25"
                  style="background-image: radial-gradient(circle at 2px 2px, rgb(53, 37, 205, 0.55) 1px, transparent 0); background-size: 22px 22px;"
                />
                <span class="material-symbols-outlined absolute inset-0 m-auto w-fit h-fit text-[56px] text-primary/40">event</span>
              </div>

              <!-- Badge tipo -->
              <div class="absolute top-3 left-3">
                <span class="bg-surface-container-lowest/90 backdrop-blur-md text-on-surface px-2.5 py-1 rounded-md text-label-sm text-[11px] uppercase font-bold tracking-wide border border-outline-variant/30 shadow-sm">
                  {{ evento.tipoEvento || 'General' }}
                </span>
              </div>

              <!-- Badge estado -->
              <div class="absolute top-3 right-3">
                <span
                  class="px-2.5 py-1 rounded-md text-label-sm text-[11px] uppercase font-bold tracking-wide flex items-center gap-1.5 shadow-sm backdrop-blur-md"
                  :class="estadoBadgeClass(evento.estadoEvento)"
                >
                  <span
                    v-if="normalizarEstado(evento.estadoEvento) === 'ACTIVO'"
                    class="w-1.5 h-1.5 rounded-full animate-pulse"
                    :class="estadoDotClass(evento.estadoEvento)"
                  />
                  {{ estadoEtiqueta(evento.estadoEvento) }}
                </span>
              </div>
            </div>

            <!-- Contenido -->
            <div class="p-5 flex flex-col flex-grow gap-4">
              <div class="flex justify-between items-start gap-3">
                <h3 class="text-headline-sm font-bold text-on-surface leading-tight line-clamp-2 group-hover:text-primary transition-colors cursor-pointer">
                  {{ evento.nombreEvento }}
                </h3>
              </div>

              <p class="text-body-md text-on-surface-variant line-clamp-2">
                {{ evento.descripcionEvento || 'Sin descripción disponible.' }}
              </p>

              <div class="flex flex-col gap-2">
                <div class="flex items-center gap-2.5 text-on-surface text-body-md">
                  <span class="material-symbols-outlined text-[18px] text-on-surface-variant">calendar_today</span>
                  <span>{{ formatFecha(evento.fechaInicioEvento) }}</span>
                </div>
                <div class="flex items-center gap-2.5 text-on-surface text-body-md">
                  <span class="material-symbols-outlined text-[18px] text-on-surface-variant">
                    {{ esVirtual(evento) ? 'devices' : 'location_on' }}
                  </span>
                  <span class="truncate">{{ esVirtual(evento) ? 'Evento Virtual' : (evento.lugarEvento || 'Lugar por definir') }}</span>
                </div>
              </div>

              <div v-if="normalizarEstado(evento.estadoEvento) === 'ACTIVO'" class="flex items-center gap-2 text-body-md font-semibold">
                <template v-if="enVivo(evento)">
                  <span class="w-2.5 h-2.5 rounded-full bg-error animate-pulse shrink-0" />
                  <span class="text-error">En Vivo</span>
                </template>
                <template v-else-if="regresivo(evento.fechaInicioEvento)">
                  <span class="material-symbols-outlined text-[18px] text-primary">hourglass_top</span>
                  <span class="text-primary">Comienza en {{ formatoRegresivo(regresivo(evento.fechaInicioEvento)) }}</span>
                </template>
              </div>

              <!-- Footer -->
              <div class="flex flex-col gap-3 mt-auto pt-4 border-t border-outline-variant/20">
                <div class="flex gap-4 min-w-0">
                  <div class="flex flex-col gap-0.5">
                    <span class="text-label-sm text-[11px] text-on-surface-variant uppercase tracking-wider font-semibold">Duración</span>
                    <span class="text-body-md font-medium text-on-surface flex items-center gap-1">
                      <span class="material-symbols-outlined text-[14px] text-outline">schedule</span>
                      {{ evento.duracionEvento ? `${evento.duracionEvento}h` : '—' }}
                    </span>
                  </div>
                  <div class="flex flex-col gap-0.5">
                    <span class="text-label-sm text-[11px] text-on-surface-variant uppercase tracking-wider font-semibold">Capacidad</span>
                    <span
                      v-if="evento.aforoMaximoEvento"
                      class="text-body-md font-medium text-on-surface flex items-center gap-1"
                    >
                      <span class="material-symbols-outlined text-[14px] text-outline">groups</span>
                      {{ evento.aforoMaximoEvento }}
                    </span>
                    <span v-else class="text-body-md font-medium text-outline">Libre</span>
                  </div>
                  <div class="flex flex-col gap-0.5">
                    <span class="text-label-sm text-[11px] text-on-surface-variant uppercase tracking-wider font-semibold">Disponibles</span>
                    <span
                      v-if="evento.aforoMaximoEvento"
                      class="text-body-md font-medium flex items-center gap-1"
                      :class="claseCupos(evento)"
                    >
                      <span class="material-symbols-outlined text-[14px] text-outline">event_seat</span>
                      {{ cuposDe(evento) }}
                    </span>
                    <span v-else class="text-body-md font-medium text-outline">Libre</span>
                  </div>
                </div>
                <AppButton
                  variant="tonal"
                  size="sm"
                  icon-right="arrow_forward"
                  class="w-full shrink-0 whitespace-nowrap"
                  :to="auth.isAuthenticated
                    ? `/app/eventos/${evento.idEventos}`
                    : `/login?redirect=/app/eventos/${evento.idEventos}`"
                >
                  Detalles
                </AppButton>
              </div>
            </div>
          </article>
        </TransitionGroup>

        <!-- Paginación -->
        <nav v-if="!loading && !error && eventosFiltrados.length > 0" class="flex flex-wrap items-center justify-center gap-x-3 gap-y-2 mt-8" aria-label="Paginación de eventos">
          <span class="text-label-md text-on-surface-variant mr-1">
            Mostrando {{ rangoInicio }}–{{ rangoFin }} de {{ eventosFiltrados.length }}
          </span>
          <ButtonIcon
            icon="chevron_left"
            title="Página anterior"
            class="!w-10 !h-10 !rounded-lg border border-outline-variant/60"
            :disabled="paginaActual === 1"
            @click="paginaActual--"
          />

          <template v-for="(p, i) in paginasVisibles" :key="`${p}-${i}`">
            <span v-if="p === '...'" class="text-on-surface-variant px-1 select-none">...</span>
            <button
              v-else
              type="button"
              class="w-10 h-10 rounded-lg text-label-md font-medium flex items-center justify-center transition-all"
              :class="p === paginaActual
                ? 'bg-primary text-on-primary shadow-md shadow-primary/25 font-semibold'
                : 'border border-outline-variant/60 text-on-surface-variant hover:border-primary hover:text-primary'"
              @click="paginaActual = p"
            >
              {{ p }}
            </button>
          </template>

          <ButtonIcon
            icon="chevron_right"
            title="Página siguiente"
            class="!w-10 !h-10 !rounded-lg border border-outline-variant/60"
            :disabled="paginaActual === totalPaginas"
            @click="paginaActual++"
          />
        </nav>
      </section>
    </main>

    <!-- ═══════════════ Resumen e hitos ═══════════════ -->
    <section class="w-full bg-surface-container-lowest border-y border-outline-variant/30 py-14">
      <div class="max-w-[1440px] mx-auto px-6 md:px-8 grid md:grid-cols-2 gap-6">
        <!-- Resumen del Catálogo -->
        <div class="relative overflow-hidden rounded-2xl bg-gradient-to-br from-primary to-secondary p-8 text-on-primary shadow-lg shadow-primary/20 flex flex-col gap-6">
          <div class="absolute top-0 right-0 w-40 h-40 bg-white/10 rounded-full blur-2xl -translate-y-1/2 translate-x-1/4" />
          <div class="absolute bottom-0 left-0 w-28 h-28 bg-white/10 rounded-full blur-xl translate-y-1/3 -translate-x-1/3" />
          <div class="flex items-center gap-3 relative z-10">
            <span class="w-11 h-11 rounded-xl bg-white/15 backdrop-blur-sm flex items-center justify-center shrink-0">
              <span class="material-symbols-outlined text-[24px]">insights</span>
            </span>
            <h3 class="text-headline-sm font-semibold">Resumen del Catálogo</h3>
          </div>
          <div class="flex items-end justify-between gap-8 flex-wrap relative z-10">
            <div>
              <p class="text-display-sm font-bold leading-none">{{ conteoEstados.ACTIVO }}</p>
              <p class="text-label-lg opacity-90 mt-1.5">eventos activos ahora mismo</p>
            </div>
            <div class="flex gap-8">
              <div>
                <p class="text-title-lg font-bold leading-none">{{ conteoEstados[''] }}</p>
                <p class="text-label-sm opacity-80 mt-1">Totales</p>
              </div>
              <div>
                <p class="text-title-lg font-bold leading-none">{{ conteoEstados.FINALIZADO }}</p>
                <p class="text-label-sm opacity-80 mt-1">Finalizados</p>
              </div>
              <div>
                <p class="text-title-lg font-bold leading-none">{{ conteoEstados.CANCELADO }}</p>
                <p class="text-label-sm opacity-80 mt-1">Cancelados</p>
              </div>
            </div>
          </div>
          <p class="text-label-md opacity-90 relative z-10 italic">Explora las últimas tendencias y oportunidades de networking en tu industria.</p>
        </div>

        <!-- Próximos Eventos -->
        <div class="relative overflow-hidden rounded-2xl bg-gradient-to-br from-secondary to-primary p-8 text-on-primary shadow-lg shadow-primary/20 flex flex-col gap-6">
          <div class="absolute top-0 right-0 w-40 h-40 bg-white/10 rounded-full blur-2xl -translate-y-1/2 translate-x-1/4" />
          <div class="flex items-center gap-3 relative z-10">
            <span class="w-11 h-11 rounded-xl bg-white/15 backdrop-blur-sm flex items-center justify-center shrink-0">
              <span class="material-symbols-outlined text-[24px]">flag</span>
            </span>
            <h3 class="text-headline-sm font-semibold">Próximos Eventos</h3>
          </div>
          <div class="flex items-center justify-between gap-6 flex-wrap relative z-10">
            <div class="min-w-0">
              <p class="text-label-sm uppercase tracking-wider opacity-80">Próximo evento</p>
              <p class="text-title-lg font-bold truncate max-w-md mt-0.5">{{ proximoEvento?.nombreEvento ?? 'Sin eventos activos' }}</p>
              <p v-if="proximoEvento" class="text-label-md opacity-90 mt-0.5">{{ formatFecha(proximoEvento.fechaInicioEvento) }}</p>
            </div>
            <RouterLink
              v-if="proximoEvento"
              :to="auth.isAuthenticated
                ? `/app/eventos/${proximoEvento.idEventos}`
                : `/login?redirect=/app/eventos/${proximoEvento.idEventos}`"
              class="inline-flex items-center gap-1.5 bg-surface text-primary px-5 py-2.5 rounded-xl text-label-lg font-bold hover:-translate-y-0.5 hover:shadow-lg transition-all shrink-0"
            >
              Ver detalle
              <span class="material-symbols-outlined text-[18px]">arrow_forward</span>
            </RouterLink>
          </div>
          <div class="flex flex-col gap-3 relative z-10">
            <div v-if="proximoEvento?.aforoMaximoEvento" class="flex flex-col gap-2">
              <div class="flex justify-between items-center text-label-sm text-[11px]">
                <span>Capacidad máxima: {{ proximoEvento.aforoMaximoEvento }} · Inscritos: {{ proximoEvento.inscritos ?? 0 }}</span>
                <span class="font-bold">{{ porcentajeDe(proximoEvento) }}% ocupado</span>
              </div>
              <div class="w-full h-1.5 bg-white/20 rounded-full overflow-hidden">
                <div class="h-full bg-white transition-all duration-500" :style="{ width: `${porcentajeDe(proximoEvento)}%` }" />
              </div>
            </div>
            <div class="flex flex-col gap-3">
              <div class="flex items-center gap-2">
                <span class="material-symbols-outlined text-[20px]">event_seat</span>
                <span v-if="proximoEvento?.aforoMaximoEvento" class="text-label-lg font-bold">
                  {{ cuposDe(proximoEvento) }} cupos disponibles
                </span>
                <span v-else class="text-label-lg font-bold">
                  {{ proximoEvento ? 'Acceso libre' : '—' }}
                </span>
              </div>
              <div
                v-if="proximoEvento"
                class="flex items-center gap-2.5 bg-white/15 backdrop-blur-sm px-4 py-2 rounded-xl w-fit"
              >
                <span v-if="enVivo(proximoEvento)" class="w-2.5 h-2.5 rounded-full bg-error animate-pulse shrink-0" />
                <span v-else class="material-symbols-outlined text-[22px]">hourglass_top</span>
                <span class="flex flex-col leading-tight">
                  <span v-if="!enVivo(proximoEvento)" class="text-[10px] uppercase tracking-wider opacity-80">Comienza en</span>
                  <span v-if="enVivo(proximoEvento)" class="text-title-md font-bold tracking-wide">En Vivo</span>
                  <span v-else class="text-title-md font-bold font-mono tabular-nums">
                    {{ formatoRegresivo(regresivo(proximoEvento.fechaInicioEvento)) }}
                  </span>
                </span>
              </div>
            </div>
            <p class="text-label-sm opacity-90 italic">¡Asegura tu lugar antes del cierre!</p>
          </div>
        </div>
      </div>
    </section>

    <!-- ═══════════════ Portafolio para empresas ═══════════════ -->
    <section id="nosotros" class="bg-surface-container-low py-16 scroll-mt-24">
      <div class="max-w-[1440px] mx-auto px-6 md:px-8">
        <h2 class="text-display-sm font-bold text-on-surface text-center mb-12">Un portafolio diseñado para empresas</h2>

        <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
          <div v-for="item in PORTAFOLIO" :key="item.titulo" class="flex flex-col gap-4">
            <div class="h-44 rounded-xl overflow-hidden relative bg-gradient-to-br from-primary/10 to-secondary/15 group/img">
              <img
                v-if="!imgsFallidas.has(item.titulo)"
                :src="item.imagen"
                :alt="item.titulo"
                loading="lazy"
                class="w-full h-full object-cover group-hover/img:scale-105 transition-transform duration-500"
                @error="marcarImgFallida(item.titulo)"
              />
              <span v-else class="material-symbols-outlined absolute inset-0 m-auto w-fit h-fit text-[52px] text-primary/40">{{ item.icono }}</span>
              <div class="absolute inset-0 bg-gradient-to-t from-black/40 via-black/10 to-transparent pointer-events-none" />
              <span class="absolute bottom-3 left-3 w-10 h-10 rounded-lg bg-surface-container-lowest/90 backdrop-blur-md shadow-md flex items-center justify-center">
                <span class="material-symbols-outlined text-primary text-[22px]">{{ item.icono }}</span>
              </span>
            </div>
            <h3 class="text-title-lg font-bold text-on-surface">{{ item.titulo }}</h3>
            <p class="text-body-md text-on-surface-variant">{{ item.descripcion }}</p>
          </div>
        </div>

        <!-- Banner estratégico -->
        <div class="mt-16 bg-gradient-to-br from-primary to-secondary rounded-2xl overflow-hidden flex flex-col md:flex-row shadow-lg">
          <div class="p-8 md:p-12 flex-1 flex flex-col justify-center text-on-primary">
            <h2 class="text-display-sm font-bold leading-tight mb-5">Un escenario estratégico para tus objetivos</h2>
            <p class="text-body-lg opacity-90 mb-8 max-w-xl">
              Kinetic Pulse es una plataforma integral para el desarrollo de eventos corporativos. Un entorno pensado
              para generar conexión, bienestar y experiencias significativas para tu organización.
            </p>
            <div>
              <RouterLink
                to="/registro"
                class="inline-flex w-fit bg-surface text-primary px-6 py-3 rounded-xl text-label-lg font-bold hover:-translate-y-0.5 hover:shadow-lg transition-all"
              >
                Cotiza ahora
              </RouterLink>
            </div>
          </div>
          <div class="flex-1 min-h-[260px] relative bg-gradient-to-tr from-secondary/60 to-primary/40">
            <img
              v-if="!imgsFallidas.has('banner')"
              src="https://images.unsplash.com/photo-1459749411175-04bf5292ceea?auto=format&fit=crop&w=1000&q=70"
              alt="Asistentes disfrutando un evento Kinetic Pulse"
              loading="lazy"
              class="absolute inset-0 w-full h-full object-cover"
              @error="marcarImgFallida('banner')"
            />
            <div class="absolute inset-0 bg-gradient-to-r from-primary/70 via-primary/20 to-transparent" />
            <span class="material-symbols-outlined absolute inset-0 m-auto w-fit h-fit text-[88px] text-white/40 drop-shadow-lg">diversity_3</span>
          </div>
        </div>
      </div>
    </section>

    <!-- ═══════════════ CTA final ═══════════════ -->
    <section class="bg-surface-container-lowest border-t border-outline-variant/30 py-16">
      <div class="max-w-[1440px] mx-auto px-6 md:px-8 flex flex-col items-center gap-6 text-center">
        <h2 class="text-display-sm font-bold text-on-surface max-w-2xl">Potencia el impacto de tus eventos</h2>
        <p class="text-body-lg text-on-surface-variant max-w-2xl">
          Únete a los líderes que están transformando la industria: más de 500 eventos gestionados,
          98% de satisfacción de asistentes y un incremento del 40% en la eficiencia operativa.
        </p>
        <div class="flex flex-col sm:flex-row gap-4 mt-2">
          <AppButton size="lg" to="/registro">Empieza ahora gratis</AppButton>
          <AppButton size="lg" variant="outlined" icon="verified" to="/verificar/codigo">Validar un certificado</AppButton>
        </div>
      </div>
    </section>

    <!-- ═══════════════ Footer ═══════════════ -->
    <footer class="w-full mt-auto bg-surface-container border-t border-outline-variant/30 flex flex-col md:flex-row justify-between items-center py-8 px-6 md:px-8 gap-4">
      <div class="flex items-center gap-2">
        <span class="material-symbols-outlined text-primary text-[22px]" :style="{ fontVariationSettings: '\'FILL\' 1' }">graphic_eq</span>
        <span class="text-title-lg font-bold text-on-surface">Kinetic Pulse</span>
      </div>
      <p class="text-label-sm text-on-surface-variant text-center order-3 md:order-2">
        © 2026 Kinetic Pulse.Gestion de Eventos
      </p>
      <nav class="flex flex-wrap justify-center gap-5 order-2 md:order-3 text-label-sm">
        <a href="#" class="text-on-surface-variant hover:text-primary transition-colors">Privacidad</a>
        <a href="#" class="text-on-surface-variant hover:text-primary transition-colors">Términos</a>
        <RouterLink to="/verificar/codigo" class="text-on-surface-variant hover:text-primary transition-colors">Validar certificado</RouterLink>
      </nav>
    </footer>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { useAuthStore } from '../../stores/auth.js'
import eventoAPI from '../../services/eventoAPI.js'
import AppButton from '../../components/ui/AppButton.vue'
import ButtonIcon from '../../components/ui/ButtonIcon.vue'

const auth  = useAuthStore()
const route = useRoute()

// ── Navbar ───────────────────────────────────────────────────────────────────
const menuAbierto   = ref(false)
const scrolled      = ref(false)
const seccionActiva = ref('')

function linkClase(activa) {
  return [
    'pb-1 border-b-2 transition-colors duration-200',
    activa
      ? 'text-primary border-primary font-semibold'
      : 'text-on-surface-variant border-transparent hover:text-primary',
  ]
}

function irSeccion(sel) {
  menuAbierto.value = false
  document.querySelector(sel)?.scrollIntoView({ behavior: 'smooth' })
}

function irInicio() {
  menuAbierto.value = false
  if (route.path === '/') window.scrollTo({ top: 0, behavior: 'smooth' })
}

// ── Estado general ───────────────────────────────────────────────────────────
const eventos = ref([])
const loading = ref(true)
const error   = ref(null)

// ── Orden ────────────────────────────────────────────────────────────────────
const ordenActual = ref('relevancia')

const eventosOrdenados = computed(() => {
  const copia = [...eventos.value]
  if (ordenActual.value === 'fecha') {
    copia.sort((a, b) => new Date(a.fechaInicioEvento ?? 0) - new Date(b.fechaInicioEvento ?? 0))
  } else if (ordenActual.value === 'populares') {
    copia.sort((a, b) => (b.aforoMaximoEvento ?? 0) - (a.aforoMaximoEvento ?? 0))
  }
  return copia
})

// ── Filtros ──────────────────────────────────────────────────────────────────
const busqueda           = ref('')
const filtroEstado       = ref('')
const filtroDesde        = ref('')
const filtroHasta        = ref('')
const tiposActivas       = ref([])
const modalidadesActivas = ref([])

const OPCIONES_ESTADO = [
  { value: '',           label: 'Todos' },
  { value: 'ACTIVO',     label: 'Activos' },
  { value: 'FINALIZADO', label: 'Finalizados' },
  { value: 'CANCELADO',  label: 'Cancelados' },
]

function normalizarTexto(valor) {
  return (valor ?? '')
    .toString()
    .trim()
    .toLowerCase()
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
}

const CANON_TIPOS       = [
  'Conferencia', 'Seminario', 'Congreso', 'Taller', 'Capacitación',
  'Curso', 'Charla', 'Webinar', 'Torneo', 'Networking',
  'Feria', 'Cultural', 'Deportivo', 'Bienestar',
]
const CANON_MODALIDADES = ['Presencial', 'Virtual', 'Híbrido', 'Semipresencial']

function opcionesDesdeDatos(campo, canon) {
  const encontrados = new Map()
  for (const ev of eventos.value) {
    const v = (ev[campo] ?? '').toString().trim()
    const k = normalizarTexto(v)
    if (v && !encontrados.has(k)) encontrados.set(k, v)
  }
  if (encontrados.size === 0) return canon
  const ordenados = []
  for (const c of canon) {
    const k = normalizarTexto(c)
    if (encontrados.has(k)) {
      ordenados.push(encontrados.get(k))
      encontrados.delete(k)
    }
  }
  return [...ordenados, ...encontrados.values()]
}

const tiposDisponibles       = computed(() => opcionesDesdeDatos('tipoEvento', CANON_TIPOS))
const modalidadesDisponibles = computed(() => opcionesDesdeDatos('modalidadEvento', CANON_MODALIDADES))

function listaIncluye(lista, valor) {
  const arreglo = Array.isArray(lista) ? lista : (lista.value ?? [])
  const objetivo = normalizarTexto(valor)
  return arreglo.some(x => normalizarTexto(x) === objetivo)
}

function esVirtual(evento) {
  return normalizarTexto(evento?.modalidadEvento) === 'virtual'
}

function normalizarEstado(estado) {
  const v = (estado ?? '').toUpperCase()
  return (v === 'ACTIVO' || v === 'PUBLICADO') ? 'ACTIVO' : v
}

function estadoEtiqueta(estado) {
  const e = normalizarEstado(estado)
  return e || 'N/D'
}

function toggleFiltroLista(lista, valor) {
  const arreglo = Array.isArray(lista) ? lista : lista.value
  const i = arreglo.findIndex(x => normalizarTexto(x) === normalizarTexto(valor))
  if (i === -1) arreglo.push(valor)
  else arreglo.splice(i, 1)
}

// ── Paginación ───────────────────────────────────────────────────────────────
const paginaActual = ref(1)
const porPagina    = 6

const conteoEstados = computed(() => {
  const c = { '': eventos.value.length, ACTIVO: 0, FINALIZADO: 0, CANCELADO: 0 }
  for (const e of eventos.value) {
    const v = normalizarEstado(e.estadoEvento)
    if (c[v] !== undefined) c[v]++
  }
  return c
})

const conteoTipos = computed(() => {
  const c = { '': eventos.value.length }
  for (const e of eventos.value) {
    const k = normalizarTexto(e.tipoEvento)
    if (k) c[k] = (c[k] || 0) + 1
  }
  return c
})

const conteoModalidades = computed(() => {
  const c = { '': eventos.value.length }
  for (const e of eventos.value) {
    const k = normalizarTexto(e.modalidadEvento)
    if (k) c[k] = (c[k] || 0) + 1
  }
  return c
})

const proximoEvento = computed(() => {
  return eventos.value
    .filter(e => normalizarEstado(e.estadoEvento) === 'ACTIVO' && e.fechaInicioEvento)
    .sort((a, b) => new Date(a.fechaInicioEvento) - new Date(b.fechaInicioEvento))[0] ?? null
})

// ── Regresivo y cupos ────────────────────────────────────────────────────────
const ahora = ref(Date.now())
let timerRegresivo = null

function iniciarRegresivo() {
  detenerRegresivo()
  ahora.value = Date.now()
  timerRegresivo = setInterval(() => { ahora.value = Date.now() }, 1000)
}

function detenerRegresivo() {
  if (timerRegresivo) {
    clearInterval(timerRegresivo)
    timerRegresivo = null
  }
}

function regresivo(fechaInicio) {
  if (!fechaInicio) return null
  const diff = new Date(fechaInicio).getTime() - ahora.value
  if (diff <= 0) return null
  const totalSegundos = Math.floor(diff / 1000)
  const dias    = Math.floor(totalSegundos / 86400)
  const horas   = Math.floor((totalSegundos % 86400) / 3600)
  const minutos = Math.floor((totalSegundos % 3600) / 60)
  const segundos = totalSegundos % 60
  return { dias, horas, minutos, segundos }
}

function formatoRegresivo(r) {
  if (!r) return null
  const pad = n => String(n).padStart(2, '0')
  if (r.dias > 0) return `${r.dias}d ${pad(r.horas)}:${pad(r.minutos)}:${pad(r.segundos)}`
  return `${pad(r.horas)}:${pad(r.minutos)}:${pad(r.segundos)}`
}

function enVivo(evento) {
  return normalizarEstado(evento.estadoEvento) === 'ACTIVO' &&
         evento.fechaInicioEvento &&
         !regresivo(evento.fechaInicioEvento)
}

function cuposDe(evento) {
  const aforo = evento.aforoMaximoEvento
  if (!aforo || aforo <= 0) return null
  return Math.max(0, aforo - (evento.inscritos ?? 0))
}

function claseCupos(evento) {
  const cupos = cuposDe(evento)
  if (cupos === null) return 'text-outline'
  if (cupos === 0) return 'text-error'
  if (cupos <= Math.max(3, Math.round((evento.aforoMaximoEvento || 0) * 0.2))) return 'text-warning'
  return 'text-success'
}

function porcentajeDe(evento) {
  const aforo = evento.aforoMaximoEvento
  if (!aforo || aforo <= 0) return 0
  return Math.min(100, Math.round(((evento.inscritos ?? 0) / aforo) * 100))
}

watch(
  [busqueda, filtroEstado, filtroDesde, filtroHasta, tiposActivas, modalidadesActivas],
  () => { paginaActual.value = 1 },
  { deep: true },
)

const filtrosActivos = computed(() => {
  const f = []
  if (busqueda.value) {
    f.push({ etiqueta: `Búsqueda: ${busqueda.value}`, limpiar: () => { busqueda.value = '' } })
  }
  if (filtroEstado.value) {
    const op = OPCIONES_ESTADO.find(o => o.value === filtroEstado.value)
    f.push({ etiqueta: `Estado: ${op?.label ?? filtroEstado.value}`, limpiar: () => { filtroEstado.value = '' } })
  }
  if (filtroDesde.value) f.push({ etiqueta: `Desde: ${filtroDesde.value}`, limpiar: () => { filtroDesde.value = '' } })
  if (filtroHasta.value) f.push({ etiqueta: `Hasta: ${filtroHasta.value}`, limpiar: () => { filtroHasta.value = '' } })
  for (const t of tiposActivas.value) {
    f.push({ etiqueta: t, limpiar: () => toggleFiltroLista(tiposActivas, t) })
  }
  for (const m of modalidadesActivas.value) {
    f.push({ etiqueta: m, limpiar: () => toggleFiltroLista(modalidadesActivas, m) })
  }
  return f
})

const eventosFiltrados = computed(() => {
  return eventosOrdenados.value.filter(e => {
    if (busqueda.value) {
      const q = normalizarTexto(busqueda.value)
      const match = [e.nombreEvento, e.descripcionEvento, e.lugarEvento, e.tipoEvento]
        .some(v => normalizarTexto(v).includes(q))
      if (!match) return false
    }
    if (filtroEstado.value && normalizarEstado(e.estadoEvento) !== filtroEstado.value) return false
    if (tiposActivas.value.length > 0 && !listaIncluye(tiposActivas, e.tipoEvento)) return false
    if (modalidadesActivas.value.length > 0 && !listaIncluye(modalidadesActivas, e.modalidadEvento)) return false
    const inicio = e.fechaInicioEvento ? new Date(e.fechaInicioEvento) : null
    if (filtroDesde.value) {
      const desde = new Date(filtroDesde.value)
      if (!inicio || inicio < desde) return false
    }
    if (filtroHasta.value) {
      const hasta = new Date(filtroHasta.value)
      hasta.setHours(23, 59, 59, 999)
      if (!inicio || inicio > hasta) return false
    }
    return true
  })
})

const totalPaginas = computed(() => Math.max(1, Math.ceil(eventosFiltrados.value.length / porPagina)))

const eventosPaginados = computed(() => {
  const start = (paginaActual.value - 1) * porPagina
  return eventosFiltrados.value.slice(start, start + porPagina)
})

const paginasVisibles = computed(() => {
  const total  = totalPaginas.value
  const actual = paginaActual.value
  if (total <= 7) return Array.from({ length: total }, (_, i) => i + 1)

  const candidatas = new Set([1, 2, actual - 1, actual, actual + 1, total - 1, total])
  const ordenadas  = [...candidatas].filter(p => p >= 1 && p <= total).sort((a, b) => a - b)

  const salida = []
  let previa = 0
  for (const p of ordenadas) {
    if (p - previa > 1) salida.push('...')
    salida.push(p)
    previa = p
  }
  return salida
})

const rangoInicio = computed(() => ((paginaActual.value - 1) * porPagina) + 1)
const rangoFin    = computed(() => Math.min(paginaActual.value * porPagina, eventosFiltrados.value.length))

// ── Imágenes demo ────────────────────────────────────────────────────────────
const IMAGENES_EVENTO = {
  conferencia:  ['photo-1540575467063-178a50c2df87', 'photo-1475721027785-f74eccf877e2'],
  taller:       ['photo-1552664730-d307ca884978', 'photo-1587825140708-dfaf72ae4b04'],
  capacitacion: ['photo-1524178232363-1fb2b075b655', 'photo-1509062522246-3755977927d7'],
  torneo:       ['photo-1461896836934-ffe607ba8211', 'photo-1554068865-24cecd4e34b8'],
  networking:   ['photo-1511795409834-ef04bbd61622', 'photo-1556761175-b413da4baf72'],
  seminario:    ['photo-1544531586-fde5298cdd40', 'photo-1434030216411-0b793f4b4173'],
  congreso:     ['photo-1505373877841-8d25f7d46678', 'photo-1517457373958-b7bdd4587205'],
  general:      ['photo-1492684223066-81342ee5ff30', 'photo-1511578314322-379afb476865', 'photo-1531058020387-3be344556be6'],
}

function urlUnsplash(id) {
  return `https://images.unsplash.com/${id}?auto=format&fit=crop&w=900&q=70`
}

function imagenDe(evento) {
  if (evento.imagenUrl) return evento.imagenUrl
  const pool = IMAGENES_EVENTO[normalizarTexto(evento.tipoEvento)] ?? IMAGENES_EVENTO.general
  const idx  = Math.abs(Number(evento.idEventos ?? 0)) % pool.length
  return urlUnsplash(pool[idx])
}

const imgsFallidas = ref(new Set())

function marcarImgFallida(clave) {
  imgsFallidas.value.add(clave)
}

function estadoBadgeClass(estado) {
  const e = normalizarEstado(estado)
  if (e === 'ACTIVO')     return 'bg-success-container/90 text-success'
  if (e === 'FINALIZADO') return 'bg-surface-container-lowest/90 text-on-surface-variant'
  if (e === 'CANCELADO')  return 'bg-error-container/90 text-error'
  return 'bg-surface-container-lowest/90 text-on-surface-variant'
}

function estadoDotClass(estado) {
  const e = normalizarEstado(estado)
  if (e === 'ACTIVO')    return 'bg-success'
  if (e === 'CANCELADO') return 'bg-error'
  return 'bg-outline'
}

function formatFecha(f) {
  if (!f) return 'Por definir'
  return new Date(f).toLocaleDateString('es-ES', {
    day: 'numeric', month: 'short', year: 'numeric',
  })
}

function limpiarFiltros() {
  busqueda.value           = ''
  filtroEstado.value       = ''
  filtroDesde.value        = ''
  filtroHasta.value        = ''
  tiposActivas.value       = []
  modalidadesActivas.value = []
  paginaActual.value       = 1
}

// ── Portafolio empresas ──────────────────────────────────────────────────────
const PORTAFOLIO = [
  {
    icono: 'handshake',
    titulo: 'Eventos a la medida',
    imagen: urlUnsplash('photo-1531058020387-3be344556be6'),
    descripcion: 'Diseñamos experiencias corporativas según objetivos, tipo de empresa y presupuesto.',
  },
  {
    icono: 'park',
    titulo: 'Espacios amplios y naturales',
    imagen: urlUnsplash('photo-1506744038136-46273834b3fb'),
    descripcion: 'Áreas abiertas y amplios espacios que permiten integrar trabajo, bienestar y recreación.',
  },
  {
    icono: 'support_agent',
    titulo: 'Acompañamiento integral',
    imagen: urlUnsplash('photo-1600880292203-757bb62b4baf'),
    descripcion: 'Asesoría antes, durante y después del evento para asegurar una experiencia organizada.',
  },
  {
    icono: 'tune',
    titulo: 'Formatos flexibles',
    imagen: urlUnsplash('photo-1517245386807-bb43f82c33c4'),
    descripcion: 'Desde 15 personas hasta encuentros de gran formato, con operación estructurada.',
  },
]

// ── Data ─────────────────────────────────────────────────────────────────────
async function cargarEventos() {
  loading.value = true
  error.value   = null
  try {
    const { data } = await eventoAPI.getAll()
    eventos.value = Array.isArray(data) ? data : []
  } catch {
    error.value = 'No se pudo conectar con el servidor. Verifica que el backend esté corriendo.'
    eventos.value = []
  } finally {
    loading.value = false
  }
}

function onScroll() {
  scrolled.value = window.scrollY > 20
  let actual = ''
  for (const id of ['eventos', 'nosotros']) {
    const el = document.getElementById(id)
    if (el && el.getBoundingClientRect().top <= 140) actual = id
  }
  seccionActiva.value = actual
}

onMounted(() => {
  cargarEventos()
  iniciarRegresivo()
  window.addEventListener('scroll', onScroll, { passive: true })
})
onUnmounted(() => {
  detenerRegresivo()
  window.removeEventListener('scroll', onScroll)
})
</script>

<style scoped>
.desplegar-enter-active,
.desplegar-leave-active {
  transition: opacity 0.2s ease, transform 0.2s ease;
}
.desplegar-enter-from,
.desplegar-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}

.card-enter-active {
  transition: opacity 0.25s ease, transform 0.25s ease;
}
.card-leave-active {
  transition: opacity 0.15s ease, transform 0.15s ease;
}
.card-enter-from {
  opacity: 0;
  transform: translateY(14px) scale(0.98);
}
.card-leave-to {
  opacity: 0;
  transform: scale(0.96);
}
.card-move {
  transition: transform 0.3s ease;
}
</style>
