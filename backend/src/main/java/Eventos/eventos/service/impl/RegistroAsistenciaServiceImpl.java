package Eventos.eventos.service.impl;

import Eventos.eventos.dto.RegistroAsistenciaDTO;
import Eventos.eventos.entity.Evento;
import Eventos.eventos.entity.Participante;
import Eventos.eventos.entity.RegistroAsistencia;
import Eventos.eventos.repository.CertificadoRepository;
import Eventos.eventos.repository.CheckInRepository;
import Eventos.eventos.repository.EventoRepository;
import Eventos.eventos.repository.ParticipanteRepository;
import Eventos.eventos.repository.RegistroAsistenciaRepository;
import Eventos.eventos.service.EmailService;
import Eventos.eventos.service.EstadoService;
import Eventos.eventos.service.NotificacionService;
import Eventos.eventos.service.RegistroAsistenciaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class RegistroAsistenciaServiceImpl implements RegistroAsistenciaService {

    @Autowired
    private RegistroAsistenciaRepository asistenciaRepository;

    @Autowired
    private CertificadoRepository certificadoRepository;

    @Autowired
    private CheckInRepository checkInRepository;

    @Autowired
    private EventoRepository eventoRepository;

    @Autowired
    private ParticipanteRepository participanteRepository;

    @Autowired
    private NotificacionService notificacionService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private EstadoService estadoService;

    @Override
    @Transactional(readOnly = true)
    public List<RegistroAsistenciaDTO> obtenerTodos() {
        return asistenciaRepository.findAll().stream()
                .filter(r -> !r.estaEliminado())
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<RegistroAsistenciaDTO> listarEliminados() {
        return asistenciaRepository.findAll().stream()
                .filter(RegistroAsistencia::estaEliminado)
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<RegistroAsistenciaDTO> obtenerPorEvento(Long idEvento) {
        return asistenciaRepository.findByEvento_IdEventos(idEvento).stream()
                .filter(r -> !r.estaEliminado())
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public RegistroAsistenciaDTO registrarAsistencia(RegistroAsistenciaDTO dto) {
        Evento evento = eventoRepository.findById(dto.getIdEventos())
                .orElseThrow(() -> new RuntimeException("Evento no encontrado con ID: " + dto.getIdEventos()));
        if (Boolean.TRUE.equals(evento.getEliminado())) {
            throw new RuntimeException("El evento no está disponible para registro.");
        }

        String estado = evento.getEstado() != null ? evento.getEstado().getNombreEstado() : null;
        boolean registrable = estado != null &&
            !estado.equalsIgnoreCase("FINALIZADO") &&
            !estado.equalsIgnoreCase("CANCELADO") &&
            !estado.equalsIgnoreCase("BORRADOR");
        if (!registrable) {
            throw new RuntimeException("El evento no está disponible para registro. Estado: " + estado);
        }

        // HU05: validate registration deadline
        if (evento.getFechaLimiteInscripcion() != null
                && LocalDateTime.now().isAfter(evento.getFechaLimiteInscripcion())) {
            throw new RuntimeException("El plazo de inscripción para este evento cerró el "
                    + evento.getFechaLimiteInscripcion().format(
                            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
        }

        List<RegistroAsistencia> registrosEvento = asistenciaRepository.findByEvento_IdEventos(evento.getIdEventos());
        long activos = registrosEvento.stream().filter(r -> !r.estaEliminado()).count();

        if (evento.getAforoMaximoEvento() != null && activos >= evento.getAforoMaximoEvento()) {
            throw new RuntimeException("No hay cupos disponibles. El evento ha alcanzado su aforo máximo.");
        }

        Participante participanteRef = participanteRepository.findById(dto.getIdParticipantes())
                .orElseThrow(() -> new RuntimeException(
                        "Participante no encontrado con ID: " + dto.getIdParticipantes()));

        boolean yaRegistradoActivo = registrosEvento.stream()
                .anyMatch(r -> !r.estaEliminado()
                        && r.getParticipante() != null
                        && r.getParticipante().getIdParticipante().equals(dto.getIdParticipantes()));
        if (yaRegistradoActivo) {
            throw new RuntimeException("El participante ya está registrado en este evento.");
        }

        RegistroAsistencia asistencia = asistenciaRepository
                .findByEvento_IdEventosAndParticipante_IdParticipante(
                        evento.getIdEventos(), dto.getIdParticipantes())
                .filter(r -> r.estaEliminado())
                .orElseGet(() -> {
                    RegistroAsistencia nuevo = new RegistroAsistencia();
                    nuevo.setEvento(evento);
                    nuevo.setParticipante(participanteRef);
                    nuevo.setCodigoQrInscripcion("QR-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
                    return nuevo;
                });

        asistencia.setEstado(estadoService.resolver(EstadoService.TIPO_REGISTRO, "REGISTRADO"));
        RegistroAsistencia guardada = asistenciaRepository.save(asistencia);

        participanteRepository.findById(dto.getIdParticipantes()).ifPresent(p -> {
            if (p.getUsuario() != null) {
                String emailParticipante = p.getUsuario().getLogin() != null
                        ? p.getUsuario().getLogin().getEmailUsuario() : null;
                
                if (emailParticipante != null && !emailParticipante.isEmpty()) {
                    emailService.enviarSeguro(
                            emailParticipante,
                            "Inscripción confirmada: " + evento.getNombreEvento(),
                            plantillaInscripcion(p, evento, guardada.getCodigoQrInscripcion())
                    );
                }

                notificacionService.enviar(
                        p.getUsuario(),
                        evento,
                        "INSCRIPCION",
                        "Inscripción confirmada",
                        "Te inscribiste al evento \"" + evento.getNombreEvento() + "\"."
                );
                notificacionService.programarRecordatorio(p.getUsuario(), evento);
            }
        });

        return mapToDTO(guardada);
    }

    @Override
    @Transactional
    public RegistroAsistenciaDTO restaurar(Long id) {
        RegistroAsistencia asistencia = asistenciaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Registro de asistencia no encontrado con ID: " + id));
        if (!asistencia.estaEliminado()) {
            throw new RuntimeException("El registro de asistencia no está eliminado.");
        }
        asistencia.setEstado(estadoService.resolver(EstadoService.TIPO_REGISTRO, "REGISTRADO"));
        return mapToDTO(asistenciaRepository.save(asistencia));
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        RegistroAsistencia asistencia = asistenciaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Registro de asistencia no encontrado con ID: " + id));
        asistencia.setEstado(estadoService.resolver(EstadoService.TIPO_REGISTRO, "ELIMINADO"));
        asistenciaRepository.save(asistencia);
    }

    private String plantillaInscripcion(Participante p, Evento e, String codigoQr) {
        String nombreCompleto = "Participante";
        if (p.getUsuario() != null) {
            String nombre = p.getUsuario().getPrimerNombreUsuario() != null ? p.getUsuario().getPrimerNombreUsuario() : "";
            String apellido = p.getUsuario().getPrimerApellidoUsuario() != null ? p.getUsuario().getPrimerApellidoUsuario() : "";
            nombreCompleto = (nombre + " " + apellido).trim();
            if (nombreCompleto.isEmpty()) {
                String email = p.getUsuario().getLogin() != null ? p.getUsuario().getLogin().getEmailUsuario() : null;
                nombreCompleto = email != null ? email : "Participante";
            }
        }

        return "<div style=\"font-family:Arial;max-width:600px;margin:auto;border:1px solid #ddd\">" +
                "<div style=\"background:#16a34a;color:#fff;padding:16px\"><h2>¡Inscripción confirmada!</h2></div>" +
                "<div style=\"padding:20px\">" +
                "<p>Hola <b>" + nombreCompleto + "</b>,</p>" +
                "<p>Tu inscripción fue registrada correctamente.</p>" +
                "<p><b>Evento:</b> " + e.getNombreEvento() + "<br>" +
                "<b>Fecha:</b> " + e.getFechaInicioEvento() + "<br>" +
                "<b>Lugar:</b> " + e.getLugarEvento() + "</p>" +
                "<p>Tu código de check-in es: <b>" + codigoQr + "</b><br>" +
                "Preséntalo o su QR el día del evento.</p>" +
                "</div></div>";
    }

    private RegistroAsistenciaDTO mapToDTO(RegistroAsistencia asistencia) {
        RegistroAsistenciaDTO dto = new RegistroAsistenciaDTO();
        dto.setIdRegistroAsistencia(asistencia.getIdRegistroAsistencia());
        dto.setIdEventos(asistencia.getEvento().getIdEventos());
        dto.setIdParticipantes(asistencia.getParticipante() != null
                ? asistencia.getParticipante().getIdParticipante() : null);
        dto.setFechaAsistencia(asistencia.getFechaAsistencia());
        dto.setEstadoAsistencia(asistencia.getEstado() != null
                ? asistencia.getEstado().getNombreEstado() : null);
        dto.setCodigoQrInscripcion(asistencia.getCodigoQrInscripcion());

        Integer requeridos = asistencia.getEvento().getDiasMinimosCertificacion();
        dto.setDiasRequeridos(requeridos != null ? requeridos : 0);
        long dias = checkInRepository
                .findByRegistroAsistencia_IdRegistroAsistencia(asistencia.getIdRegistroAsistencia())
                .stream()
                .map(c -> c.getCreatedAt() != null ? c.getCreatedAt().toLocalDate() : null)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .count();
        dto.setDiasAsistidas((int) dias);
        dto.setTieneCertificado(certificadoRepository
                .findByRegistroAsistencia_IdRegistroAsistencia(asistencia.getIdRegistroAsistencia())
                .isPresent());
        return dto;
    }
}