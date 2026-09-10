package Eventos.eventos.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import jakarta.persistence.EntityListeners;
import Eventos.eventos.audit.AuditEntityListener;

@Entity
@EntityListeners(AuditEntityListener.class)
@Table(name = "importaciones")
@Getter
@Setter
public class Importacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_importacion")
    private Long idImportacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario")
    private Usuario usuario;

    @Column(name = "nombre_archivo_importacion", length = 255)
    private String nombreArchivoImportacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_evento")
    private Evento evento;

    @Column(name = "total_fila_importacion")
    private Integer totalFilaImportacion;

    @Column(name = "fila_exitosa_importacion")
    private Integer filaExitosaImportacion;

    @Column(name = "fila_error_importacion")
    private Integer filaErrorImportacion;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_estado", nullable = false)
    private Estado estado;

    @Column(name = "fecha_importacion")
    private LocalDateTime fechaImportacion;

    @PrePersist
    protected void onCreate() {
        this.fechaImportacion = LocalDateTime.now();
    }
}