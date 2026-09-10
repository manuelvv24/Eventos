import api from './api.js'

export default {
  getAll:       ()              => api.get('/usuarios'),
  getById:      (id)            => api.get(`/usuarios/${id}`),
  create:       (data)          => api.post('/usuarios', data),
  update:       (id, data)      => api.put(`/usuarios/${id}`, data),
  remove:       (id)            => api.delete(`/usuarios/${id}`),
  cambiarEstado:(id, estado)    => api.patch(`/usuarios/${id}/estado`, { estado }),
  cambiarRol:   (id, idRol)     => api.patch(`/usuarios/${id}/rol`, { idRol }),
}
