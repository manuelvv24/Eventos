import api from './api.js'

export default {
  getAll:  ()         => api.get('/roles'),
  getById: (id)       => api.get(`/roles/${id}`),
  create:  (data)     => api.post('/roles', data),
  update:  (id, data) => api.put(`/roles/${id}`, data),
  remove:  (id)       => api.delete(`/roles/${id}`),
}
