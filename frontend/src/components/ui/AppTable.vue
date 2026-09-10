<template>
  <AppCard :title="title" :subtitle="subtitle" :shadow="shadow" padding="none">
    <!-- Header action slot (título con botón) -->
    <template v-if="$slots['header-action']" #header-action>
      <slot name="header-action" />
    </template>

    <!-- Barra superior: tabs + búsqueda + filtros + exportar -->
    <div class="px-4 py-3 border-b border-outline-variant/20 bg-surface-container-lowest flex flex-col gap-3">
      <!-- Tabs -->
      <div v-if="tabs && tabs.length" class="flex items-center gap-2 overflow-x-auto no-scrollbar">
        <button
          v-for="tab in tabs"
          :key="tab"
          class="px-4 py-1.5 rounded-full text-label-md whitespace-nowrap transition-colors"
          :class="activeTab === tab
            ? 'bg-primary-fixed text-primary font-semibold'
            : 'text-on-surface-variant hover:bg-surface-container'"
          @click="selectTab(tab)"
        >
          {{ tab }}
        </button>
      </div>

      <!-- Búsqueda + filtros slot + exportar -->
      <div v-if="searchable || $slots.filters || exportable" class="flex flex-col sm:flex-row gap-3 items-center">
        <div v-if="searchable" class="relative flex-1 max-w-sm w-full">
          <span class="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-outline text-[18px]">search</span>
          <input
            v-model="searchQuery"
            :placeholder="searchPlaceholder"
            class="w-full pl-9 pr-4 py-2 rounded-lg border border-outline-variant text-body-sm bg-white focus:outline-none focus:border-primary focus:ring-1 focus:ring-primary/20 transition-all"
          />
        </div>
        <div class="flex items-center gap-2 flex-wrap">
          <slot name="filters" />
          <button
            v-if="exportable"
            class="px-3 py-2 rounded-lg border border-outline-variant text-on-surface-variant hover:bg-surface-container transition-colors flex items-center gap-1 text-label-sm"
            title="Exportar CSV"
            @click="exportCSV"
          >
            <span class="material-symbols-outlined text-[18px]">download</span>
            <span class="hidden sm:inline">Exportar</span>
          </button>
        </div>
      </div>
    </div>

    <!-- Loading -->
    <div v-if="loading" class="flex flex-col items-center justify-center py-16 gap-3 text-on-surface-variant">
      <AppSpinner size="lg" class="text-primary" />
      <span class="text-body-md">{{ loadingText }}</span>
    </div>

    <!-- Error -->
    <div v-else-if="error" class="flex flex-col items-center justify-center py-16 gap-3 text-error">
      <span class="material-symbols-outlined text-[48px]">error_outline</span>
      <p class="text-body-md font-medium">{{ error }}</p>
      <AppButton variant="outlined" icon="refresh" size="sm" @click="$emit('retry')">Reintentar</AppButton>
    </div>

    <!-- Tabla -->
    <div v-else class="overflow-x-auto">
      <table class="w-full text-left border-collapse">
        <thead class="bg-surface-container-low/50">
          <tr>
            <!-- Checkbox select-all -->
            <th v-if="selectable" class="px-4 py-3 w-10">
              <input
                type="checkbox"
                :checked="isAllSelected"
                class="w-4 h-4 rounded border-outline-variant text-primary focus:ring-primary cursor-pointer"
                @change="toggleSelectAll"
              />
            </th>

            <th
              v-for="col in columns"
              :key="col.key"
              class="px-5 py-3 text-label-lg font-bold text-on-surface border-b border-outline-variant/20 whitespace-nowrap select-none"
              :class="[col.headerClass, col.sortable !== false ? 'cursor-pointer hover:text-primary transition-colors' : '', sortKey === col.key ? 'text-primary' : '']"
              @click="col.sortable !== false ? sortBy(col.key) : null"
            >
              <div class="flex items-center gap-1">
                {{ col.label }}
                <span v-if="col.sortable !== false" class="material-symbols-outlined text-[15px]">
                  {{ sortKey === col.key ? (sortOrder === 'asc' ? 'arrow_upward' : 'arrow_downward') : 'unfold_more' }}
                </span>
              </div>
            </th>

            <th v-if="$slots.actions" class="px-5 py-3 text-label-lg font-bold text-on-surface border-b border-outline-variant/20 text-right">
              Acciones
            </th>
          </tr>
        </thead>

        <tbody class="divide-y divide-outline-variant/10">
          <tr
            v-for="(row, idx) in paginatedRows"
            :key="row[rowKey] ?? idx"
            class="hover:bg-surface-container-lowest transition-colors group"
            :class="[rowClass ? rowClass(row) : '', selectable && selectedRows.includes(row[rowKey]) ? 'bg-primary-fixed/20' : '']"
          >
            <!-- Checkbox por fila -->
            <td v-if="selectable" class="px-4 py-3">
              <input
                type="checkbox"
                :checked="selectedRows.includes(row[rowKey])"
                class="w-4 h-4 rounded border-outline-variant text-primary focus:ring-primary cursor-pointer"
                @change="toggleSelectRow(row[rowKey])"
              />
            </td>

            <td
              v-for="col in columns"
              :key="col.key"
              class="px-5 py-3.5 text-body-sm text-on-surface"
              :class="col.cellClass"
            >
              <!-- Compatibilidad con slot antiguo :row y nuevo :item -->
              <slot :name="`cell-${col.key}`" :row="row" :item="row" :value="row[col.key]">
                {{ row[col.key] ?? '—' }}
              </slot>
            </td>

            <td v-if="$slots.actions" class="px-5 py-3.5 text-right">
              <div class="flex items-center justify-end gap-1">
                <slot name="actions" :row="row" :item="row" />
              </div>
            </td>
          </tr>

          <!-- Vacío -->
          <tr v-if="paginatedRows.length === 0">
            <td :colspan="columns.length + ($slots.actions ? 1 : 0) + (selectable ? 1 : 0)" class="px-5 py-16 text-center">
              <div class="flex flex-col items-center gap-2 text-on-surface-variant">
                <span class="material-symbols-outlined text-[48px]">{{ emptyIcon }}</span>
                <p class="text-body-md font-medium">{{ emptyText }}</p>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- Footer: paginación + tamaño de página -->
    <div
      v-if="!loading && !error && filteredRows.length > 0"
      class="px-4 py-3 border-t border-outline-variant/20 bg-surface-container-low/30 flex flex-col sm:flex-row items-center justify-between gap-3"
    >
      <!-- Info -->
      <span class="text-body-sm text-on-surface-variant">
        {{ startIndex + 1 }}–{{ endIndex }} de {{ filteredRows.length }} registros
      </span>

      <!-- Páginas -->
      <div v-if="totalPages > 1" class="flex items-center gap-1">
        <button
          class="w-8 h-8 flex items-center justify-center rounded-lg border border-outline-variant text-on-surface-variant hover:bg-surface-container disabled:opacity-40 disabled:cursor-not-allowed transition-colors"
          :disabled="currentPage === 1"
          @click="currentPage--"
        >
          <span class="material-symbols-outlined text-[18px]">chevron_left</span>
        </button>

        <button
          v-for="p in visiblePages"
          :key="p"
          class="w-8 h-8 flex items-center justify-center rounded-lg text-label-sm transition-colors"
          :class="p === currentPage
            ? 'bg-primary text-white'
            : typeof p === 'number'
              ? 'border border-outline-variant text-on-surface-variant hover:bg-surface-container cursor-pointer'
              : 'text-on-surface-variant cursor-default'"
          @click="typeof p === 'number' && (currentPage = p)"
        >
          {{ p }}
        </button>

        <button
          class="w-8 h-8 flex items-center justify-center rounded-lg border border-outline-variant text-on-surface-variant hover:bg-surface-container disabled:opacity-40 disabled:cursor-not-allowed transition-colors"
          :disabled="currentPage === totalPages"
          @click="currentPage++"
        >
          <span class="material-symbols-outlined text-[18px]">chevron_right</span>
        </button>
      </div>

      <!-- Filas por página -->
      <div class="flex items-center gap-2">
        <select
          v-model="localPageSize"
          class="border border-outline-variant rounded-lg px-2 py-1 text-body-sm bg-surface outline-none focus:border-primary"
        >
          <option v-for="n in pageSizeOptions" :key="n" :value="n">{{ n }}</option>
        </select>
        <span class="text-body-sm text-on-surface-variant">/ página</span>
      </div>
    </div>
  </AppCard>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import AppCard    from './AppCard.vue'
