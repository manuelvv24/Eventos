package Eventos.eventos.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import Eventos.eventos.audit.AuditEntityListener;
import java.time.LocalDateTime;

/**
 * Credenciales de acceso y estado de autenticación del usuario.
 * Separa la autenticación (email, contraseña, bloqueos) del perfil (usuarios).
 */
@Entity
@EntityListeners(AuditEntityListener.class)
@Table(name = "login",
       uniqueConstraints = {
           @UniqueConstraint(name = "uk_login_email", columnNames = "email_usuario"),
           @UniqueConstraint(name = "uk_login_usuario", columnNames = "id_usuario")
       })
@Getter
@Setter
public class Login {

    public static final String ESTADO_ACTIVO     = "ACTIVO";
    public static final String ESTADO_INACTIVO   = "INACTIVO";
    public static final String ESTADO_BLOQUEADO  = "BLOQUEADO";
    public static final String ESTADO_ELIMINADO  = "ELIMINADO";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_login")
    private Long idLogin;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario", nullable = false, unique = true)
    private Usuario usuario;

    @Column(name = "email_usuario", nullable = false, unique = true, length = 100)
    private String emailUsuario;

    @Column(name = "contrasena_usuario", nullable = false)
    private String contrasenaUsuario;

    /**
     * Estado de la cuenta de acceso (tipo_estado = "LOGIN"):
     * ACTIVO, INACTIVO, BLOQUEADO, ELIMINADO.
     */
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "id_estado", nullable = false)
    private Estado estado;

    @Column(name = "intentos_fallidos", nullable = false)
    private Integer intentosFallidos = 0;

    @Column(name = "fecha_bloqueo")
    private LocalDateTime fechaBloqueo;

    @Column(name = "ultimo_intento_fallido")
    private LocalDateTime ultimoIntentoFallido;

    /** Correo alternativo al que se envía el código de recuperación de cuenta. */
    @Column(name = "email_recuperacion", length = 100)
    private String emailRecuperacion;

    /** Código de 6 dígitos para recuperación de cuenta (password reset). */
    @Column(name = "codigo_recuperacion", length = 6)
    private String codigoRecuperacion;

    /** Fecha/hora de expiración del código de recuperación. */
    @Column(name = "codigo_recuperacion_expira")
    private LocalDateTime codigoRecuperacionExpira;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.intentosFallidos == null) {
            this.intentosFallidos = 0;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}