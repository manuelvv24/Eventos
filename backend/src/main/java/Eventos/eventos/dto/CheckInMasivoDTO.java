package Eventos.eventos.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CheckInMasivoDTO {
    private Long idEvento;
    private String nombreEvento;
    private Integer totalInscritos;
    private Integer pendientes;
    private Integer yaAsistieron;
    private Integer marcados;
    private Integer omitidos;
    private String mensaje;
}