import AppButton  from './AppButton.vue'
import AppSpinner from './AppSpinner.vue'

const props = defineProps({
  // ── Datos ────────────────────────────────────────────────────────────────
  columns:           { type: Array,    required: true },
  rows:              { type: Array,    default: () => [] },
  rowKey:            { type: String,   default: 'id' },

  // ── Header ───────────────────────────────────────────────────────────────
  title:             { type: String,   default: null },
  subtitle:          { type: String,   default: null },
  tabs:              { type: Array,    default: null },

  // ── Estado ───────────────────────────────────────────────────────────────
  loading:           { type: Boolean,  default: false },
  error:             { type: String,   default: null },

  // ── Búsqueda ─────────────────────────────────────────────────────────────
  searchable:        { type: Boolean,  default: true },
  searchPlaceholder: { type: String,   default: 'Buscar...' },
  searchFields:      { type: Array,    default: null },

  // ── Paginación ───────────────────────────────────────────────────────────
  pageSize:          { type: Number,   default: 10 },
  pageSizeOptions:   { type: Array,    default: () => [5, 10, 25, 50, 100] },

  // ── Ordenamiento inicial ─────────────────────────────────────────────────
  defaultSortKey:    { type: String,   default: '' },
  defaultSortOrder:  { type: String,   default: 'asc' },

  // ── Selección ────────────────────────────────────────────────────────────
  selectable:        { type: Boolean,  default: false },

  // ── Exportar ─────────────────────────────────────────────────────────────
  exportable:        { type: Boolean,  default: false },

  // ── Miscelánea ───────────────────────────────────────────────────────────
  shadow:            { type: Boolean,  default: true },
  emptyText:         { type: String,   default: 'No hay registros' },
  emptyIcon:         { type: String,   default: 'inbox' },
  loadingText:       { type: String,   default: 'Cargando...' },
  rowClass:          { type: Function, default: null },
})

