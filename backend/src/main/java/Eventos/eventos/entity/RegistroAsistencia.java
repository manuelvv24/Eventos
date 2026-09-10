package Eventos.eventos.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import jakarta.persistence.EntityListeners;
import Eventos.eventos.audit.AuditEntityListener;

@Entity
@EntityListeners(AuditEntityListener.class)
@Table(name = "registro_asistencia")
@Getter
@Setter
public class RegistroAsistencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_registro_asistencia")
    private Long idRegistroAsistencia;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_eventos", nullable = false)
    private Evento evento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_participantes", nullable = false)
    private Participante participante;

    @Column(name = "fecha_asistencia")
    private LocalDateTime fechaAsistencia;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_estado", nullable = false)
    private Estado estado;

    @Column(name = "codigo_qr_inscripcion", length = 255)
    private String codigoQrInscripcion;

    @Column(name = "metodo_check_in", length = 50)
    private String metodoCheckIn;

    @Column(name = "fecha_check_in")
    private LocalDateTime fechaCheckIn;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_sesion")
    private Sesion sesion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_operador")
    private Usuario operador;

    @Column(name = "ip_check_in", length = 45)
    private String ipCheckIn;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.fechaAsistencia == null) {
            this.fechaAsistencia = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public boolean estaEliminado() {
        return estado != null && "ELIMINADO".equalsIgnoreCase(estado.getNombreEstado());
    }
}