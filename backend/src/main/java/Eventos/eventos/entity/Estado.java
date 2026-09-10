package Eventos.eventos.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.Set;
import Eventos.eventos.audit.AuditEntityListener;

@Entity
@EntityListeners(AuditEntityListener.class)
@Table(name = "estados",
        uniqueConstraints = @UniqueConstraint(
            name = "uk_estado_tipo_nombre",
            columnNames = {"tipo_estado", "nombre_estado"}
        ))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Estado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_estado")
    private Long idEstado;

    @Column(name = "nombre_estado", nullable = false, length = 100)
    private String nombreEstado;

    @Column(name = "descripcion_estado", length = 255)
    private String descripcionEstado;

    @Column(name = "tipo_estado", length = 50)
    private String tipoEstado;

    @JsonIgnore
    @OneToMany(mappedBy = "estado")
    private Set<Evento> eventos;

    @JsonIgnore
    @OneToMany(mappedBy = "estado")
    private Set<Notificacion> notificaciones;

    @JsonIgnore
    @OneToMany(mappedBy = "estado")
    private Set<Certificado> certificados;
}