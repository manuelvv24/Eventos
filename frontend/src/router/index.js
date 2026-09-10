import { createRouter, createWebHistory } from 'vue-router'
import { ROLES, homeRouteForRol } from '../constants/index.js'
import { getToken, getUser, clearSession } from '../utils/session.js'

const Login = () => import('../views/Auth/Login.vue')
const Registro = () => import('../views/Auth/Registro.vue')
const Recuperar = () => import('../views/Auth/Recuperar.vue')
const Verificar = () => import('../views/Public/VerificarCertificado.vue')
const Catalogo = () => import('../views/Public/Catalogo.vue')

const AppLayout = () => import('../components/layout/AppLayout.vue')

const Dashboard = () => import('../views/Dashboard/Dashboard.vue')
const ListaEventos = () => import('../views/Eventos/ListaEventos.vue')
const RegistrarEvento = () => import('../views/Eventos/RegistrarEvento.vue')
const DetalleEvento = () => import('../views/Eventos/DetalleEvento.vue')
const MisEventos = () => import('../views/MisEventos/MisEventos.vue')
const ListaParticipantes = () => import('../views/Participantes/ListaParticipantes.vue')
const CheckIn = () => import('../views/CheckIn/CheckIn.vue')
const ImportacionCSV = () => import('../views/Importacion/ImportacionCSV.vue')
const ListaCertificados = () => import('../views/Certificados/ListaCertificados.vue')
const MisCertificados = () => import('../views/Certificados/MisCertificados.vue')
const PlantillaCertificado = () => import('../views/Certificados/PlantillaCertificado.vue')
const ListaUsuarios = () => import('../views/Usuarios/ListaUsuarios.vue')
const ListaRoles = () => import('../views/Roles/ListaRoles.vue')
const Papelera = () => import('../views/Restaurar/Papelera.vue')
const Auditoria = () => import('../views/Auditoria/Auditoria.vue')

const routes = [
  {
    path: '/',
    name: 'Catalogo',
    component: Catalogo,
    meta: { public: true },
  },
  { path: '/login', name: 'Login', component: Login, meta: { public: true } },
  { path: '/registro', name: 'Registro', component: Registro, meta: { public: true } },
  { path: '/recuperar', name: 'Recuperar', component: Recuperar, meta: { public: true } },
  {
    path: '/verificar/codigo',
    name: 'VerificarCertificado',
    component: Verificar,
    meta: { public: true },
  },
  {
    path: '/app',
    component: AppLayout,
    meta: { requiresAuth: true },
    children: [
      {
        path: '',
        redirect: () => homeRouteForRol(getUser()?.rol),
      },
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: Dashboard,
        meta: { roles: [ROLES.SUPER_ADMIN, ROLES.ADMIN, ROLES.MONITOR] },
      },
      {
        path: 'mis-eventos',
        name: 'MisEventos',
        component: MisEventos,
        meta: { roles: [ROLES.INVITADO] },
      },
      {
        path: 'eventos',
        name: 'ListaEventos',
        component: ListaEventos,
        meta: { roles: [ROLES.SUPER_ADMIN, ROLES.ADMIN, ROLES.OPERADOR, ROLES.MONITOR, ROLES.INVITADO] },
      },
      {
        path: 'eventos/nuevo',
        name: 'RegistrarEvento',
        component: RegistrarEvento,
        meta: { roles: [ROLES.SUPER_ADMIN, ROLES.ADMIN, ROLES.OPERADOR] },
      },
      {
        path: 'eventos/:id/editar',
        name: 'EditarEvento',
        component: RegistrarEvento,
        meta: { roles: [ROLES.SUPER_ADMIN, ROLES.ADMIN, ROLES.OPERADOR] },
      },
      {
        path: 'eventos/:id',
        name: 'DetalleEvento',
        component: DetalleEvento,
        meta: { roles: [ROLES.SUPER_ADMIN, ROLES.ADMIN, ROLES.OPERADOR, ROLES.MONITOR, ROLES.INVITADO] },
      },
      {
        path: 'participantes',
        name: 'ListaParticipantes',
        component: ListaParticipantes,
        meta: { roles: [ROLES.SUPER_ADMIN, ROLES.ADMIN, ROLES.OPERADOR, ROLES.MONITOR] },
      },
      {
        path: 'check-in',
        name: 'CheckIn',
        component: CheckIn,
        meta: { roles: [ROLES.SUPER_ADMIN, ROLES.ADMIN, ROLES.OPERADOR] },
      },
      {
        path: 'importacion',
        name: 'ImportacionCSV',
        component: ImportacionCSV,
        meta: { roles: [ROLES.SUPER_ADMIN, ROLES.ADMIN, ROLES.OPERADOR] },
      },
      {
        path: 'certificados',
        name: 'ListaCertificados',
        component: ListaCertificados,
        meta: { roles: [ROLES.SUPER_ADMIN, ROLES.ADMIN, ROLES.OPERADOR, ROLES.MONITOR] },
      },
      {
        path: 'plantilla-certificado',
        name: 'PlantillaCertificado',
        component: PlantillaCertificado,
        meta: { roles: [ROLES.SUPER_ADMIN, ROLES.ADMIN, ROLES.OPERADOR] },
      },
      {
        path: 'usuarios',
        name: 'ListaUsuarios',
        component: ListaUsuarios,
        meta: { roles: [ROLES.SUPER_ADMIN, ROLES.ADMIN] },
      },
      {
        path: 'roles',
        name: 'ListaRoles',
        component: ListaRoles,
        meta: { roles: [ROLES.SUPER_ADMIN, ROLES.ADMIN] },
      },
      {
        path: 'auditoria',
        name: 'Auditoria',
        component: Auditoria,
        meta: { roles: [ROLES.SUPER_ADMIN, ROLES.ADMIN] },
      },
      {
        path: 'restaurar',
        name: 'Papelera',
        component: Papelera,
        meta: { roles: [ROLES.SUPER_ADMIN] },
      },
      {
        path: 'mis-certificados',
        name: 'MisCertificados',
        component: MisCertificados,
        meta: { roles: [ROLES.INVITADO] },
      },
    ],
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    redirect: '/',
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior: () => ({ top: 0 }),
})

router.beforeEach((to, _from, next) => {
  const user = getUser()
  const isAuthenticated = !!(getToken() && user)
  const rol = user?.rol || null

  if (to.meta.public) {
    if (isAuthenticated && (to.name === 'Login' || to.name === 'Registro')) {
      return next(homeRouteForRol(rol))
    }
    return next()
  }

  if (!isAuthenticated) {
    return next({ name: 'Login', query: { redirect: to.fullPath } })
  }

  // Sesión con rol obsoleto (previo al renombrado de roles): forzar re-login
  if (!Object.values(ROLES).includes(rol)) {
    clearSession()
    return next({ name: 'Login' })
  }

  const rolesRequeridos = to.meta.roles
  if (rolesRequeridos?.length > 0 && !rolesRequeridos.includes(rol)) {
    return next(homeRouteForRol(rol))
  }

  next()
})

export default router
