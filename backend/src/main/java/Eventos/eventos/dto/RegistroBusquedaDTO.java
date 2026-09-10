package Eventos.eventos.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class RegistroBusquedaDTO {
    private Long idRegistroAsistencia;
    private Long idEventos;
    private String nombreEvento;
    private LocalDateTime fechaInicioEvento;
    private String estadoRegistro;
    private boolean yaAsistio;
    private LocalDateTime fechaCheckIn;
    private String tipoDocumento;
    private String numeroDocumento;
    private String nombreCompleto;
}