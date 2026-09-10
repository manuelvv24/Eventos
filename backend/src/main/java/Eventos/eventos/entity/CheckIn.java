package Eventos.eventos.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import jakarta.persistence.EntityListeners;
import Eventos.eventos.audit.AuditEntityListener;

@Entity
@EntityListeners(AuditEntityListener.class)
@Table(name = "check_in")
@Getter
@Setter
public class CheckIn {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_check_in")
    private Long idCheckIn;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_registro_asistencia", nullable = false)
    private RegistroAsistencia registroAsistencia;

    @Column(name = "metodo_check_in", length = 50)
    private String metodoCheckIn;

    @Column(name = "id_usuario")
    private Long idUsuario;

    @Column(name = "ip_check_in", length = 45)
    private String ipCheckIn;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}