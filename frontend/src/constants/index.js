/**
 * Constantes compartidas de la aplicación.
 * Única fuente de verdad para roles y rutas de inicio.
 * Los valores deben coincidir exactamente con los del backend.
 */
export const ROLES = {
  SUPER_ADMIN: 'Super Administrador',
  ADMIN: 'Administrador',
  OPERADOR: 'Operador',
  MONITOR: 'Monitor',
  INVITADO: 'Invitado',
}

/** Ruta home según el rol (destino tras login o acceso denegado) */
export function homeRouteForRol(rol) {
  switch (rol) {
    case ROLES.SUPER_ADMIN:
    case ROLES.ADMIN:
    case ROLES.MONITOR:
      return '/app/dashboard'
    case ROLES.OPERADOR:
      return '/app/check-in'
    default:
      return '/app/eventos'
  }
}

/** Nombre del panel que carga cada rol (para el selector de rol en el login) */
export function panelLabel(rol) {
  switch (rol) {
    case ROLES.SUPER_ADMIN:
    case ROLES.ADMIN:
    case ROLES.MONITOR:
      return 'Panel de control'
    case ROLES.OPERADOR:
      return 'QR'
    default:
      return 'Eventos'
  }
}
