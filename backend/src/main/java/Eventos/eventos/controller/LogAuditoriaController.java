package Eventos.eventos.controller;

import Eventos.eventos.dto.LogAuditoriaDTO;
import Eventos.eventos.entity.LogAuditoria;
import Eventos.eventos.service.AuditoriaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auditoria")
public class LogAuditoriaController {

    @Autowired
    private AuditoriaService auditoriaService;

    @GetMapping
    public ResponseEntity<List<LogAuditoriaDTO>> listar(
            @RequestParam(required = false) String accion,
            @RequestParam(required = false) String entidad,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta,
            @RequestParam(required = false) Long idUsuario) {

        List<LogAuditoria> logs = auditoriaService.filtrar(accion, entidad, desde, hasta, idUsuario);
        return ResponseEntity.ok(logs.stream().map(this::mapToDTO).collect(Collectors.toList()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<LogAuditoriaDTO> obtenerPorId(@PathVariable Long id) {
        return auditoriaService.obtenerTodos().stream()
                .filter(l -> l.getIdLogAuditoria().equals(id))
                .findFirst()
                .map(l -> ResponseEntity.ok(mapToDTO(l)))
                .orElse(ResponseEntity.notFound().build());
    }

    private LogAuditoriaDTO mapToDTO(LogAuditoria log) {
        LogAuditoriaDTO dto = new LogAuditoriaDTO();
        dto.setIdLogAuditoria(log.getIdLogAuditoria());
        dto.setIdUsuario(log.getUsuario() != null ? log.getUsuario().getIdUsuario() : null);
        dto.setAccionAuditoria(log.getAccionLogAuditoria());
        dto.setEntidadAuditada(log.getEntidadLogAuditoria());
        dto.setIdEntidadAuditada(log.getIdEntidadLogAuditoria());
        dto.setDetalleAuditoria(log.getDetalleLogAuditoria());
        dto.setIpOrigenAuditoria(log.getIpOrigenLogAuditoria());
        dto.setFechaAuditoria(log.getFechaLogAuditoria());
        return dto;
    }
}
