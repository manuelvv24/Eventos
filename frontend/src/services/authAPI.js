import api from './api.js'

export default {
  login:    (data) => api.post('/auth/login', data),
  registro: (data) => api.post('/auth/registro', data),
  verificarDisponibilidad: (data) => api.post('/auth/verificar-disponibilidad', data),
  recuperar:      (email) => api.post('/auth/recuperar', { email }),
  verificarCodigo: (email, codigo) => api.post('/auth/verificar-codigo', { email, codigo }),
  restablecer:    (token, nuevaPassword) => api.post('/auth/restablecer', { token, nuevaPassword }),
  cambiarRol:     (rol) => api.post('/auth/cambiar-rol', { rol }),
}
