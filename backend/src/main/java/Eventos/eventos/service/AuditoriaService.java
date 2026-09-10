package Eventos.eventos.service;

import Eventos.eventos.entity.LogAuditoria;
import Eventos.eventos.entity.Usuario;
import Eventos.eventos.repository.LogAuditoriaRepository;
import Eventos.eventos.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AuditoriaService {

    @Autowired
    private LogAuditoriaRepository logRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrar(String accion, String entidad, Long idEntidad,
                          String detalle, Long idUsuario, String ip) {
        try {
            LogAuditoria log = new LogAuditoria();
            log.setAccionLogAuditoria(accion);
            log.setEntidadLogAuditoria(entidad);
            log.setIdEntidadLogAuditoria(idEntidad);
            log.setDetalleLogAuditoria(detalle);
            if (idUsuario != null) {
                log.setUsuario(usuarioRepository.findById(idUsuario).orElse(null));
            }
            log.setIpOrigenLogAuditoria(ip);
            logRepository.save(log);
        } catch (Exception e) {
            System.err.println("[AUDITORIA ERROR] No se pudo registrar el log: " + e.getMessage());
        }
    }

    public List<LogAuditoria> filtrar(String accion, String entidad,
                                      LocalDateTime desde, LocalDateTime hasta,
                                      Long idUsuario) {
        return logRepository.findAllByOrderByFechaLogAuditoriaDesc().stream()
                .filter(l -> accion   == null || accion.equalsIgnoreCase(l.getAccionLogAuditoria()))
                .filter(l -> entidad  == null || entidad.equalsIgnoreCase(l.getEntidadLogAuditoria()))
                .filter(l -> desde    == null || !l.getFechaLogAuditoria().isBefore(desde))
                .filter(l -> hasta    == null || !l.getFechaLogAuditoria().isAfter(hasta))
                .filter(l -> idUsuario == null || l.getUsuario() == null
                        || idUsuario.equals(l.getUsuario().getIdUsuario()))
                .collect(java.util.stream.Collectors.toList());
    }
    public List<LogAuditoria> obtenerTodos() {
        return logRepository.findAll();
    }
}
