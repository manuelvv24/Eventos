package Eventos.eventos.dto;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LogAuditoriaDTO {
    private Long idLogAuditoria;
    private Long idUsuario;
    private String accionAuditoria;
    private String entidadAuditada;
    private Long idEntidadAuditada;
    private String detalleAuditoria;
    private String ipOrigenAuditoria;
    private LocalDateTime fechaAuditoria;
}