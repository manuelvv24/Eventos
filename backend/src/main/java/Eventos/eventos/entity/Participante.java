package Eventos.eventos.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.List;
import Eventos.eventos.audit.AuditEntityListener;

@Entity
@EntityListeners(AuditEntityListener.class)
@Table(name = "participantes")
@Getter
@Setter
public class Participante {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_participante")
    private Long idParticipante;

    @Column(name = "fecha_registro_participante")
    private LocalDateTime fechaRegistroParticipante;

    @Column(name = "eliminado")
    private Boolean eliminado = false;

    @Column(name = "created_at_participante", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at_participante")
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_usuarios", nullable = false)
    private Usuario usuario;

    @JsonIgnore
    @OneToMany(mappedBy = "participante", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RegistroAsistencia> registrosAsistencia;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.fechaRegistroParticipante = LocalDateTime.now();
        if (this.eliminado == null) {
            this.eliminado = false;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public boolean estaEliminado() {
        return Boolean.TRUE.equals(this.eliminado);
    }
}