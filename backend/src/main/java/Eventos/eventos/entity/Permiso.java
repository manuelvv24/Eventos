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
@Table(name = "permisos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Permiso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_permiso")
    private Long idPermiso;

    @Column(name = "nombre_permiso", nullable = false, length = 100)
    private String nombrePermiso;

    @Column(name = "descripcion_permiso", length = 255)
    private String descripcionPermiso;

    @Column(name = "Modulo_permiso", length = 100)
    private String moduloPermiso;

    @Column(name = "accion_permiso", length = 100)
    private String accionPermiso;

    @JsonIgnore
    @ManyToMany(mappedBy = "permisos")
    private Set<Rol> roles;
}