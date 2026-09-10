package Eventos.eventos.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * DTO para solicitar el envío del código de recuperación de cuenta.
 * El correo puede ser el de inicio de sesión o el correo alternativo de recuperación.
 */
@Getter
@Setter
public class RecuperarDTO {

    @NotBlank(message = "El correo es obligatorio")
    @Email(message = "Debe ser un correo electrónico válido")
    @Size(max = 100, message = "El correo no puede superar 100 caracteres")
    private String email;

    public void setEmail(String v) {
        this.email = v != null ? v.trim().toLowerCase() : null;
    }
}