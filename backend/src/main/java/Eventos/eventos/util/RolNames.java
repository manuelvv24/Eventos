package Eventos.eventos.util;

import Eventos.eventos.entity.Rol;

import java.util.Collection;

/**
 * Nombres canónicos de roles (primera letra en mayúscula, resto en minúscula).
 * Son el único origen de verdad: se guardan en BD, viajan en el JWT
 * (claim "rol"), se usan como autoridades Spring Security ("ROLE_<nombre>")
 * y se muestran tal cual en el frontend.
 */
public final class RolNames {

    public static final String SUPER_ADMIN = "Super Administrador";
    public static final String ADMIN       = "Administrador";
    public static final String OPERADOR    = "Operador";
    public static final String MONITOR     = "Monitor";
    public static final String INVITADO    = "Invitado";

    /** Orden de jerarquía (mayor a menor). */
    private static final String[] PRIORIDAD = { SUPER_ADMIN, ADMIN, OPERADOR, MONITOR, INVITADO };

    private RolNames() {
    }

    public static boolean contiene(Collection<Rol> roles, String nombre) {
        if (roles == null || nombre == null) return false;
        for (Rol r : roles) {
            if (r.getNombreRol() != null && r.getNombreRol().equalsIgnoreCase(nombre)) return true;
        }
        return false;
    }

    public static boolean esSuperAdmin(Collection<Rol> roles) {
        return contiene(roles, SUPER_ADMIN);
    }

    /**
     * Roles de personal organizador (no participan como asistentes).
     * Solo los usuarios que NO tienen ninguno de estos roles pueden inscribirse
     * en eventos y recibir certificados.
     */
    private static final String[] PERSONAL = { SUPER_ADMIN, ADMIN, OPERADOR, MONITOR };

    public static boolean esPersonal(Collection<Rol> roles) {
        if (roles == null) return false;
        for (String candidato : PERSONAL) {
            if (contiene(roles, candidato)) return true;
        }
        return false;
    }

    /** Devuelve el rol de mayor jerarquía del usuario (determinista para la sesión). */
    public static String primario(Collection<Rol> roles) {
        if (roles == null || roles.isEmpty()) return null;
        for (String candidato : PRIORIDAD) {
            if (contiene(roles, candidato)) return candidato;
        }
        return roles.iterator().next().getNombreRol();
    }
}