package Eventos.eventos.entity;

import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import Eventos.eventos.audit.AuditEntityListener;
import java.util.List;

@Entity
@EntityListeners(AuditEntityListener.class)
@Table(name = "eventos")
@Getter
@Setter
public class Evento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_eventos")
    private Long idEventos;

    @Column(name = "nombre_evento", nullable = false, length = 150)
    private String nombreEvento;

    @Column(name = "descripcion_evento", columnDefinition = "TEXT")
    private String descripcionEvento;

    @Column(name = "tipo_evento", length = 50)
    private String tipoEvento;

    @Column(name = "modalidad_evento", length = 50)
    private String modalidadEvento;

    @Column(name = "fecha_inicio_evento")
    private LocalDateTime fechaInicioEvento;

    @Column(name = "fecha_fin_evento")
    private LocalDateTime fechaFinEvento;

    @Column(name = "Lugar_evento", length = 200)
    private String lugarEvento;

    @Column(name = "aforo_maximo_evento")
    private Integer aforoMaximoEvento;

    @Column(name = "duracion_evento")
    private Integer duracionEvento;

    /** Días de asistencia mínimos para hacerse acreedor al certificado. Null/0 = sin mínimo. */
    @Column(name = "dias_minimos_certificacion")
    private Integer diasMinimosCertificacion;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_estado", nullable = false)
    private Estado estado;

    @Column(name = "imagen_url", columnDefinition = "TEXT")
    private String imagenUrl;

    @Column(name = "enlace_url", columnDefinition = "TEXT")
    private String enlaceUrl;

    @Column(name = "fecha_limite_inscripcion")
    private LocalDateTime fechaLimiteInscripcion;

    @Column(name = "eliminado")
    private Boolean eliminado = false;

    @Column(name = "created_at_evento", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at_evento")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "evento", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Notificacion> notificaciones;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.eliminado == null) {
            this.eliminado = false;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}