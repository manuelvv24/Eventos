package Eventos.eventos.dto;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ImportacionDTO {
    private Long idImportacion;
    private Long idUsuarios;
    private String nombreArchivo;
    private Long idEvento;
    private Integer totalFilas;
    private Integer filasExitosas;
    private Integer filasError;
    private String estadoImportacion;
    private LocalDateTime fechaImportacion;
    private List<ErrorFilaDTO> errores;
    private List<ParticipanteImportDTO> preview;

    @Getter
    @Setter
    public static class ErrorFilaDTO {
        private int numeroFila;
        private String contenidoFila;
        private String motivoError;

        public ErrorFilaDTO(int numeroFila, String contenidoFila, String motivoError) {
            this.numeroFila = numeroFila;
            this.contenidoFila = contenidoFila;
            this.motivoError = motivoError;
        }
    }

    @Getter
    @Setter
    public static class ParticipanteImportDTO {
        private int numeroFila;
        private String primerNombre;
        private String segundoNombre;
        private String primerApellido;
        private String segundoApellido;
        private String email;
        private String tipoDocumento;
        private String numeroDocumento;
        private String telefono;
    }
}