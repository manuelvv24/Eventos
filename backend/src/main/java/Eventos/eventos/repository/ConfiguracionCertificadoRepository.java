package Eventos.eventos.repository;

import Eventos.eventos.entity.ConfiguracionCertificado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConfiguracionCertificadoRepository extends JpaRepository<ConfiguracionCertificado, Long> {
    List<ConfiguracionCertificado> findAllByOrderByFechaActualizacionDesc();
    List<ConfiguracionCertificado> findAllByActivaTrueOrderByFechaActualizacionDesc();
    List<ConfiguracionCertificado> findAllByActivaFalse();
    Optional<ConfiguracionCertificado> findFirstByEsPredeterminadaTrue();
    Optional<ConfiguracionCertificado> findFirstByActivaTrue();
}
