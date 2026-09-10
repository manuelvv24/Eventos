package Eventos.eventos.controller;

import Eventos.eventos.entity.Participante;
import Eventos.eventos.entity.RegistroAsistencia;
import Eventos.eventos.entity.Usuario;
import Eventos.eventos.repository.LoginRepository;
import Eventos.eventos.repository.ParticipanteRepository;
import Eventos.eventos.repository.RegistroAsistenciaRepository;
import Eventos.eventos.service.EstadoService;
import Eventos.eventos.util.RolNames;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
@RestController
@RequestMapping("/api/participantes")
public class ParticipanteController {

    @Autowired
    private ParticipanteRepository participanteRepository;

    @Autowired
    private LoginRepository loginRepository;

    @Autowired
    private RegistroAsistenciaRepository registroAsistenciaRepository;

    @Autowired
    private EstadoService estadoService;

    @PostMapping
    public ResponseEntity<?> registrar(@RequestBody Participante participante) {
        Participante nuevo = participanteRepository.save(participante);
        return new ResponseEntity<>(toDTO(nuevo), HttpStatus.CREATED);
    }
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> listarTodos() {
        List<Map<String, Object>> result = participanteRepository.findActivos()
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> obtenerPorId(@PathVariable Long id) {
        return participanteRepository.findById(id)
                .filter(p -> !p.estaEliminado())
                .map(p -> ResponseEntity.ok(toDTO(p)))
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable Long id) {
        Participante participante = participanteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Participante no encontrado con ID: " + id));
        if (participante.estaEliminado()) {
            throw new RuntimeException("El participante ya está eliminado.");
        }
        participante.setEliminado(Boolean.TRUE);
        participanteRepository.save(participante);

        registroAsistenciaRepository.findByParticipante_IdParticipante(id).forEach(registro -> {
            registro.setEstado(estadoService.resolver(EstadoService.TIPO_REGISTRO, "ELIMINADO"));
            registroAsistenciaRepository.save(registro);
        });

        return ResponseEntity.noContent().build();
    }
    @GetMapping("/me")
    public ResponseEntity<?> miParticipante() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || auth.getPrincipal() == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "No autenticado"));
            }

            // Buscar el usuario por email (principal del JWT) — más robusto que usar el credential ID
            String email = auth.getPrincipal().toString();
            Usuario usuario = loginRepository.findByEmailUsuario(email)
                    .map(login -> login.getUsuario())
                    .orElse(null);

            if (usuario == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Usuario no encontrado. Por favor cierre sesión e inicie sesión nuevamente."));
            }

            if (RolNames.esPersonal(usuario.getRoles())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error",
                                "Solo los usuarios con rol Invitado pueden inscribirse en eventos."));
            }

            Long idUsuario = usuario.getIdUsuario();

            Participante participante = participanteRepository.findByUsuario_IdUsuario(idUsuario)
                    .filter(p -> !p.estaEliminado())
                    .orElseGet(() -> {
                        Participante nuevo = new Participante();
                        nuevo.setUsuario(usuario);
                        return participanteRepository.save(nuevo);
                    });

            return ResponseEntity.ok(Map.of(
                "idParticipantes", participante.getIdParticipante(),
                "idUsuarios",      participante.getUsuario() != null
                        ? participante.getUsuario().getIdUsuario() : null
            ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
    private Map<String, Object> toDTO(Participante p) {
        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("idParticipantes", p.getIdParticipante());
        dto.put("idUsuarios",      p.getUsuario() != null ? p.getUsuario().getIdUsuario() : null);
        dto.put("eliminado",       p.estaEliminado() ? Boolean.TRUE : Boolean.FALSE);
        if (p.getUsuario() != null) {
            Usuario u = p.getUsuario();
            {
                dto.put("primerNombre",   u.getPrimerNombreUsuario());
                dto.put("segundoNombre",  u.getSegundoNombreUsuario());
                dto.put("primerApellido", u.getPrimerApellidoUsuario());
                dto.put("segundoApellido", u.getSegundoApellidoUsuario());
                dto.put("email",          u.getLogin() != null ? u.getLogin().getEmailUsuario() : null);
                dto.put("tipoDocumento",  u.getTipoDocumentoUsuario());
                dto.put("documento",      u.getNumeroDocumentoUsuario());
                dto.put("telefono",       u.getNumeroTelefonoUsuario());
            }
        }
        return dto;
    }
}
