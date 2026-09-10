package Eventos.eventos.controller;

import Eventos.eventos.dto.AuthResponseDTO;
import Eventos.eventos.dto.LoginDTO;
import Eventos.eventos.dto.RecuperarDTO;
import Eventos.eventos.dto.RegistroDTO;
import Eventos.eventos.dto.RestablecerDTO;
import Eventos.eventos.dto.VerificarCodigoDTO;
import Eventos.eventos.dto.VerificarDisponibilidadDTO;
import Eventos.eventos.exeption.DuplicadoException;
import Eventos.eventos.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;
    @PostMapping("/registro")
    public ResponseEntity<?> registrarUsuario(@Valid @RequestBody RegistroDTO dto) {
        try {
            AuthResponseDTO respuesta = authService.registrarUsuario(dto);
            return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
        } catch (DuplicadoException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", e.getMessage(), "campo", e.getCampo()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error interno del servidor"));
        }
    }

    @PostMapping("/verificar-disponibilidad")
    public ResponseEntity<?> verificarDisponibilidad(@Valid @RequestBody VerificarDisponibilidadDTO dto) {
        try {
            return ResponseEntity.ok(authService.verificarDisponibilidad(
                    dto.getEmail(), dto.getNumeroDocumento()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error interno del servidor"));
        }
    }
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginDTO dto,
                                   HttpServletRequest request) {
        try {
            AuthResponseDTO respuesta = authService.login(dto);
            // Log para diagnóstico
            System.out.println("[LOGIN OK] usuario=" + respuesta.getEmailUsuario()
                + " rol=" + respuesta.getRol()
                + " token=" + (respuesta.getToken() != null ? respuesta.getToken().substring(0, 20) + "..." : "NULL"));
            return ResponseEntity.ok(respuesta);
        } catch (RuntimeException e) {
            System.out.println("[LOGIN FAIL] " + e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            System.out.println("[LOGIN ERROR] " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error interno del servidor"));
        }
    }

    @PostMapping("/recuperar")
    public ResponseEntity<?> recuperar(@Valid @RequestBody RecuperarDTO dto) {
        try {
            authService.solicitarRecuperacion(dto.getEmail());
            return ResponseEntity.ok(Map.of("mensaje", "Si el correo está registrado, recibirás un código de recuperación"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error interno del servidor"));
        }
    }

    @PostMapping("/verificar-codigo")
    public ResponseEntity<?> verificarCodigo(@Valid @RequestBody VerificarCodigoDTO dto) {
        try {
            String token = authService.verificarCodigo(dto.getEmail(), dto.getCodigo());
            return ResponseEntity.ok(Map.of("token", token));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error interno del servidor"));
        }
    }

    @PostMapping("/restablecer")
    public ResponseEntity<?> restablecer(@Valid @RequestBody RestablecerDTO dto) {
        try {
            authService.restablecerContrasena(dto.getToken(), dto.getNuevaPassword());
            return ResponseEntity.ok(Map.of("mensaje", "Contraseña restablecida correctamente"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error interno del servidor"));
        }
    }

    @PostMapping("/cambiar-rol")
    public ResponseEntity<?> cambiarRol(@RequestBody Map<String, String> body,
                                        HttpServletRequest request) {
        try {
            String header = request.getHeader("Authorization");
            String token = header != null && header.startsWith("Bearer ")
                    ? header.substring(7) : null;
            String rol = body.get("rol");
            if (rol == null || rol.isBlank()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "El campo 'rol' es requerido"));
            }
            AuthResponseDTO respuesta = authService.cambiarRol(token, rol);
            return ResponseEntity.ok(respuesta);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error interno del servidor"));
        }
    }
}
