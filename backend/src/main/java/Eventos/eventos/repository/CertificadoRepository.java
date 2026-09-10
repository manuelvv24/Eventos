package Eventos.eventos.repository;

import Eventos.eventos.entity.Certificado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CertificadoRepository extends JpaRepository<Certificado, Long> {
    Optional<Certificado> findByCodigoVerificacionCertificado(String codigoVerificacion);

    // HU07: check for existing certificate before creating a duplicate
    Optional<Certificado> findByRegistroAsistencia_IdRegistroAsistencia(Long idRegistroAsistencia);
}