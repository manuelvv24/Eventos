import api from './api.js'

export default {
  getAll:    ()         => api.get('/participantes'),
  getById:   (id)       => api.get(`/participantes/${id}`),
  create:    (data)     => api.post('/participantes', data),
  update:    (id, data) => api.put(`/participantes/${id}`, data),
  remove:    (id)       => api.delete(`/participantes/${id}`),
  historial: (id)       => api.get(`/dashboard/participantes/${id}/historial`),
}
