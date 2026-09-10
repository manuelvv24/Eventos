import { defineStore } from 'pinia'
import { ref } from 'vue'
import dashboardAPI from '../services/dashboardAPI.js'

export const useDashboardStore = defineStore('dashboard', () => {
  const resumen  = ref(null)
  const eventos  = ref([])
  const loading  = ref(false)
  const error    = ref(null)

  async function fetchResumen() {
    loading.value = true; error.value = null
    try {
      const { data } = await dashboardAPI.resumen()
      resumen.value = data
    } catch (e) {
      error.value = e.response?.data?.error || 'Error al cargar resumen'
    } finally { loading.value = false }
  }

  async function fetchEventos(params = {}) {
    loading.value = true; error.value = null
    try {
      const { data } = await dashboardAPI.eventos(params)
      eventos.value = data
    } catch (e) {
      error.value = e.response?.data?.error || 'Error al cargar eventos del dashboard'
    } finally { loading.value = false }
  }

  return { resumen, eventos, loading, error, fetchResumen, fetchEventos }
})
