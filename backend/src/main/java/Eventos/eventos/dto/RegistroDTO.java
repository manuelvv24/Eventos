package Eventos.eventos.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;

/**
 * DTO de registro con sanitización integrada: los setters limpian el dato
 * ANTES de que corran las validaciones Bean Validation.
 */
@Getter
public class RegistroDTO {

    @NotBlank(message = "El primer nombre es obligatorio")
    @Size(min = 2, max = 30, message = "El primer nombre debe tener entre 2 y 30 caracteres")
    @Pattern(regexp = "^[\\p{L}\\s]*$", message = "El primer nombre solo admite letras y espacios")
    private String primerNombre;

    @Size(min = 2, max = 30, message = "El segundo nombre debe tener entre 2 y 30 caracteres")
    @Pattern(regexp = "^[\\p{L}\\s]*$", message = "El segundo nombre solo admite letras y espacios")
    private String segundoNombre;

    @NotBlank(message = "El primer apellido es obligatorio")
    @Size(min = 2, max = 30, message = "El primer apellido debe tener entre 2 y 30 caracteres")
    @Pattern(regexp = "^[\\p{L}\\s]*$", message = "El primer apellido solo admite letras y espacios")
    private String primerApellido;

    @Size(min = 2, max = 30, message = "El segundo apellido debe tener entre 2 y 30 caracteres")
    @Pattern(regexp = "^[\\p{L}\\s]*$", message = "El segundo apellido solo admite letras y espacios")
    private String segundoApellido;

    @NotBlank(message = "El tipo de documento es obligatorio")
    @Pattern(regexp = "CC|CE|TI|PASAPORTE", message = "Tipo de documento no válido")
    private String tipoDocumento;

    @NotBlank(message = "El número de documento es obligatorio")
    @Size(min = 5, max = 18, message = "El número de documento debe tener entre 5 y 18 caracteres")
    @Pattern(regexp = "^[A-Z0-9]*$", message = "El número de documento solo admite letras y números")
    private String numeroDocumento;

    @Pattern(regexp = "^\\d{7,15}$", message = "El teléfono debe tener entre 7 y 15 dígitos")
    private String telefono;

    @NotBlank(message = "El correo electrónico es obligatorio")
    @Email(message = "Debe ser un correo electrónico válido")
    @Size(max = 100, message = "El correo no puede superar 100 caracteres")
    private String email;

    @Email(message = "Debe ser un correo de recuperación válido")
    @Size(max = 100, message = "El correo de recuperación no puede superar 100 caracteres")
    private String emailRecuperacion;

    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 8, max = 72, message = "La contraseña debe tener entre 8 y 72 caracteres")
    private String password;

    private Boolean terms = false;

    // ── Setters con sanitización (se ejecutan antes de la validación) ─────────

    public void setPrimerNombre(String v)    { this.primerNombre    = limpiarNombre(v); }
    public void setSegundoNombre(String v)   { this.segundoNombre   = limpiarNombre(v); }
    public void setPrimerApellido(String v)  { this.primerApellido  = limpiarNombre(v); }
    public void setSegundoApellido(String v) { this.segundoApellido = limpiarNombre(v); }

    public void setTipoDocumento(String v) {
        this.tipoDocumento = v != null ? v.trim().toUpperCase() : null;
    }

    public void setNumeroDocumento(String v) {
        this.numeroDocumento = v != null
                ? v.trim().toUpperCase().replaceAll("[^A-Z0-9]", "") : null;
    }

    public void setTelefono(String v) {
        String digitos = v != null ? v.replaceAll("[^0-9]", "") : null;
        this.telefono = digitos != null && !digitos.isBlank() ? digitos : null;
    }

    public void setEmail(String v) {
        this.email = v != null ? v.trim().toLowerCase() : null;
    }

    public void setEmailRecuperacion(String v) {
        String limpio = v != null ? v.trim().toLowerCase() : null;
        this.emailRecuperacion = limpio != null && !limpio.isBlank() ? limpio : null;
    }

    public void setPassword(String v) { this.password = v; }

    public void setTerms(Boolean v)   { this.terms = v; }

    private static String limpiarNombre(String v) {
        if (v == null) return null;
        String limpio = v.trim().replaceAll("\\s+", " ");
        return limpio.isBlank() ? null : limpio;
    }
}
