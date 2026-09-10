package Eventos.eventos.repository;

import Eventos.eventos.entity.Estado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EstadoRepository extends JpaRepository<Estado, Long> {
    Optional<Estado> findByTipoEstadoIgnoreCaseAndNombreEstadoIgnoreCase(String tipoEstado, String nombreEstado);
    Optional<Estado> findByNombreEstadoIgnoreCase(String nombreEstado);
}