const emit = defineEmits(['retry', 'filterTab', 'search', 'sort', 'update:selected'])

// ── Estado interno ────────────────────────────────────────────────────────
const searchQuery  = ref('')
const currentPage  = ref(1)
const localPageSize= ref(props.pageSize)
const sortKey      = ref(props.defaultSortKey)
const sortOrder    = ref(props.defaultSortOrder)
const activeTab    = ref(props.tabs?.[0] ?? null)
const selectedRows = ref([])

// Reset página al cambiar datos o búsqueda
watch([() => props.rows, searchQuery], () => { currentPage.value = 1 })
watch(() => props.pageSize, v => { localPageSize.value = v })

// ── Procesamiento de datos ─────────────────────────────────────────────────
const filteredRows = computed(() => {
  let result = [...props.rows]

  // Búsqueda
  if (searchQuery.value.trim()) {
    const q = searchQuery.value.toLowerCase()
    result = result.filter(row => {
      const fields = props.searchFields ?? Object.keys(row)
      return fields.some(f => String(row[f] ?? '').toLowerCase().includes(q))
    })
  }

  // Ordenamiento
  if (sortKey.value) {
    result.sort((a, b) => {
      const av = a[sortKey.value]
      const bv = b[sortKey.value]
      if (av == null) return 1
      if (bv == null) return -1
      if (typeof av === 'number' && typeof bv === 'number')
        return sortOrder.value === 'asc' ? av - bv : bv - av
      return sortOrder.value === 'asc'
        ? String(av).localeCompare(String(bv))
        : String(bv).localeCompare(String(av))
    })
  }

  return result
})

const totalPages  = computed(() => Math.max(1, Math.ceil(filteredRows.value.length / localPageSize.value)))
const startIndex  = computed(() => (currentPage.value - 1) * localPageSize.value)
const endIndex    = computed(() => Math.min(startIndex.value + localPageSize.value, filteredRows.value.length))

const paginatedRows = computed(() =>
  filteredRows.value.slice(startIndex.value, endIndex.value)
)

const visiblePages = computed(() => {
  const total = totalPages.value
  const cur   = currentPage.value
  if (total <= 7) return Array.from({ length: total }, (_, i) => i + 1)
  if (cur <= 4)          return [1, 2, 3, 4, 5, '...', total]
  if (cur >= total - 3)  return [1, '...', total-4, total-3, total-2, total-1, total]
  return [1, '...', cur-1, cur, cur+1, '...', total]
})

// ── Selección ─────────────────────────────────────────────────────────────
const isAllSelected = computed(() =>
  paginatedRows.value.length > 0 &&
  paginatedRows.value.every(r => selectedRows.value.includes(r[props.rowKey]))
)

function toggleSelectRow(id) {
  const i = selectedRows.value.indexOf(id)
  i > -1 ? selectedRows.value.splice(i, 1) : selectedRows.value.push(id)
  emit('update:selected', [...selectedRows.value])
}

function toggleSelectAll() {
  if (isAllSelected.value) {
    paginatedRows.value.forEach(r => {
      const i = selectedRows.value.indexOf(r[props.rowKey])
      if (i > -1) selectedRows.value.splice(i, 1)
    })
  } else {
    paginatedRows.value.forEach(r => {
      if (!selectedRows.value.includes(r[props.rowKey]))
        selectedRows.value.push(r[props.rowKey])
    })
  }
  emit('update:selected', [...selectedRows.value])
}

// ── Ordenamiento ──────────────────────────────────────────────────────────
function sortBy(key) {
  sortOrder.value = sortKey.value === key && sortOrder.value === 'asc' ? 'desc' : 'asc'
  sortKey.value   = key
  emit('sort', { key, order: sortOrder.value })
}

// ── Tabs ─────────────────────────────────────────────────────────────────
function selectTab(tab) {
  activeTab.value   = tab
  currentPage.value = 1
  emit('filterTab', tab)
}

// ── Exportar CSV ─────────────────────────────────────────────────────────
function exportCSV() {
  const headers = props.columns.map(c => c.label).join(',')
  const rows    = filteredRows.value.map(row =>
    props.columns.map(c => {
      const v = row[c.key]
      if (v == null) return ''
      const s = String(v).replace(/"/g, '""')
      return s.includes(',') || s.includes('"') || s.includes('\n') ? `"${s}"` : s
    }).join(',')
  )
  const csv  = [headers, ...rows].join('\n')
  const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' })
  const link = document.createElement('a')
  link.href     = URL.createObjectURL(blob)
  link.download = `export_${new Date().toISOString().slice(0,10)}.csv`
  link.click()
}
</script>

<style scoped>
.no-scrollbar::-webkit-scrollbar { display: none; }
.no-scrollbar { -ms-overflow-style: none; scrollbar-width: none; }
</style>
