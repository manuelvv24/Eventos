package Eventos.eventos.dto;

import java.time.LocalDateTime;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EventoDTO {
    private Long idEventos;

    @NotBlank(message = "El nombre del evento es obligatorio")
    private String nombreEvento;

    private String descripcionEvento;
    private String tipoEvento;
    private String modalidadEvento;
    private LocalDateTime fechaInicioEvento;
    private LocalDateTime fechaFinEvento;
    
    private String lugarEvento;
    
    private Integer aforoMaximoEvento;
    private Integer duracionEvento;
    /** Cantidad de personas inscritas actualmente en el evento. */
    private Integer inscritos;
    /** Días de asistencia mínimos para certificar. Null/0 = sin mínimo (basta 1 check-in). */
    private Integer diasMinimosCertificacion;
    private String estadoEvento;
    private String imagenUrl;
    private String enlaceUrl;
    private LocalDateTime fechaLimiteInscripcion;
    private Boolean eliminado;
}