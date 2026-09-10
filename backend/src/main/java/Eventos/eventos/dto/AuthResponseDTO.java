package Eventos.eventos.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class AuthResponseDTO {
    private String mensaje;
    private Long idUsuario;
    private String emailUsuario;
    private String nombreCompleto;
    private String rol;
    private String token;
    private List<String> roles;
}