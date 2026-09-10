import api from './api.js'

export default {
  listarTodas:          ()              => api.get('/configuracion-certificado'),
  obtenerPorId:         (id)            => api.get(`/configuracion-certificado/${id}`),
  crear:                (data)          => api.post('/configuracion-certificado', data),
  actualizar:           (id, data)      => api.put(`/configuracion-certificado/${id}`, data),
  eliminar:             (id)            => api.delete(`/configuracion-certificado/${id}`),
  marcarPredeterminada: (id)            => api.patch(`/configuracion-certificado/${id}/predeterminada`),
  previewPdf:           (idPlantilla)   => api.get(`/configuracion-certificado/preview-pdf${idPlantilla ? '?idPlantilla=' + idPlantilla : ''}`, { responseType: 'blob' }),
}
