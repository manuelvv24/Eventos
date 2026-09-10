import api from './api.js'

export default {
  registrar:      (data)       => api.post('/asistencias', data),
  cancelar:       (idRegistro) => api.delete(`/asistencias/${idRegistro}`),
  cupos:          (idEvento)   => api.get(`/eventos/${idEvento}/cupos`),
  porEvento:      (idEvento)   => api.get(`/asistencias/evento/${idEvento}`),
  listarTodas:    ()           => api.get('/asistencias'),
  miParticipante: ()           => api.get('/participantes/me'),
  misEventos:     ()           => api.get('/invitado/mis-eventos'),
}

