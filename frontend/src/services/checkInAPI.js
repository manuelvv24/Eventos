import api from './api.js'

export default {
  /** Check-in manual por ID de registro */
  checkInManual: (data) => api.post('/check-in', data),

  /** Buscar inscripciones por número de identificación */
  buscarPorDocumento: (numeroDocumento, tipoDocumento) =>
    api.get('/check-in/buscar', {
      params: { numeroDocumento, ...(tipoDocumento ? { tipoDocumento } : {}) }
    }),
  
  /** Check-in por código QR */
  checkInQr: (codigoQr) => api.post('/check-in/qr', { codigoQr }),
  
  /** Listar todos los check-ins */
  getAll: () => api.get('/check-in'),
  
  /** Check-ins por ID de asistencia */
  porAsistencia: (id) => api.get(`/check-in/asistencia/${id}`),

  // ========== CHECK-IN MASIVO ==========
  
  /** Previsualizar cuántos hay pendientes antes de marcar */
  previewMasivo: (idEvento) => api.get(`/check-in/masivo/preview/${idEvento}`),
  
  /** Ejecutar check-in masivo */
  ejecutarMasivo: (payload) => api.post('/check-in/masivo/confirmar', payload)
}