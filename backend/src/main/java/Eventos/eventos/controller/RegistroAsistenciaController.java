package Eventos.eventos.controller;

import Eventos.eventos.dto.RegistroAsistenciaDTO;
import Eventos.eventos.entity.Usuario;
import Eventos.eventos.repository.LoginRepository;
import Eventos.eventos.service.RegistroAsistenciaService;
import Eventos.eventos.util.RolNames;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/asistencias")
@CrossOrigin(origins = "*")
public class RegistroAsistenciaController {

    @Autowired
    private RegistroAsistenciaService asistenciaService;

    @Autowired
    private LoginRepository loginRepository;

    @GetMapping
    public ResponseEntity<List<RegistroAsistenciaDTO>> listarTodas() {
        return ResponseEntity.ok(asistenciaService.obtenerTodos());
    }

    @GetMapping("/evento/{idEvento}")
    public ResponseEntity<List<RegistroAsistenciaDTO>> listarPorEvento(@PathVariable Long idEvento) {
        return ResponseEntity.ok(asistenciaService.obtenerPorEvento(idEvento));
    }

    @PostMapping
    public ResponseEntity<?> registrar(@Valid @RequestBody RegistroAsistenciaDTO dto) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() != null) {
            Usuario usuario = loginRepository.findByEmailUsuario(auth.getPrincipal().toString())
                    .map(login -> login.getUsuario())
                    .orElse(null);
            if (usuario != null && RolNames.esPersonal(usuario.getRoles())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error",
                                "Solo los usuarios con rol Invitado pueden inscribirse en eventos."));
            }
        }
        return new ResponseEntity<>(asistenciaService.registrarAsistencia(dto), HttpStatus.CREATED);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        asistenciaService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}