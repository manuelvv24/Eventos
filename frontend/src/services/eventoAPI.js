import api from './api.js'

export default {
  getAll:   ()         => api.get('/eventos'),
  getById:  (id)       => api.get(`/eventos/${id}`),
  create:   (data)     => api.post('/eventos', data),
  update:   (id, data) => api.put(`/eventos/${id}`, data),
  cancel:   (id)       => api.put(`/eventos/${id}/cancelar`),
  remove:   (id)       => api.delete(`/eventos/${id}`),
}
