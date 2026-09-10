package Eventos.eventos.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import jakarta.persistence.EntityListeners;
import Eventos.eventos.audit.AuditEntityListener;

@Entity
@EntityListeners(AuditEntityListener.class)
@Table(name = "certificado")
@Getter
@Setter
public class Certificado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_certificado")
    private Long idCertificado;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_registro_asistencia", nullable = false)
    private RegistroAsistencia registroAsistencia;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_estado", nullable = false)
    private Estado estado;

    @Column(name = "url_pdf_certificado", length = 255)
    private String urlPdfCertificado;

    @Column(name = "created_at_certificado", updatable = false)
    private LocalDateTime createdAtCertificado;

    @Column(name = "updated_at_certificado")
    private LocalDateTime updatedAtCertificado;

    @Column(name = "codigo_verificacion_certificado", length = 100, unique = true)
    private String codigoVerificacionCertificado;

    @Column(name = "fecha_emision_certificado")
    private LocalDateTime fechaEmisionCertificado;

    @PrePersist
    protected void onCreate() {
        this.createdAtCertificado = LocalDateTime.now();
        this.updatedAtCertificado = LocalDateTime.now();
        if (this.fechaEmisionCertificado == null) {
            this.fechaEmisionCertificado = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAtCertificado = LocalDateTime.now();
    }
}