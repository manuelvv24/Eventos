package Eventos.eventos.service.impl;

import Eventos.eventos.entity.Estado;
import Eventos.eventos.entity.Evento;
import Eventos.eventos.entity.Notificacion;
import Eventos.eventos.entity.RegistroAsistencia;
import Eventos.eventos.entity.Usuario;
import Eventos.eventos.repository.NotificacionRepository;
import Eventos.eventos.repository.RegistroAsistenciaRepository;
import Eventos.eventos.service.EmailService;
import Eventos.eventos.service.EstadoService;
import Eventos.eventos.service.NotificacionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class NotificacionServiceImpl implements NotificacionService {

    @Autowired private NotificacionRepository notificacionRepository;
    @Autowired private RegistroAsistenciaRepository registroAsistenciaRepository;
    @Autowired private EmailService emailService;
    @Autowired private EstadoService estadoService;

    private void setEstado(Notificacion n, String nombreEstado) {
        Estado estado = estadoService.resolver(EstadoService.TIPO_NOTIFICACION, nombreEstado);
        if (estado != null) {
            n.setEstado(estado);
        }
    }

    @Override
    public Notificacion enviar(Usuario usuario, Evento evento, String tipo, String asunto, String descripcion) {
        Notificacion n = new Notificacion();
        n.setUsuario(usuario);
        n.setEvento(evento);
        n.setTipoNotificacion(tipo);
        n.setAsuntoNotificacion(asunto);
        n.setDescripcionNotificacion(descripcion);
        n.setMedioEnvio("APP");
        setEstado(n, "ENVIADA");
        n.setFechaEnvio(LocalDateTime.now());
        Notificacion guardada = notificacionRepository.save(n);

        if (usuario.getLogin() != null && usuario.getLogin().getEmailUsuario() != null
                && !usuario.getLogin().getEmailUsuario().isEmpty()) {
            emailService.enviarSeguro(
                    usuario.getLogin().getEmailUsuario(),
                    asunto,
                    plantillaCorreo(asunto, descripcion, evento)
            );
        }

        return guardada;
    }

    @Override
    public Notificacion programar(Usuario usuario, Evento evento, String tipo, String asunto, String descripcion, LocalDateTime fechaProgramada) {
        Notificacion n = new Notificacion();
        n.setUsuario(usuario);
        n.setEvento(evento);
        n.setTipoNotificacion(tipo);
        n.setAsuntoNotificacion(asunto);
        n.setDescripcionNotificacion(descripcion);
        n.setMedioEnvio("APP");
        setEstado(n, "PROGRAMADA");
        n.setFechaProgramada(fechaProgramada);
        return notificacionRepository.save(n);
    }

    @Override
    public void programarRecordatorio(Usuario usuario, Evento evento) {
        LocalDateTime fecha = evento.getFechaInicioEvento().minusHours(24);
        String asunto = "Tu evento empieza pronto";
        String desc = "El evento \"" + evento.getNombreEvento() + "\" inicia el " +
                evento.getFechaInicioEvento().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) + ". ¡No faltes!";
        if (fecha.isBefore(LocalDateTime.now())) {
            enviar(usuario, evento, "RECORDATORIO", asunto, desc);
        } else {
            programar(usuario, evento, "RECORDATORIO", asunto, desc, fecha);
        }
    }

    @Override
    @Async
    public void notificarInscritos(Evento evento, String tipo, String asunto, String descripcion) {
        try {
            List<RegistroAsistencia> asistencias = registroAsistenciaRepository.findByEvento_IdEventos(evento.getIdEventos());
            if (asistencias == null) return;
            for (RegistroAsistencia r : asistencias) {
                Usuario usuario = (r != null && r.getParticipante() != null) ? r.getParticipante().getUsuario() : null;
                if (usuario != null) {
                    try {
                        enviar(usuario, evento, tipo, asunto, descripcion);
                    } catch (Exception e) {
                        System.err.println("[WARN] No se pudo enviar notificacion a usuario ID " + usuario.getIdUsuario() + ": " + e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[WARN] Error general en notificarInscritos: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    @Scheduled(fixedDelayString = "${notificaciones.scheduler.delay-ms:60000}")
    public void procesarProgramadas() {
        notificacionRepository
            .findByEstado_NombreEstadoIgnoreCaseAndFechaProgramadaLessThanEqual("PROGRAMADA", LocalDateTime.now())
            .forEach(n -> {
                setEstado(n, "ENVIADA");
                n.setFechaEnvio(LocalDateTime.now());
                notificacionRepository.save(n);

                if (n.getUsuario().getLogin() != null && n.getUsuario().getLogin().getEmailUsuario() != null
                        && !n.getUsuario().getLogin().getEmailUsuario().isEmpty()) {
                    emailService.enviarSeguro(
                            n.getUsuario().getLogin().getEmailUsuario(),
                            n.getAsuntoNotificacion(),
                            plantillaCorreo(n.getAsuntoNotificacion(), n.getDescripcionNotificacion(), n.getEvento())
                    );
                }
            });
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> obtenerDeUsuario(Long idUsuario) {
         return notificacionRepository.findByUsuarioWithEvento(idUsuario).stream().map(n -> {
            Map<String, Object> m = new HashMap<>();
            m.put("idNotificaciones", n.getIdNotificaciones());
            m.put("asunto", n.getAsuntoNotificacion());
            m.put("descripcion", n.getDescripcionNotificacion());
            m.put("tipo", n.getTipoNotificacion());
            m.put("estado", n.getEstado() != null ? n.getEstado().getNombreEstado() : null);
            m.put("fecha", n.getCreatedAt());
            m.put("evento", n.getEvento() != null ? n.getEvento().getNombreEvento() : null);
            return m;
        }).collect(Collectors.toList());
    }

    @Override
    public long contarNoLeidas(Long idUsuario) {
        return notificacionRepository.countByUsuario_IdUsuarioAndEstado_NombreEstadoIgnoreCase(idUsuario, "ENVIADA");
    }

    @Override
    @Transactional
    public void marcarLeida(Long idNotificacion, Long idUsuario) {
        notificacionRepository.findById(idNotificacion).ifPresent(n -> {
            if (n.getUsuario().getIdUsuario().equals(idUsuario)) {
                setEstado(n, "LEIDA");
                notificacionRepository.save(n);
            }
        });
    }

    @Override
    @Transactional
    public void marcarTodasLeidas(Long idUsuario) {
        notificacionRepository.findByUsuario_IdUsuarioAndEstado_NombreEstadoIgnoreCase(idUsuario, "ENVIADA")
            .forEach(n -> {
                setEstado(n, "LEIDA");
                notificacionRepository.save(n);
            });
    }

    private String plantillaCorreo(String asunto, String descripcion, Evento evento) {
        StringBuilder html = new StringBuilder();
        html.append("<div style='font-family:Arial;max-width:600px;margin:auto;border:1px solid #ddd'>");
        html.append("<div style='background:#1f2937;color:#fff;padding:16px'><h2>").append(asunto).append("</h2></div>");
        html.append("<div style='padding:20px'>");
        html.append("<p>").append(descripcion).append("</p>");
        if (evento != null) {
            html.append("<hr><p><b>Evento:</b> ").append(evento.getNombreEvento());
            if (evento.getFechaInicioEvento() != null) {
                html.append("<br><b>Fecha:</b> ").append(evento.getFechaInicioEvento());
            }
            if (evento.getLugarEvento() != null) {
                html.append("<br><b>Lugar:</b> ").append(evento.getLugarEvento());
            }
            html.append("</p>");
        }
        html.append("</div></div>");
        return html.toString();
    }
}