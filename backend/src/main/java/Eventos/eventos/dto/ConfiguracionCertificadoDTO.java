package Eventos.eventos.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ConfiguracionCertificadoDTO {

    private Long idConfiguracion;
    private String nombrePlantilla;
    private Boolean esPredeterminada;
    private String nombreOrganizacion;
    private String tituloCertificado;
    private String textoCuerpo;
    private String colorBorde;
    private String colorAcento;
    private String logoBase64;

    // Firma 1
    private String firma1Nombre;
    private String firma1Cargo;
    private String firma1ImagenBase64;

    // Firma 2
    private String firma2Nombre;
    private String firma2Cargo;
    private String firma2ImagenBase64;

    // Firma 3
    private String firma3Nombre;
    private String firma3Cargo;
    private String firma3ImagenBase64;
}
