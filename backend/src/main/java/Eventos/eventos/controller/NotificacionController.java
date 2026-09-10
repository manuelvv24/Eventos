package Eventos.eventos.controller;

import Eventos.eventos.entity.Usuario;
import Eventos.eventos.repository.LoginRepository;
import Eventos.eventos.service.NotificacionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notificaciones")
public class NotificacionController {

    @Autowired private NotificacionService notificacionService;
    @Autowired private LoginRepository loginRepository;

    private Usuario usuarioActual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return loginRepository.findByEmailUsuario(auth.getName())
                .map(login -> login.getUsuario())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> listar() {
        return ResponseEntity.ok(notificacionService.obtenerDeUsuario(usuarioActual().getIdUsuario()));
    }

    @GetMapping("/no-leidas")
    public ResponseEntity<Map<String, Long>> noLeidas() {
        return ResponseEntity.ok(Map.of("noLeidas", notificacionService.contarNoLeidas(usuarioActual().getIdUsuario())));
    }

    @PutMapping("/{id}/leer")
    public ResponseEntity<Void> marcarLeida(@PathVariable Long id) {
        notificacionService.marcarLeida(id, usuarioActual().getIdUsuario());
        return ResponseEntity.ok().build();
    }

    @PutMapping("/leer-todas")
    public ResponseEntity<Void> marcarTodas() {
        notificacionService.marcarTodasLeidas(usuarioActual().getIdUsuario());
        return ResponseEntity.ok().build();
    }
}