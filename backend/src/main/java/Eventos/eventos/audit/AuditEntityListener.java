package Eventos.eventos.audit;

import jakarta.persistence.*;
import org.springframework.context.ApplicationEventPublisher;
import java.lang.reflect.Field;

public class AuditEntityListener {

    @PostPersist
    public void postPersist(Object entity) { publicar(entity, "CREATE"); }

    @PostUpdate
    public void postUpdate(Object entity) { publicar(entity, "UPDATE"); }

    @PostRemove
    public void postRemove(Object entity) { publicar(entity, "DELETE"); }

    private void publicar(Object entity, String accion) {
    
        if (entity.getClass().getSimpleName().equals("LogAuditoria")) return;

        String entidad = entity.getClass().getSimpleName();
        Long entidadId = extraerId(entity);
        String verbo;
        switch (accion) {
            case "CREATE": verbo = "creó"; break;
            case "UPDATE": verbo = "actualizó"; break;
            case "EXPORT": verbo = "exportó"; break;
            default: verbo = "realizó"; break;
        }
        String detalle = String.format("Admin ID %s %s el registro de %s con ID %s", SeguridadUtils.idUsuarioActual(), verbo, entidad, entidadId != null ? entidadId.toString() : "-");
        SpringContext.getEventPublisher()
                .publishEvent(new EventoAuditoria(
                accion, entidad, entidadId,
                detalle,
                SeguridadUtils.idUsuarioActual(),
                SeguridadUtils.ipActual()));
    }

    private Long extraerId(Object entity) {
        try {
            for (Field f : entity.getClass().getDeclaredFields()) {
                if (f.isAnnotationPresent(Id.class)) {
                    f.setAccessible(true);
                    if (f.get(entity) instanceof Long l) return l;
                }
            }
        } catch (Exception ignored) { }
        return null;
    }
}