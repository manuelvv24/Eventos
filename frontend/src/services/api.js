import axios from 'axios'
import { getToken, clearSession } from '../utils/session.js'

/**
 * Normaliza la URL base: siempre debe terminar en /api.
 * En desarrollo, si no se define VITE_API_URL se usa el proxy de Vite (/api)
 * configurado en vite.config.js.
 */
function buildBaseURL() {
  const raw = import.meta.env.VITE_API_URL || '/api'
  if (raw.endsWith('/api') || raw.endsWith('/api/')) return raw.replace(/\/$/, '')
  return raw.replace(/\/$/, '') + '/api'
}

export const API_BASE = buildBaseURL()

const api = axios.create({
  baseURL: API_BASE,
  headers: { 'Content-Type': 'application/json' },
})

// Inyecta el JWT en cada request si existe
api.interceptors.request.use(config => {
  const token = getToken()
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

// Manejo global de errores
api.interceptors.response.use(
  res => res,
  err => {
    const status = err.response?.status

    // 401 = token expirado o inválido → cerrar sesión y redirigir al login
    // 403 = sin permiso (rol insuficiente) → NO cerrar sesión, dejar que la UI lo maneje
    if (status === 401 && getToken()) {
      clearSession()
      window.location.replace('/login')
    }

    return Promise.reject(err)
  }
)

export default api
