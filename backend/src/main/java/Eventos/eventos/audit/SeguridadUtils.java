package Eventos.eventos.audit;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

public final class SeguridadUtils {

    private SeguridadUtils() { }

    public static Long idUsuarioActual() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null) {
                Object principal = auth.getPrincipal();
                if (principal instanceof Long l) return l;
                
                if (auth.getCredentials() instanceof Long id) return id;
                try {
                    java.lang.reflect.Method m = principal.getClass().getMethod("getId");
                    Object id = m.invoke(principal);
                    if (id instanceof Long l) return l;
                } catch (Exception ignored) { }
            }
        } catch (Exception ignored) { }
        return null;
    }

    public static String ipActual() {
        try {
            ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest req = attrs.getRequest();
                String fwd = req.getHeader("X-Forwarded-For");
                return (fwd != null && !fwd.isBlank()) ? fwd.split(",")[0].trim() : req.getRemoteAddr();
            }
        } catch (Exception ignored) { }
        return "desconocida";
    }
}