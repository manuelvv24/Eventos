package Eventos.eventos.dto;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CertificadoDTO {
    private Long idCertificado;
    private Long idRegistroAsistencia;
    private String codigoVerificacion;
    private String estadoCertificado;
    private String urlPdf;
    private LocalDateTime fechaEmisionCertificado;
    private String nombreParticipante;
    private String documentoParticipante;
    private String nombreEvento;
    private String duracionEvento;
    private String tipoEvento;
    private String modalidadEvento;
    private LocalDateTime fechaInicioEvento;
    private LocalDateTime fechaFinEvento;
}