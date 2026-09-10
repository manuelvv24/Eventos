package Eventos.eventos.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "configuracion_certificado")
@Getter
@Setter
public class ConfiguracionCertificado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_configuracion")
    private Long idConfiguracion;

    /** Nombre descriptivo de la plantilla (ej. "Plantilla Institucional EAN", "Plantilla Elegante Gold") */
    @Column(name = "nombre_plantilla", length = 150)
    private String nombrePlantilla;

    /** Si esta plantilla es la predeterminada del sistema */
    @Column(name = "es_predeterminada")
    private Boolean esPredeterminada = false;

    /** Nombre de la organización que expide el certificado */
    @Column(name = "nombre_organizacion", length = 200)
    private String nombreOrganizacion;

    /** Título que aparece en el certificado (ej. "Certificado de Participación") */
    @Column(name = "titulo_certificado", length = 200)
    private String tituloCertificado;

    /** Texto descriptivo del cuerpo (puede incluir placeholder [[EVENTO]] y [[DURACION]]) */
    @Column(name = "texto_cuerpo", columnDefinition = "TEXT")
    private String textoCuerpo;

    /** Color CSS del borde principal (ej. #000e2d) */
    @Column(name = "color_borde", length = 20)
    private String colorBorde;

    /** Color CSS de las esquinas y líneas doradas (ej. #7b580e) */
    @Column(name = "color_acento", length = 20)
    private String colorAcento;

    /** Logo de la organización en Base64 (data:image/...;base64,...) */
    @Column(name = "logo_base64", columnDefinition = "TEXT")
    private String logoBase64;

    // ── Firma 1 ───────────────────────────────────────────────────────────────
    @Column(name = "firma1_nombre", length = 150)
    private String firma1Nombre;

    @Column(name = "firma1_cargo", length = 150)
    private String firma1Cargo;

    @Column(name = "firma1_imagen_base64", columnDefinition = "TEXT")
    private String firma1ImagenBase64;

    // ── Firma 2 ───────────────────────────────────────────────────────────────
    @Column(name = "firma2_nombre", length = 150)
    private String firma2Nombre;

    @Column(name = "firma2_cargo", length = 150)
    private String firma2Cargo;

    @Column(name = "firma2_imagen_base64", columnDefinition = "TEXT")
    private String firma2ImagenBase64;

    // ── Firma 3 ───────────────────────────────────────────────────────────────
    @Column(name = "firma3_nombre", length = 150)
    private String firma3Nombre;

    @Column(name = "firma3_cargo", length = 150)
    private String firma3Cargo;

    @Column(name = "firma3_imagen_base64", columnDefinition = "TEXT")
    private String firma3ImagenBase64;

    /** Si es la configuración actualmente activa */
    @Column(name = "activa")
    private Boolean activa = true;

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;

    @PrePersist
    @PreUpdate
    protected void onSave() {
        this.fechaActualizacion = LocalDateTime.now();
    }
}
