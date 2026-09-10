package Eventos.eventos.repository;

import Eventos.eventos.entity.Importacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ImportacionRepository extends JpaRepository<Importacion, Long> {
    List<Importacion> findByUsuario_IdUsuario(Long idUsuario);
    List<Importacion> findByEvento_IdEventos(Long idEventos);
    List<Importacion> findByEstado_NombreEstadoIgnoreCase(String estado);
}
