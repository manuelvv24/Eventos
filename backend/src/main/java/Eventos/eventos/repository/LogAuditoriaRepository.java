package Eventos.eventos.repository;

import Eventos.eventos.entity.LogAuditoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface LogAuditoriaRepository extends JpaRepository<LogAuditoria, Long> {

    List<LogAuditoria> findByAccionLogAuditoriaIgnoreCase(String accion);
    List<LogAuditoria> findByEntidadLogAuditoriaIgnoreCase(String entidad);
    List<LogAuditoria> findByUsuario_IdUsuario(Long idUsuario);
    List<LogAuditoria> findByFechaLogAuditoriaBetween(LocalDateTime desde, LocalDateTime hasta);
    List<LogAuditoria> findAllByOrderByFechaLogAuditoriaDesc();
}
