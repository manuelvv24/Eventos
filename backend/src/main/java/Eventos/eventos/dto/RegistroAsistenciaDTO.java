package Eventos.eventos.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
public class RegistroAsistenciaDTO {
    private Long idRegistroAsistencia;

    @NotNull(message = "El ID del evento es obligatorio")
    private Long idEventos;

    @NotNull(message = "El ID del participante es obligatorio")
    private Long idParticipantes;

    private LocalDateTime fechaAsistencia;
    private String estadoAsistencia;
    private String codigoQrInscripcion;

    // Información de solo lectura para la UI (no se valida al registrar)
    private Integer diasAsistidas;
    private Integer diasRequeridos;
    private Boolean tieneCertificado;
}