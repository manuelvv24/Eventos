package Eventos.eventos.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.List;
import jakarta.persistence.EntityListeners;
import Eventos.eventos.audit.AuditEntityListener;

@Entity
@EntityListeners(AuditEntityListener.class)
@Table(name = "usuarios")
@Getter
@Setter
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_usuario")
    private Long idUsuario;

    @NotBlank(message = "El primer nombre es obligatorio")
    @Size(max = 50)
    @Column(name = "primer_nombre_usuario", nullable = false, length = 30)
    private String primerNombreUsuario;

    @Size(max = 50)
    @Column(name = "segundo_nombre_usuario", length = 30)
    private String segundoNombreUsuario;

    @NotBlank(message = "El primer apellido es obligatorio")
    @Size(max = 50)
    @Column(name = "primer_apellido_usuario", nullable = false, length = 30)
    private String primerApellidoUsuario;

    @Size(max = 50)
    @Column(name = "segundo_apellido_usuario", length = 30)
    private String segundoApellidoUsuario;

    @NotBlank(message = "El tipo de documento es obligatorio")
    @Column(name = "tipo_documento_usuario", nullable = false, length = 50)
    private String tipoDocumentoUsuario;

    @NotBlank(message = "El número de documento es obligatorio")
    @Column(name = "numero_documento_usuario", nullable = false, unique = true, length = 18)
    private String numeroDocumentoUsuario;

    @Column(name = "numero_telefono_usuario", length = 15)
    private String numeroTelefonoUsuario;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_estado", nullable = false)
    private Estado estado;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @JsonIgnore
    @OneToOne(mappedBy = "usuario", fetch = FetchType.LAZY)
    private Login login;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "roles_usuarios",
        joinColumns = @JoinColumn(name = "id_usuario"),
        inverseJoinColumns = @JoinColumn(name = "id_rol")
    )
    private Set<Rol> roles;

    @JsonIgnore
    @OneToMany(mappedBy = "usuario", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Importacion> importaciones;

    @JsonIgnore
    @OneToMany(mappedBy = "usuario")
    private List<LogAuditoria> logsAuditoria;

    @JsonIgnore
    @OneToMany(mappedBy = "usuario")
    private List<Notificacion> notificaciones;

    @JsonIgnore
    @OneToMany(mappedBy = "usuario", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Participante> participantes;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public boolean estaEliminado() {
        return estado != null && "ELIMINADO".equalsIgnoreCase(estado.getNombreEstado());
    }
}