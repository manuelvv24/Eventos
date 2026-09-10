package Eventos.eventos.repository;

import Eventos.eventos.entity.Participante;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ParticipanteRepository extends JpaRepository<Participante, Long> {
    Optional<Participante> findByUsuario_IdUsuario(Long idUsuario);
    List<Participante> findAllByUsuario_IdUsuario(Long idUsuario);
    boolean existsByUsuario_IdUsuario(Long idUsuario);

    @Query("SELECT p FROM Participante p WHERE p.eliminado IS NULL OR p.eliminado = false")
    List<Participante> findActivos();

    @Query("SELECT p FROM Participante p WHERE p.eliminado = true")
    List<Participante> findEliminados();
}
