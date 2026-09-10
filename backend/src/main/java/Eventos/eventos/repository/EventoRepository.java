package Eventos.eventos.repository;

import Eventos.eventos.entity.Evento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface EventoRepository extends JpaRepository<Evento, Long> {

    List<Evento> findByEliminadoTrue();

    @Override
    @Query("SELECT e FROM Evento e WHERE e.eliminado = false OR e.eliminado IS NULL")
    List<Evento> findAll();

    @Query("SELECT e FROM Evento e WHERE LOWER(e.tipoEvento) = LOWER(:tipoEvento) AND (e.eliminado = false OR e.eliminado IS NULL)")
    List<Evento> findByTipoEventoIgnoreCase(@Param("tipoEvento") String tipoEvento);

    @Query("SELECT e FROM Evento e WHERE LOWER(e.modalidadEvento) = LOWER(:modalidadEvento) AND (e.eliminado = false OR e.eliminado IS NULL)")
    List<Evento> findByModalidadEventoIgnoreCase(@Param("modalidadEvento") String modalidadEvento);

    @Query("SELECT e FROM Evento e WHERE LOWER(e.estado.nombreEstado) = LOWER(:estadoEvento) AND (e.eliminado = false OR e.eliminado IS NULL)")
    List<Evento> findByEstado_NombreEstadoIgnoreCase(@Param("estadoEvento") String estadoEvento);

    @Query("SELECT e FROM Evento e WHERE e.fechaInicioEvento BETWEEN :desde AND :hasta AND (e.eliminado = false OR e.eliminado IS NULL)")
    List<Evento> findByFechaInicioEventoBetween(@Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);

    @Query("SELECT e FROM Evento e WHERE " +
           "(:tipo IS NULL OR LOWER(e.tipoEvento) = LOWER(:tipo)) AND " +
           "(:modalidad IS NULL OR LOWER(e.modalidadEvento) = LOWER(:modalidad)) AND " +
           "(:estado IS NULL OR LOWER(e.estado.nombreEstado) = LOWER(:estado)) AND " +
           "(:desde IS NULL OR e.fechaInicioEvento >= :desde) AND " +
           "(:hasta IS NULL OR e.fechaInicioEvento <= :hasta) AND " +
           "(e.eliminado = false OR e.eliminado IS NULL)")
    List<Evento> filtrar(
            @Param("tipo") String tipo,
            @Param("modalidad") String modalidad,
            @Param("estado") String estado,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta
    );

    @Query("SELECT COUNT(r) FROM RegistroAsistencia r WHERE r.evento.idEventos = :idEvento")
    long contarRegistradosPorEvento(@Param("idEvento") Long idEvento);

    @Query("SELECT COUNT(e) > 0 FROM Evento e WHERE lower(e.nombreEvento) = :nombreLower " +
           "AND e.fechaInicioEvento IS NOT NULL AND e.fechaInicioEvento >= :inicio " +
           "AND e.fechaInicioEvento < :fin AND (e.eliminado = false OR e.eliminado IS NULL) " +
           "AND (:idActual IS NULL OR e.idEventos <> :idActual)")
    boolean existsCadena(
            @Param("nombreLower") String nombreLower,
            @Param("inicio") LocalDateTime inicio,
            @Param("fin") LocalDateTime fin,
            @Param("idActual") Long idActual);
}