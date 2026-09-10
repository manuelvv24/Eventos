package Eventos.eventos.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import jakarta.persistence.EntityListeners;
import Eventos.eventos.audit.AuditEntityListener;

@Entity
@EntityListeners(AuditEntityListener.class)
@Table(name = "log_auditoria")
@Getter
@Setter
public class LogAuditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_log_auditoria")
    private Long idLogAuditoria;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario")
    private Usuario usuario;

    @Column(name = "accion_log_auditoria", nullable = false, length = 100)
    private String accionLogAuditoria;

    @Column(name = "entidad_log_auditoria", length = 100)
    private String entidadLogAuditoria;

    @Column(name = "id_entidad_log_auditoria")
    private Long idEntidadLogAuditoria;

    @Column(name = "detalle_log_auditoria", columnDefinition = "TEXT")
    private String detalleLogAuditoria;

    @Column(name = "ip_origen_log_auditoria", length = 45)
    private String ipOrigenLogAuditoria;

    @Column(name = "fecha_log_auditoria")
    private LocalDateTime fechaLogAuditoria;

    @PrePersist
    protected void onCreate() {
        this.fechaLogAuditoria = LocalDateTime.now();
    }
}