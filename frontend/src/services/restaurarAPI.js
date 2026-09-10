import api from './api.js'

export default {
  usuarios:           () => api.get('/restaurar/usuarios'),
  restaurarUsuario:   (id) => api.post(`/restaurar/usuarios/${id}`),
  asistencias:        () => api.get('/restaurar/asistencias'),
  restaurarAsistencia:(id) => api.post(`/restaurar/asistencias/${id}`),
  roles:              () => api.get('/restaurar/roles'),
  restaurarRol:       (id) => api.post(`/restaurar/roles/${id}`),
  configuraciones:    () => api.get('/restaurar/configuraciones-certificado'),
  restaurarConfiguracion:(id) => api.post(`/restaurar/configuraciones-certificado/${id}`),
  eventos:            () => api.get('/restaurar/eventos'),
  restaurarEvento:    (id) => api.post(`/restaurar/eventos/${id}`),
  participantes:      () => api.get('/restaurar/participantes'),
  restaurarParticipante:(id) => api.post(`/restaurar/participantes/${id}`),
}