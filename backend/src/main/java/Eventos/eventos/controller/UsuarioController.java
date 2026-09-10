package Eventos.eventos.controller;

import Eventos.eventos.dto.UsuarioDTO;
import Eventos.eventos.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    @Autowired
    private UsuarioService usuarioService;
    @GetMapping
    public ResponseEntity<List<UsuarioDTO>> listar() {
        return ResponseEntity.ok(usuarioService.listarTodos());
    }
    @GetMapping("/{id}")
    public ResponseEntity<?> obtenerPorId(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(usuarioService.obtenerPorId(id));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }
    @PostMapping
    public ResponseEntity<?> registrar(@Valid @RequestBody UsuarioDTO dto) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(usuarioService.registrarUsuario(dto));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    @PutMapping("/{id}")
    public ResponseEntity<?> actualizar(@PathVariable Long id,
                                        @RequestBody UsuarioDTO dto) {
        try {
            return ResponseEntity.ok(usuarioService.actualizarUsuario(id, dto));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    @PatchMapping("/{id}/estado")
    public ResponseEntity<?> cambiarEstado(@PathVariable Long id,
                                           @RequestBody Map<String, Boolean> body) {
        try {
            Boolean nuevoEstado = body.get("estado");
            if (nuevoEstado == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "El campo 'estado' es requerido"));
            }
            UsuarioDTO dto = usuarioService.obtenerPorId(id);
            dto.setEstadoUsuario(nuevoEstado);
            UsuarioDTO actualizado = usuarioService.actualizarUsuario(id, dto);
            return ResponseEntity.ok(Map.of(
                    "mensaje", "Estado actualizado correctamente",
                    "estado", Boolean.TRUE.equals(actualizado.getEstadoUsuario())
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    @PatchMapping("/{id}/rol")
    public ResponseEntity<?> cambiarRol(@PathVariable Long id,
                                        @RequestBody Map<String, Long> body) {
        try {
            Long idRol = body.get("idRol");
            if (idRol == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "El campo 'idRol' es requerido"));
            }
            UsuarioDTO dto = usuarioService.obtenerPorId(id);
            dto.setIdRol(idRol);
            UsuarioDTO actualizado = usuarioService.actualizarUsuario(id, dto);
            return ResponseEntity.ok(Map.of(
                    "mensaje", "Rol actualizado correctamente",
                    "nuevoRol", actualizado.getRolNombre()
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable Long id) {
        try {
            usuarioService.eliminarUsuario(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}