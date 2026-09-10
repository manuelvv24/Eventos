package Eventos.eventos.audit;

public record EventoAuditoria(
        String accion,
        String entidad,
        Long idEntidad,
        String detalle,
        Long idUsuario,
        String ip) {
}