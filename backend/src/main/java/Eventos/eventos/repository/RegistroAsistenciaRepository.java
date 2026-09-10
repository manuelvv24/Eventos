package Eventos.eventos.repository;

import Eventos.eventos.entity.RegistroAsistencia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RegistroAsistenciaRepository extends JpaRepository<RegistroAsistencia, Long> {

    List<RegistroAsistencia> findByEvento_IdEventos(Long idEventos);
    Optional<RegistroAsistencia> findByCodigoQrInscripcion(String codigoQrInscripcion);
    boolean existsByEvento_IdEventosAndParticipante_IdParticipante(Long idEventos, Long idParticipante);
    Optional<RegistroAsistencia> findByEvento_IdEventosAndParticipante_IdParticipante(
            Long idEventos, Long idParticipante);

    @Query("SELECT COUNT(r) FROM RegistroAsistencia r WHERE r.evento.idEventos = :idEvento AND UPPER(r.estado.nombreEstado) <> 'ELIMINADO'")
    long countInscritosPorEvento(@Param("idEvento") Long idEvento);

    @Query("SELECT COUNT(r) FROM RegistroAsistencia r WHERE r.evento.idEventos = :idEvento AND UPPER(r.estado.nombreEstado) = 'ASISTIO'")
    long contarAsistenciasPorEvento(@Param("idEvento") Long idEvento);

    @Query("SELECT r FROM RegistroAsistencia r JOIN FETCH r.evento WHERE r.participante.idParticipante = :idParticipante")
    List<RegistroAsistencia> findByIdParticipantesWithEvento(@Param("idParticipante") Long idParticipante);

    List<RegistroAsistencia> findByParticipante_IdParticipante(Long idParticipante);

    @Query("SELECT r FROM RegistroAsistencia r " +
            "WHERE r.participante.usuario.numeroDocumentoUsuario = :numeroDocumento")
    List<RegistroAsistencia> findByNumeroDocumento(@Param("numeroDocumento") String numeroDocumento);

    long countByEvento_IdEventos(Long idEventos);

    interface HeatmapProjection {
        int getDia();
        int getHora();
        long getTotal();
    }

    interface MesProjection {
        String getMes();
        long getTotal();
    }
@Query(value = "SELECT CAST(EXTRACT(ISODOW FROM fecha_asistencia) AS integer) AS dia, " +
               "CAST(EXTRACT(HOUR FROM fecha_asistencia) AS integer) AS hora, " +
               "CAST(COUNT(*) AS bigint) AS total " +
               "FROM registro_asistencia " +
               "WHERE fecha_asistencia IS NOT NULL " +
               "GROUP BY 1, 2 " +
               "ORDER BY 1, 2", nativeQuery = true)
List<HeatmapProjection> heatmapAsistencia();

@Query(value = "SELECT TO_CHAR(fecha_asistencia, 'YYYY-MM') AS mes, " +
               "CAST(COUNT(*) AS bigint) AS total " +
               "FROM registro_asistencia " +
               "WHERE fecha_asistencia IS NOT NULL " +
               "GROUP BY 1 " +
               "ORDER BY 1", nativeQuery = true)
List<MesProjection> registrosPorMes();
}
