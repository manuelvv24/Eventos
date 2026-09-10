import api from './api.js'

export default {
  listar:      () => api.get('/notificaciones'),
  noLeidas:    () => api.get('/notificaciones/no-leidas'),
  marcarLeida: (id) => api.put(`/notificaciones/${id}/leer`),
  marcarTodas: () => api.put('/notificaciones/leer-todas'),
}