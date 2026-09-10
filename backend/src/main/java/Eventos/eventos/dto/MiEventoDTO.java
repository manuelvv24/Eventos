package Eventos.eventos.dto;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MiEventoDTO {
    private Long idRegistro;
    private Long idEvento;
    private String nombreEvento;
    private String tipoEvento;
    private String modalidadEvento;
    private LocalDateTime fechaInicioEvento;
    private String lugarEvento;
    private String estadoEvento;
    private String estadoAsistencia;
    private String codigoQr;
}