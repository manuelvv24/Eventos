package Eventos.eventos.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * DTO para validar el código de 6 dígitos recibido en el correo de recuperación.
 * Al validar, devuelve un token temporal para restablecer la contraseña.
 */
@Getter
@Setter
public class VerificarCodigoDTO {

    @NotBlank(message = "El correo es obligatorio")
    @Email(message = "Debe ser un correo electrónico válido")
    @Size(max = 100, message = "El correo no puede superar 100 caracteres")
    private String email;

    @NotBlank(message = "El código es obligatorio")
    @Pattern(regexp = "^\\d{6}$", message = "El código debe tener 6 dígitos")
    private String codigo;

    public void setEmail(String v) {
        this.email = v != null ? v.trim().toLowerCase() : null;
    }

    public void setCodigo(String v) {
        this.codigo = v != null ? v.replaceAll("\\D", "") : null;
    }
}