package Eventos.eventos.repository;

import Eventos.eventos.entity.Notificacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {

    @Query("SELECT n FROM Notificacion n LEFT JOIN FETCH n.evento " +
           "WHERE n.usuario.idUsuario = :id ORDER BY n.createdAt DESC")
    List<Notificacion> findByUsuarioWithEvento(@Param("id") Long idUsuario);

    long countByUsuario_IdUsuarioAndEstado_NombreEstadoIgnoreCase(Long idUsuario, String estado);

    List<Notificacion> findByEstado_NombreEstadoIgnoreCaseAndFechaProgramadaLessThanEqual(String estado, LocalDateTime fecha);

    List<Notificacion> findByUsuario_IdUsuarioAndEstado_NombreEstadoIgnoreCase(Long idUsuario, String estado);
}