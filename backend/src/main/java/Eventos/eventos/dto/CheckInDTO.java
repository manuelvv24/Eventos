package Eventos.eventos.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
public class CheckInDTO {
    private Long idCheckIn;

    @NotNull(message = "El ID de registro de asistencia es obligatorio")
    private Long idRegistroAsistencia;

    private String metodoCheckIn;
    private Long idUsuario;
    private String ipCheckIn;
    private LocalDateTime createdAt;
}


