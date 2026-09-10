package Eventos.eventos.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * DTO para verificar en vivo la disponibilidad del correo y/o número de documento
 * antes de enviar el registro completo.
 */
@Getter
@Setter
public class VerificarDisponibilidadDTO {

    @Email(message = "Debe ser un correo electrónico válido")
    @Size(max = 100, message = "El correo no puede superar 100 caracteres")
    private String email;

    @Size(min = 5, max = 18, message = "El número de documento debe tener entre 5 y 18 caracteres")
    @Pattern(regexp = "^[A-Z0-9]*$", message = "El número de documento solo admite letras y números")
    private String numeroDocumento;

    public void setEmail(String v) {
        this.email = v != null ? v.trim().toLowerCase() : null;
    }

    public void setNumeroDocumento(String v) {
        this.numeroDocumento = v != null
                ? v.trim().toUpperCase().replaceAll("[^A-Z0-9]", "") : null;
    }
}