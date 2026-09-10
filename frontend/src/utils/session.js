/**
 * Acceso centralizado a la sesión persistida en localStorage.
 * Único punto que conoce las claves de almacenamiento;
 * usado por api.js (interceptor), el router y el store de auth.
 */
const TOKEN_KEY = 'token'
const USER_KEY = 'user'

export function getToken() {
  return localStorage.getItem(TOKEN_KEY)
}

/** Lee el usuario guardado sin fallar si el JSON está corrupto */
export function getUser() {
  try {
    return JSON.parse(localStorage.getItem(USER_KEY) || 'null')
  } catch {
    return null
  }
}

export function setSession(token, user) {
  localStorage.setItem(TOKEN_KEY, token)
  localStorage.setItem(USER_KEY, JSON.stringify(user))
}

export function clearSession() {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(USER_KEY)
}
