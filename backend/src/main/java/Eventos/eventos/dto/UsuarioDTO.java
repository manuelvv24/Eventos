package Eventos.eventos.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class UsuarioDTO {
    private Long idUsuario;
    private String primerNombreUsuario;
    private String segundoNombreUsuario;
    private String primerApellidoUsuario;
    private String segundoApellidoUsuario;
    private String emailUsuario;
    private String contrasenaUsuario;
    private String tipoDocumentoUsuario;
    private String numeroDocumentoUsuario;
    private String numeroTelefonoUsuario;
    private Boolean estadoUsuario;
    private Long idRol;
    private String rolNombre;
    private List<Long> idsRoles;
    private List<String> nombresRoles;
}