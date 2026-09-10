package Eventos.eventos.controller;

import Eventos.eventos.dto.EventoDTO;
import Eventos.eventos.dto.RegistroAsistenciaDTO;
import Eventos.eventos.dto.UsuarioDTO;
import Eventos.eventos.entity.ConfiguracionCertificado;
import Eventos.eventos.entity.Participante;
import Eventos.eventos.entity.Rol;
import Eventos.eventos.entity.RegistroAsistencia;
import Eventos.eventos.repository.ConfiguracionCertificadoRepository;
import Eventos.eventos.repository.ParticipanteRepository;
import Eventos.eventos.repository.RegistroAsistenciaRepository;
import Eventos.eventos.repository.RolRepository;
import Eventos.eventos.service.EstadoService;
import Eventos.eventos.service.EventoService;
import Eventos.eventos.service.RegistroAsistenciaService;
import Eventos.eventos.service.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Restablecimiento de registros eliminados (soft-delete).
 * Acceso exclusivo del rol Super Administrador (ver SecurityConfig).
 */
@RestController
@RequestMapping("/api/restaurar")
public class RestaurarController {

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private RegistroAsistenciaService asistenciaService;

    @Autowired
    private RolRepository rolRepository;

    @Autowired
    private ConfiguracionCertificadoRepository configuracionRepository;

    @Autowired
    private EventoService eventoService;

    @Autowired
    private ParticipanteRepository participanteRepository;

    @Autowired
    private RegistroAsistenciaRepository registroAsistenciaRepository;

    @Autowired
    private EstadoService estadoService;

    // ── Participantes ─────────────────────────────────────────────────────────
    @GetMapping("/participantes")
    public List<Map<String, Object>> listarParticipantes() {
        return participanteRepository.findEliminados().stream()
                .map(this::participanteDTO)
                .collect(Collectors.toList());
    }

    @PostMapping("/participantes/{id}")
    public ResponseEntity<?> restaurarParticipante(@PathVariable Long id) {
        Participante participante = participanteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Participante no encontrado con ID: " + id));
        if (!participante.estaEliminado()) {
            throw new RuntimeException("El participante no está eliminado.");
        }
        participante.setEliminado(Boolean.FALSE);
        participanteRepository.save(participante);

        registroAsistenciaRepository.findByParticipante_IdParticipante(id).forEach(registro -> {
            if (registro.estaEliminado()) {
                registro.setEstado(estadoService.resolver(EstadoService.TIPO_REGISTRO, "REGISTRADO"));
                registroAsistenciaRepository.save(registro);
            }
        });

        return ResponseEntity.ok(participanteDTO(participante));
    }

    private Map<String, Object> participanteDTO(Participante p) {
        java.util.LinkedHashMap<String, Object> dto = new java.util.LinkedHashMap<>();
        dto.put("idParticipantes", p.getIdParticipante());
        if (p.getUsuario() != null) {
            Eventos.eventos.entity.Usuario u = p.getUsuario();
            dto.put("idUsuarios",      u.getIdUsuario());
            dto.put("primerNombre",    u.getPrimerNombreUsuario());
            dto.put("primerApellido",  u.getPrimerApellidoUsuario());
            dto.put("email",           u.getLogin() != null ? u.getLogin().getEmailUsuario() : null);
            dto.put("tipoDocumento",   u.getTipoDocumentoUsuario());
            dto.put("documento",       u.getNumeroDocumentoUsuario());
            dto.put("telefono",        u.getNumeroTelefonoUsuario());
        }
        return dto;
    }

    // ── Usuarios ──────────────────────────────────────────────────────────────
    @GetMapping("/usuarios")
    public List<UsuarioDTO> listarUsuarios() {
        return usuarioService.listarEliminados();
    }

    @PostMapping("/usuarios/{id}")
    public ResponseEntity<?> restaurarUsuario(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(usuarioService.restaurarUsuario(id));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ── Registros de asistencia ───────────────────────────────────────────────
    @GetMapping("/asistencias")
    public List<RegistroAsistenciaDTO> listarAsistencias() {
        return asistenciaService.listarEliminados();
    }

    @PostMapping("/asistencias/{id}")
    public ResponseEntity<?> restaurarAsistencia(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(asistenciaService.restaurar(id));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ── Roles ─────────────────────────────────────────────────────────────────
    @GetMapping("/roles")
    public ResponseEntity<List<Rol>> listarRoles() {
        return ResponseEntity.ok(rolRepository.findByActivoFalse());
    }

    @PostMapping("/roles/{id}")
    public ResponseEntity<?> restaurarRol(@PathVariable Long id) {
        return rolRepository.findById(id)
                .map(rol -> {
                    if (Boolean.TRUE.equals(rol.getActivo())) {
                        return ResponseEntity.badRequest().body(Map.of("error", "El rol no está inactivado"));
                    }
                    rol.setActivo(true);
                    rolRepository.save(rol);
                    return ResponseEntity.ok(rol);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // ── Configuraciones de certificado ────────────────────────────────────────
    @GetMapping("/configuraciones-certificado")
    public ResponseEntity<List<ConfiguracionCertificado>> listarConfiguraciones() {
        return ResponseEntity.ok(configuracionRepository.findAllByActivaFalse());
    }

    @PostMapping("/configuraciones-certificado/{id}")
    public ResponseEntity<?> restaurarConfiguracion(@PathVariable Long id) {
        return configuracionRepository.findById(id)
                .map(config -> {
                    if (Boolean.TRUE.equals(config.getActiva())) {
                        return ResponseEntity.badRequest().body(Map.of("error", "La configuración no está inactivada"));
                    }
                    config.setActiva(true);
                    configuracionRepository.save(config);
                    return ResponseEntity.ok(config);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // ── Eventos (papelera) ────────────────────────────────────────────────────
    @GetMapping("/eventos")
    public List<EventoDTO> listarEventos() {
        return eventoService.obtenerEliminados();
    }

    @PostMapping("/eventos/{id}")
    public ResponseEntity<?> restaurarEvento(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(eventoService.restaurar(id));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}