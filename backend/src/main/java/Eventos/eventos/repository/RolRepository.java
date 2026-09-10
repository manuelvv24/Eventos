package Eventos.eventos.repository;

import Eventos.eventos.entity.Rol;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RolRepository extends JpaRepository<Rol, Long> {
    Optional<Rol> findByNombreRolIgnoreCase(String nombreRol);
    boolean existsByNombreRolIgnoreCase(String nombreRol);
    List<Rol> findByActivoTrueOrderByNombreRolAsc();
    List<Rol> findByActivoFalse();
}