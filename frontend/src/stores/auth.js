import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import authAPI from '../services/authAPI.js'
import { ROLES, homeRouteForRol } from '../constants/index.js'
import { getToken, getUser, setSession, clearSession } from '../utils/session.js'

// Re-exportado para compatibilidad con vistas que importan ROLES desde el store
export { ROLES }

/** Menú de navegación filtrado por rol */
export const NAV_ITEMS = [
  {
    label: 'Panel de control',
    icon: 'dashboard',
    route: '/app/dashboard',
    roles: [ROLES.SUPER_ADMIN, ROLES.ADMIN, ROLES.MONITOR],
  },
  {
    label: 'Eventos',
    icon: 'event',
    route: '/app/eventos',
    roles: [ROLES.SUPER_ADMIN, ROLES.ADMIN, ROLES.OPERADOR, ROLES.MONITOR, ROLES.INVITADO],
  },
  {
    label: 'Participantes',
    icon: 'group',
    route: '/app/participantes',
    roles: [ROLES.SUPER_ADMIN, ROLES.ADMIN, ROLES.OPERADOR, ROLES.MONITOR],
  },
  {
    label: 'QR',
    icon: 'qr_code_scanner',
    route: '/app/check-in',
    roles: [ROLES.SUPER_ADMIN, ROLES.ADMIN, ROLES.OPERADOR],
  },
  {
    label: 'Importar CSV',
    icon: 'upload_file',
    route: '/app/importacion',
    roles: [ROLES.SUPER_ADMIN, ROLES.ADMIN, ROLES.OPERADOR],
  },
  {
    label: 'Certificados',
    icon: 'workspace_premium',
    route: '/app/certificados',
    roles: [ROLES.SUPER_ADMIN, ROLES.ADMIN, ROLES.OPERADOR, ROLES.MONITOR],
  },
  {
    label: 'Plantilla Certificado',
    icon: 'palette',
    route: '/app/plantilla-certificado',
    roles: [ROLES.SUPER_ADMIN, ROLES.ADMIN, ROLES.OPERADOR],
  },
  {
    label: 'Usuarios',
    icon: 'manage_accounts',
    route: '/app/usuarios',
    roles: [ROLES.SUPER_ADMIN, ROLES.ADMIN],
  },
  {
    label: 'Roles',
    icon: 'shield',
    route: '/app/roles',
    roles: [ROLES.SUPER_ADMIN, ROLES.ADMIN],
  },
  {
    label: 'Papelera',
    icon: 'restore',
    route: '/app/restaurar',
    roles: [ROLES.SUPER_ADMIN],
  },
  {
    label: 'Auditoría',
    icon: 'history',
    route: '/app/auditoria',
    roles: [ROLES.SUPER_ADMIN, ROLES.ADMIN],
  },
  {
    label: 'Mis Eventos',
    icon: 'confirmation_number',
    route: '/app/mis-eventos',
    roles: [ROLES.INVITADO],
  },
  {
    label: 'Mis Certificados',
    icon: 'workspace_premium',
    route: '/app/mis-certificados',
    roles: [ROLES.INVITADO],
  },
]

export const useAuthStore = defineStore('auth', () => {
  // ── State ────────────────────────────────────────────────────────────────
  const token = ref(getToken())
  const user = ref(getUser())
  const loading = ref(false)
  const error = ref(null)

  // ── Getters ──────────────────────────────────────────────────────────────
  const isAuthenticated = computed(() => !!token.value && !!user.value)
  const rol = computed(() => user.value?.rol || null)
  const nombreCompleto = computed(() => user.value?.nombreCompleto || user.value?.emailUsuario || '')
  const idUsuario = computed(() => user.value?.idUsuario || null)

  /** Rutas de navegación visibles para el rol actual */
  const navItems = computed(() =>
    NAV_ITEMS.filter(item => !item.roles || item.roles.includes(rol.value))
  )

  /** Comprueba si el usuario tiene uno de los roles dados.
   *  El Super Administrador hereda todos los permisos de los demás roles. */
  const hasRole = (...roles) => {
    if (!rol.value) return false
    if (rol.value === ROLES.SUPER_ADMIN) return true
    return roles.includes(rol.value)
  }

  /** Ruta home por rol (a dónde redirigir después del login) */
  const homeRoute = computed(() => homeRouteForRol(rol.value))

  // ── Actions ──────────────────────────────────────────────────────────────
  async function login(email, password) {
    loading.value = true
    error.value = null
    try {
      const { data } = await authAPI.login({ email, password })
      _persistSession(data)
      return data
    } catch (err) {
      error.value = err.response?.data?.error || 'Error al iniciar sesión'
      throw new Error(error.value)
    } finally {
      loading.value = false
    }
  }

  async function registro(payload) {
    loading.value = true
    error.value = null
    try {
      const { data } = await authAPI.registro(payload)
      _persistSession(data)
      return data
    } catch (err) {
      error.value = err.response?.data?.error || 'Error al registrarse'
      const e = new Error(error.value)
      e.campo = err.response?.data?.campo
      throw e
    } finally {
      loading.value = false
    }
  }

  function logout() {
    token.value = null
    user.value = null
    clearSession()
  }

  function _persistSession(data) {
    token.value = data.token
    user.value = {
      idUsuario: data.idUsuario,
      emailUsuario: data.emailUsuario,
      nombreCompleto: data.nombreCompleto,
      rol: data.rol,
      roles: Array.isArray(data.roles) && data.roles.length ? data.roles : [data.rol],
    }
    setSession(token.value, user.value)
  }

  async function cambiarRol(rol) {
    loading.value = true
    error.value = null
    try {
      const { data } = await authAPI.cambiarRol(rol)
      _persistSession(data)
      return data
    } catch (err) {
      error.value = err.response?.data?.error || 'Error al cambiar de rol'
      throw new Error(error.value)
    } finally {
      loading.value = false
    }
  }

  return {
    // state
    token,
    user,
    loading,
    error,
    // getters
    isAuthenticated,
    rol,
    nombreCompleto,
    idUsuario,
    navItems,
    homeRoute,
    // methods
    hasRole,
    login,
    registro,
    logout,
    cambiarRol,
  }
})
