
import api from './api.js'

export default {
  resumen:   () => api.get('/dashboard/resumen'),
  eventos:   (params) => api.get('/dashboard/eventos', { params }),
  heatmap:   () => api.get('/dashboard/heatmap'),
  graficos:  () => api.get('/dashboard/graficos'),
  auditoria: (params) => api.get('/auditoria', { params }),
  asistencias: () => api.get('/asistencias'),
}